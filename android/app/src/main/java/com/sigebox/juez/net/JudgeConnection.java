package com.sigebox.juez.net;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.protocol.Challenge;
import com.sigebox.core.protocol.CodeInfo;
import com.sigebox.core.protocol.CodeLookup;
import com.sigebox.core.protocol.CodeUnknown;
import com.sigebox.core.protocol.FightEnded;
import com.sigebox.core.protocol.JoinUri;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.NextFight;
import com.sigebox.core.protocol.RoundClosed;
import com.sigebox.core.protocol.RoundOpened;
import com.sigebox.core.protocol.State;
import com.sigebox.juez.data.Clock;
import com.sigebox.juez.data.Outbox;
import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.data.SessionStore;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * WebSocket del juez (OkHttp) con reconexión automática. Todo pasa por un solo hilo, así {@link JudgeSession} nunca
 * ve dos eventos a la vez. Los eventos de un socket viejo se descartan por generación.
 */
public final class JudgeConnection {
    /** Ruta del WebSocket en la central (spike DSK-002). */
    public static final String PATH = "/ws";
    /** Espera antes de repetir una búsqueda que dio {@code CODE_UNKNOWN}: la central admite una por segundo. */
    static final long LOOKUP_RETRY_MS = 1_100;

    private final OkHttpClient baseClient;
    private final SessionStore store;
    private final Backoff backoff;
    private final long tickMs;
    private final JudgeSession session;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "sigebox-ws");
        thread.setDaemon(true);
        return thread;
    });
    private final MutableLiveData<ConnectionState> liveState = new MutableLiveData<>(ConnectionState.INITIAL);
    private final List<JudgeListener> listeners = new CopyOnWriteArrayList<>();
    // Lo último que mandó la central, para los listeners que se registran después (bajo el candado de listeners).
    private State lastState;
    private FightEnded fightEnded;
    /** Último NEXT_FIGHT sin lugar ("No participas"); se olvida con el siguiente aviso o un STATE. */
    private NextFight notSeated;

    // Solo se tocan en el hilo del executor.
    private WebSocket socket;
    private int generation;
    private boolean running;
    private ScheduledFuture<?> pendingReconnect;
    private ScheduledFuture<?> ticker;
    /** Unión por código pendiente de {@code CODE_INFO} (ADR 0004): a dónde conectar y con qué nombre. */
    private JoinUri lookup;
    private String lookupName;
    private boolean lookupRetried;

    public JudgeConnection(OkHttpClient baseClient, Outbox outbox, SessionStore store, DeviceSigner signer,
            Clock clock, Backoff backoff, long tickMs) {
        this.baseClient = baseClient;
        this.store = store;
        this.backoff = backoff;
        this.tickMs = tickMs;
        this.session = new JudgeSession(outbox, store, signer, clock, new JudgeListener() {
            @Override
            public void onState(ConnectionState state) {
                liveState.postValue(state);
                for (JudgeListener l : listeners) {
                    l.onState(state);
                }
            }

            @Override
            public void onMessage(Message message) {
                dispatch(message);
            }
        });
    }

    /** Estado para la UI (Activities y ViewModels). */
    public LiveData<ConnectionState> state() {
        return liveState;
    }

    public ConnectionState current() {
        return session.state();
    }

    /**
     * Registra {@code listener} y le entrega de inmediato el último {@code STATE} (con el round al día) y el
     * {@code FIGHT_ENDED} si ya llegaron: una pantalla detenida o que abre tarde no pierde avisos (AND-014). Solo se
     * repite lo que la central ya mandó a este teléfono.
     */
    public void addListener(JudgeListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
            if (lastState != null) {
                listener.onMessage(lastState);
            }
            if (fightEnded != null) {
                listener.onMessage(fightEnded);
            }
            if (notSeated != null) {
                listener.onMessage(notSeated);
            }
        }
    }

    /** Entrega un mensaje de la central a los listeners y actualiza lo que se repite a los que lleguen después. */
    void dispatch(Message message) {
        synchronized (listeners) {
            remember(message);
            for (JudgeListener l : listeners) {
                l.onMessage(message);
            }
        }
    }

    private void remember(Message message) {
        if (message instanceof NextFight next) {
            // Con lugar empieza otra pelea: nada de la anterior se repite. Sin lugar se recuerda para la pantalla.
            lastState = null;
            fightEnded = null;
            notSeated = next.seat() == null ? next : null;
        } else if (message instanceof State state) {
            notSeated = null;
            lastState = state;
            if (fightEnded != null && !fightEnded.fightId().equals(state.fightId())) {
                fightEnded = null;
            }
        } else if (message instanceof FightEnded ended) {
            fightEnded = ended;
        } else if (message instanceof RoundOpened opened) {
            lastState = withRound(lastState, opened.fightId(), opened.round(), EstadoRound.ABIERTO);
        } else if (message instanceof RoundClosed closed) {
            lastState = withRound(lastState, closed.fightId(), closed.round(), EstadoRound.CERRADO);
        }
    }

    /** El último STATE con el round que anunció la central después; null si aún no hay STATE de esa pelea. */
    private static State withRound(State state, String fightId, int round, EstadoRound status) {
        if (state == null || !state.fightId().equals(fightId)) {
            return state;
        }
        return new State(state.fightId(), state.seat(), state.judgeName(), state.redCorner(), state.blueCorner(),
                state.rounds(), state.fightStatus(), round, status, state.cards(), state.finalCardSent());
    }

    public void removeListener(JudgeListener listener) {
        listeners.remove(listener);
    }

    /** Reconecta al lugar guardado, si lo hay (al abrir la app tras un cierre forzado). */
    public void start() {
        executor.execute(() -> {
            session.refresh();
            running = true;
            startTicker();
            if (store.load() != null) {
                open();
            }
        });
    }

    /**
     * Une el teléfono al lugar del QR: guarda el lugar, conecta y manda {@code JOIN} tras el {@code CHALLENGE}. Con
     * el código de lugar ({@link JoinUri#code()}) primero pregunta con {@code CODE_LOOKUP}; con {@code CODE_INFO}
     * guarda el lugar y manda el mismo {@code JOIN} firmado. Un {@code CODE_UNKNOWN} se reintenta una vez tras
     * {@link #LOOKUP_RETRY_MS} (la central puede estar ocupada) y luego llega a los listeners sin unirse.
     */
    public void join(JoinUri uri, String judgeName) {
        executor.execute(() -> {
            running = true;
            startTicker();
            closeSocket();
            synchronized (listeners) {
                // Unirse a otro lugar empieza de cero: nada de la pelea anterior se repite.
                lastState = null;
                fightEnded = null;
                notSeated = null;
            }
            if (uri.code() != null) {
                lookup = uri;
                lookupName = judgeName;
                lookupRetried = false;
            } else {
                lookup = null;
                session.join(uri, judgeName);
            }
            open();
        });
    }

    /** Guarda el mensaje en la bandeja y lo manda cuando haya conexión. */
    public void submit(Message message) {
        executor.execute(() -> session.submit(message));
    }

    public void stop() {
        executor.execute(() -> {
            running = false;
            if (ticker != null) {
                ticker.cancel(false);
                ticker = null;
            }
            cancelReconnect();
            closeSocket();
            session.onClosed();
        });
    }

    private void startTicker() {
        if (ticker == null) {
            ticker = executor.scheduleWithFixedDelay(this::tick, tickMs, tickMs, TimeUnit.MILLISECONDS);
        }
    }

    private void tick() {
        if (socket != null && !session.tick()) {
            // Sin noticias de la central: se tira el socket y onFailure agenda la reconexión.
            socket.cancel();
        }
    }

    private void open() {
        cancelReconnect();
        SessionEntity saved = lookup == null ? store.load() : null;
        if (!running || (lookup == null && saved == null)) {
            return;
        }
        String host = lookup != null ? lookup.host() : saved.host;
        int port = lookup != null ? lookup.port() : saved.port;
        String fingerprint = lookup != null ? lookup.certFingerprint() : saved.certFingerprint;
        OkHttpClient client = fingerprint == null
                ? baseClient
                : PinnedTrust.pin(baseClient, fingerprint);
        String scheme = fingerprint == null ? "ws" : "wss";
        Request request = new Request.Builder()
                .url(scheme + "://" + hostForUrl(host) + ":" + port + PATH)
                .build();
        int gen = ++generation;
        socket = client.newWebSocket(request, new Listener(gen));
    }

    /** Atiende lo de la búsqueda por código; {@code false} si el mensaje sigue su camino normal. */
    private boolean onLookupText(String text) {
        if (lookup == null) {
            return false;
        }
        Message message;
        try {
            message = Messages.decode(text);
        } catch (RuntimeException e) {
            return false;
        }
        if (message instanceof Challenge challenge) {
            // Solo el nonce: el lugar guardado no se toca ni manda HELLO/JOIN hasta CODE_INFO (AND-034).
            session.rememberChallenge(challenge);
            sendLookup();
            return true;
        }
        if (message instanceof CodeInfo info && info.code().equals(lookup.code())) {
            JoinUri place = new JoinUri(lookup.host(), lookup.port(), info.fightId(), info.seat(), lookup.code(),
                    lookup.certFingerprint());
            String judgeName = lookupName;
            lookup = null;
            session.join(place, judgeName); // guarda el lugar y manda el JOIN firmado sobre el mismo CHALLENGE
            dispatch(info);
            return true;
        }
        if (message instanceof CodeUnknown unknown && unknown.code().equals(lookup.code())) {
            if (!lookupRetried) {
                lookupRetried = true;
                int gen = generation;
                executor.schedule(() -> {
                    if (gen == generation && lookup != null) {
                        sendLookup();
                    }
                }, LOOKUP_RETRY_MS, TimeUnit.MILLISECONDS);
                return true;
            }
            lookup = null;
            running = false;
            cancelReconnect();
            closeSocket();
            session.onClosed();
            dispatch(unknown);
            return true;
        }
        return false;
    }

    private void sendLookup() {
        if (socket != null) {
            socket.send(Messages.encode(new CodeLookup(lookup.code())));
        }
    }

    private static String hostForUrl(String host) {
        return host.contains(":") && !host.startsWith("[") ? "[" + host + "]" : host;
    }

    private void closeSocket() {
        if (socket != null) {
            generation++;
            socket.close(1000, null);
            socket = null;
        }
    }

    private void scheduleReconnect() {
        if (!running || pendingReconnect != null) {
            return;
        }
        pendingReconnect = executor.schedule(() -> {
            pendingReconnect = null;
            open();
        }, backoff.next(), TimeUnit.MILLISECONDS);
    }

    private void cancelReconnect() {
        if (pendingReconnect != null) {
            pendingReconnect.cancel(false);
            pendingReconnect = null;
        }
    }

    private final class Listener extends WebSocketListener {
        private final int gen;

        Listener(int gen) {
            this.gen = gen;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            executor.execute(() -> {
                if (gen != generation) {
                    return;
                }
                backoff.reset();
                session.onOpen(new Transport() {
                    @Override
                    public boolean send(String text) {
                        return webSocket.send(text);
                    }

                    @Override
                    public void close() {
                        // Se corta sin esperar el saludo de cierre: todo está guardado o con ACK, y así la
                        // reconexión a la pelea nueva es inmediata (onFailure la agenda).
                        webSocket.cancel();
                    }
                });
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            executor.execute(() -> {
                if (gen == generation && !onLookupText(text)) {
                    session.onText(text);
                }
            });
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(1000, null);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            lost();
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            lost();
        }

        private void lost() {
            executor.execute(() -> {
                if (gen != generation) {
                    return;
                }
                socket = null;
                session.onClosed();
                scheduleReconnect();
            });
        }
    }
}
