package com.sigebox.judgesim;

import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Challenge;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.FightClosed;
import com.sigebox.core.protocol.FightEnded;
import com.sigebox.core.protocol.FinalCardHash;
import com.sigebox.core.protocol.Hello;
import com.sigebox.core.protocol.Join;
import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.JoinPending;
import com.sigebox.core.protocol.JoinRejected;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.Ping;
import com.sigebox.core.protocol.Pong;
import com.sigebox.core.protocol.ProtocolException;
import com.sigebox.core.protocol.RoundClosed;
import com.sigebox.core.protocol.RoundOpened;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitFinalCard;
import com.sigebox.core.protocol.SubmitRound;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Un juez simulado, sin depender del transporte: recibe texto con {@link #onMessage} y avanza el
 * tiempo con {@link #tick}. Se comporta como la app (contrato AND-003): espera el
 * {@code CHALLENGE}, se une con {@code JOIN} firmado o vuelve con {@code HELLO}, reenvía cada
 * {@code SUBMIT_*} con el mismo {@code id} hasta su {@code ACK} y manda {@code PING} cada 5 s.
 */
public final class SimJudge {

    /** El canal hacia la central. */
    public interface Transport {
        void send(String text);

        void close();
    }

    public enum Phase { DISCONNECTED, AUTHENTICATING, PENDING, APPROVED, DONE }

    static final long RETRY_MS = 2_000;
    static final long PING_MS = 5_000;

    private record Pending(String json, int round, boolean finalCard, int red, int blue) {}

    private final String fightId;
    private final int seat;
    private final String token;
    private final String name;
    private final String deviceId;
    private final KeyPair keys;
    private final Map<Integer, Script.Plan> plans;
    private final LatencyStats stats;
    private final LongSupplier clock;
    private final Consumer<String> log;

    private Transport transport;
    private Phase phase = Phase.DISCONNECTED;
    private boolean everApproved;
    private final LinkedHashMap<String, Pending> outbox = new LinkedHashMap<>();
    private final Map<String, Long> lastSent = new LinkedHashMap<>();
    private final TreeMap<Integer, State.OwnCard> own = new TreeMap<>();
    private boolean finalWanted;
    private boolean finalSent;
    private long lastPing;
    private int acks;

    public SimJudge(String fightId, int seat, String token, String name, Map<Integer, Script.Plan> plans,
            LatencyStats stats, LongSupplier clock, Consumer<String> log) {
        this.fightId = fightId;
        this.seat = seat;
        this.token = token;
        this.name = name;
        this.deviceId = "judge-sim-" + seat;
        this.keys = newKeys();
        this.plans = plans;
        this.stats = stats;
        this.clock = clock;
        this.log = log;
    }

    public int seat() {
        return seat;
    }

    public synchronized Phase phase() {
        return phase;
    }

    /** ACK positivos recibidos (para decidir cuándo simular el corte). */
    public synchronized int acks() {
        return acks;
    }

    public synchronized List<State.OwnCard> ownCards() {
        return List.copyOf(own.values());
    }

    /** Se está abriendo una conexión: no intentar otra mientras tanto. */
    public synchronized void connecting() {
        if (phase == Phase.DISCONNECTED) {
            phase = Phase.AUTHENTICATING;
        }
    }

    /** La conexión no se pudo abrir; se reintenta en el siguiente ciclo. */
    public synchronized void connectFailed() {
        if (transport == null && phase == Phase.AUTHENTICATING) {
            phase = Phase.DISCONNECTED;
        }
    }

    public synchronized void connected(Transport t) {
        transport = t;
        phase = Phase.AUTHENTICATING;
    }

    /**
     * La conexión {@code t} se cayó; lo pendiente se reenvía al volver. Se ignora si {@code t} ya
     * no es la conexión actual (el cierre tardío de una conexión vieja).
     */
    public synchronized void disconnected(Transport t) {
        if (t != transport) {
            return;
        }
        transport = null;
        if (phase != Phase.DONE) {
            phase = Phase.DISCONNECTED;
        }
    }

    /** Cierra la conexión a propósito (corte de Wi-Fi simulado). */
    public synchronized void drop() {
        var t = transport;
        disconnected(t);
        if (t != null) {
            t.close();
        }
    }

    public synchronized void onMessage(String text) {
        Message msg;
        try {
            msg = Messages.decode(text);
        } catch (ProtocolException e) {
            log.accept("juez " + seat + ": mensaje inválido de la central");
            return;
        }
        switch (msg) {
            case Challenge c -> {
                if (everApproved) {
                    var sig = DeviceProof.sign(keys.getPrivate(),
                            DeviceProof.helloText(fightId, seat, deviceId, c.nonce()));
                    send(new Hello(fightId, seat, deviceId, c.nonce(), sig));
                } else {
                    var sig = DeviceProof.sign(keys.getPrivate(),
                            DeviceProof.joinText(fightId, seat, token, deviceId, c.nonce()));
                    send(new Join(fightId, seat, token, name, deviceId,
                            DeviceProof.encodePublicKey(keys.getPublic()), c.nonce(), sig));
                }
            }
            case JoinPending p -> {
                phase = Phase.PENDING;
                log.accept("juez " + seat + ": esperando aprobación en la central");
            }
            case JoinApproved a -> approved();
            case State s -> {
                for (var card : s.cards()) {
                    own.put(card.round(), card);
                }
                // Lo que la central ya tiene no se reenvía.
                outbox.values().removeIf(p -> !p.finalCard() && own.containsKey(p.round()));
                finalSent |= Boolean.TRUE.equals(s.finalCardSent());
                approved();
                catchUp(s);
            }
            case JoinRejected r -> {
                phase = Phase.DONE;
                log.accept("juez " + seat + ": rechazado (" + r.reason() + ")");
            }
            case RoundOpened r -> {
                var plan = plans.get(r.round());
                if (plan != null && !plan.late()) {
                    submit(plan);
                }
            }
            case RoundClosed r -> {
                var plan = plans.get(r.round());
                if (Boolean.TRUE.equals(r.mustSubmit()) && plan != null && !own.containsKey(r.round())
                        && outbox.values().stream().noneMatch(p -> p.round() == r.round())) {
                    submit(plan); // tardía: la central la exige al cerrar
                }
            }
            case Ack a -> onAck(a);
            case FightEnded e -> finalWanted = true;
            case FightClosed c -> {
                phase = Phase.DONE;
                log.accept("juez " + seat + ": pelea cerrada");
            }
            case Ping p -> send(new Pong(p.ts()));
            default -> {
                // PONG y demás: nada que hacer.
            }
        }
        maybeSendFinal();
    }

    /** Reintentos y PING; llamar cada ~100 ms. */
    public synchronized void tick() {
        long now = clock.getAsLong();
        if (transport == null) {
            return;
        }
        if (phase == Phase.APPROVED) {
            for (var e : outbox.entrySet()) {
                long last = lastSent.getOrDefault(e.getKey(), Long.MIN_VALUE);
                if (last == Long.MIN_VALUE || now - last >= RETRY_MS) {
                    transport.send(e.getValue().json());
                    lastSent.put(e.getKey(), now);
                }
            }
        }
        if ((phase == Phase.APPROVED || phase == Phase.PENDING) && now - lastPing >= PING_MS) {
            send(new Ping(now));
            lastPing = now;
        }
        maybeSendFinal();
    }

    private void approved() {
        phase = Phase.APPROVED;
        if (!everApproved) {
            log.accept("juez " + seat + ": aprobado");
        }
        everApproved = true;
        lastSent.clear(); // conexión nueva: reenviar todo lo pendiente ya
        tick();
    }

    /**
     * Tras reconectar: lo que se perdió mientras no había red. Rounds ya cerrados sin tarjeta van
     * como tardíos; el round abierto, si el guion no lo marca tardío.
     */
    private void catchUp(State s) {
        int current = s.currentRound() == null ? 0 : s.currentRound();
        boolean open = s.roundStatus() == com.sigebox.core.model.EstadoRound.ABIERTO;
        for (var plan : plans.values()) {
            boolean closed = plan.round() < current || plan.round() == current && !open;
            boolean openNow = plan.round() == current && open && !plan.late();
            boolean queued = outbox.values().stream().anyMatch(p -> !p.finalCard() && p.round() == plan.round());
            if ((closed || openNow) && !own.containsKey(plan.round()) && !queued) {
                submit(plan);
            }
        }
    }

    private void submit(Script.Plan plan) {
        var msg = new SubmitRound(fightId, seat, plan.round(), plan.red(), plan.blue());
        outbox.put(msg.id(), new Pending(Messages.encode(msg), plan.round(), false, plan.red(), plan.blue()));
        tick();
    }

    private void maybeSendFinal() {
        if (!finalWanted || finalSent || phase != Phase.APPROVED
                || outbox.values().stream().anyMatch(p -> !p.finalCard())) {
            return;
        }
        var cards = List.copyOf(own.values());
        var hash = FinalCardHash.of(fightId, seat, cards);
        var sig = DeviceProof.sign(keys.getPrivate(), DeviceProof.finalCardText(fightId, seat, cards));
        var msg = new SubmitFinalCard(fightId, seat, hash, sig);
        outbox.put(msg.id(), new Pending(Messages.encode(msg), 0, true, 0, 0));
        finalSent = true;
        tick();
    }

    private void onAck(Ack a) {
        var pending = outbox.remove(a.ackId());
        Long sentAt = lastSent.remove(a.ackId());
        if (pending == null) {
            return; // ACK repetido
        }
        if (!a.ok()) {
            log.accept("juez " + seat + ": la central rechazó " + (pending.finalCard() ? "la tarjeta final" :
                    "el round " + pending.round()) + " (" + a.error() + ")");
            if (pending.finalCard()) {
                finalSent = false;
                finalWanted = false;
            }
            return;
        }
        acks++;
        if (sentAt != null) {
            stats.add(clock.getAsLong() - sentAt);
        }
        if (!pending.finalCard()) {
            var status = a.status() == null ? EstadoTarjeta.ENVIADA : a.status();
            own.put(pending.round(), new State.OwnCard(pending.round(), pending.red(), pending.blue(), status));
        } else {
            log.accept("juez " + seat + ": tarjeta final confirmada");
        }
    }

    private void send(Message m) {
        if (transport != null) {
            transport.send(Messages.encode(m));
        }
    }

    /** Ids aún sin ACK, en orden (para pruebas). */
    synchronized List<String> pendingIds() {
        return new ArrayList<>(outbox.keySet());
    }

    private static KeyPair newKeys() {
        try {
            var gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            return gen.generateKeyPair();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("sin soporte EC P-256", e);
        }
    }
}
