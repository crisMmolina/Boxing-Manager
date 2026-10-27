package com.sigebox.juez.net;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.protocol.FightEnded;
import com.sigebox.core.protocol.JoinUri;
import com.sigebox.core.protocol.Message;
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

    // Solo se tocan en el hilo del executor.
    private WebSocket socket;
    private int generation;
    private boolean running;
    private ScheduledFuture<?> pendingReconnect;
    private ScheduledFuture<?> ticker;

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
        if (message instanceof State state) {
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

    /** Une el teléfono al lugar del QR: guarda el lugar, conecta y manda {@code JOIN} tras el {@code CHALLENGE}. */
    public void join(JoinUri uri, String judgeName) {
        executor.execute(() -> {
            running = true;
            startTicker();
            closeSocket();
            synchronized (listeners) {
                // Unirse a otro lugar empieza de cero: nada de la pelea anterior se repite.
                lastState = null;
                fightEnded = null;
            }
            session.join(uri, judgeName);
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
        SessionEntity target = store.load();
        if (!running || target == null) {
            return;
        }
        OkHttpClient client = target.certFingerprint == null
                ? baseClient
                : PinnedTrust.pin(baseClient, target.certFingerprint);
        String scheme = target.certFingerprint == null ? "ws" : "wss";
        Request request = new Request.Builder()
                .url(scheme + "://" + hostForUrl(target.host) + ":" + target.port + PATH)
                .build();
        int gen = ++generation;
        socket = client.newWebSocket(request, new Listener(gen));
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
                        webSocket.close(1000, null);
                    }
                });
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            executor.execute(() -> {
                if (gen == generation) {
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
