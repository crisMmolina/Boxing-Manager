package com.sigebox.juez.net;

import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Challenge;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.Hello;
import com.sigebox.core.protocol.Join;
import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.JoinPending;
import com.sigebox.core.protocol.JoinRejected;
import com.sigebox.core.protocol.JoinUri;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.Ping;
import com.sigebox.core.protocol.Pong;
import com.sigebox.core.protocol.RoundClosed;
import com.sigebox.core.protocol.RoundOpened;
import com.sigebox.core.protocol.State;
import com.sigebox.juez.data.Clock;
import com.sigebox.juez.data.Outbox;
import com.sigebox.juez.data.OutboxEntity;
import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.data.SessionStore;

/**
 * Protocolo del juez sobre un socket, sin saber de OkHttp ni de hilos (los serializa {@link JudgeConnection}).
 *
 * <p>Flujo: la central abre con {@code CHALLENGE}; con un lugar aprobado se responde {@code HELLO} firmado y la
 * central manda {@code STATE}; con un lugar sin aprobar se reenvía {@code JOIN}. Al quedar sincronizado se vacía la
 * bandeja en orden. Todo lo que se califica pasa por la bandeja: nada se pierde si se cae la red o se cierra la app.
 */
public final class JudgeSession {
    /** Reenvía lo que no tiene {@code ACK} después de este tiempo (el servidor es idempotente por id). */
    public static final long RESEND_AFTER_MS = 5_000;
    /** Sin ningún mensaje de la central en este tiempo, la conexión se da por caída. */
    public static final long DEAD_AFTER_MS = 15_000;

    private final Outbox outbox;
    private final SessionStore store;
    private final DeviceSigner signer;
    private final Clock clock;
    private final JudgeListener listener;

    private Transport transport;
    private String nonce;
    private boolean synced;
    private long lastReceived;
    private ConnectionState state = ConnectionState.INITIAL;

    public JudgeSession(Outbox outbox, SessionStore store, DeviceSigner signer, Clock clock, JudgeListener listener) {
        this.outbox = outbox;
        this.store = store;
        this.signer = signer;
        this.clock = clock;
        this.listener = listener;
    }

    public synchronized ConnectionState state() {
        return state;
    }

    /** Recalcula los pendientes de la bandeja (al arrancar, antes de conectar). */
    public synchronized void refresh() {
        publish(state.withPendingAcks(outbox.pendingCount()));
    }

    public synchronized void onOpen(Transport transport) {
        this.transport = transport;
        nonce = null;
        synced = false;
        lastReceived = clock.now();
        publish(state.withStatus(ConnectionState.Status.CONNECTING));
    }

    public synchronized void onClosed() {
        transport = null;
        nonce = null;
        synced = false;
        if (state.status != ConnectionState.Status.REJECTED) {
            publish(state.withStatus(ConnectionState.Status.DISCONNECTED));
        }
    }

    /** Un mensaje de la central. Lo que no pasa la validación de core se ignora. */
    public synchronized void onText(String text) {
        Message message;
        try {
            message = Messages.decode(text);
        } catch (RuntimeException e) {
            return;
        }
        lastReceived = clock.now();
        handle(message);
        listener.onMessage(message);
    }

    /** Guarda el lugar del QR y manda {@code JOIN} en cuanto haya {@code CHALLENGE}. */
    public synchronized void join(JoinUri uri, String judgeName) {
        store.saveJoin(uri, judgeName);
        if (transport != null && nonce != null) {
            sendJoin(store.load());
        }
    }

    /** Encola un mensaje del juez ({@code SUBMIT_ROUND}, {@code SUBMIT_FINAL_CARD}) y lo manda si se puede. */
    public synchronized void submit(Message message) {
        outbox.enqueue(message);
        if (synced && send(Messages.encode(message))) {
            outbox.markSent(message.id());
        }
        publish(state.withPendingAcks(outbox.pendingCount()));
    }

    /**
     * Latido periódico: manda {@code PING} y reenvía lo vencido.
     *
     * @return {@code false} si la conexión está muerta y hay que cerrarla y reconectar
     */
    public synchronized boolean tick() {
        if (transport == null) {
            return true;
        }
        if (clock.now() - lastReceived > DEAD_AFTER_MS) {
            return false;
        }
        send(Messages.encode(new Ping(clock.now())));
        if (synced) {
            for (OutboxEntity entity : outbox.dueForResend(RESEND_AFTER_MS)) {
                sendStored(entity);
            }
        }
        return true;
    }

    private void handle(Message message) {
        if (message instanceof Challenge challenge) {
            nonce = challenge.nonce();
            SessionEntity session = store.load();
            if (session != null && session.approved) {
                sendHello(session);
            } else if (session != null) {
                sendJoin(session);
            }
        } else if (message instanceof JoinPending) {
            publish(state.withStatus(ConnectionState.Status.WAITING_APPROVAL));
        } else if (message instanceof JoinApproved) {
            store.markApproved();
            becomeSynced();
        } else if (message instanceof JoinRejected rejected) {
            store.clear();
            publish(state.rejected(rejected.reason()));
        } else if (message instanceof State st) {
            publish(state.withRound(st.currentRound(), st.roundStatus()));
            becomeSynced();
        } else if (message instanceof RoundOpened opened) {
            publish(state.withRound(opened.round(), EstadoRound.ABIERTO));
        } else if (message instanceof RoundClosed closed) {
            publish(state.withRound(closed.round(), EstadoRound.CERRADO));
        } else if (message instanceof Ack ack) {
            outbox.onAck(ack);
            publish(state.withPendingAcks(outbox.pendingCount()));
        } else if (message instanceof Ping ping) {
            send(Messages.encode(new Pong(ping.ts())));
        }
    }

    private void becomeSynced() {
        synced = true;
        for (OutboxEntity entity : outbox.pending()) {
            sendStored(entity);
        }
        publish(state.withStatus(ConnectionState.Status.CONNECTED).withPendingAcks(outbox.pendingCount()));
    }

    private void sendHello(SessionEntity session) {
        String sig = signer.sign(DeviceProof.helloText(session.fightId, session.seat, signer.deviceId(), nonce));
        send(Messages.encode(new Hello(session.fightId, session.seat, signer.deviceId(), nonce, sig)));
    }

    private void sendJoin(SessionEntity session) {
        String sig = signer.sign(
                DeviceProof.joinText(session.fightId, session.seat, session.token, signer.deviceId(), nonce));
        send(Messages.encode(new Join(session.fightId, session.seat, session.token, session.judgeName,
                signer.deviceId(), signer.publicKey(), nonce, sig)));
    }

    private void sendStored(OutboxEntity entity) {
        if (send(entity.payload)) {
            outbox.markSent(entity.messageId);
        }
    }

    private boolean send(String text) {
        return transport != null && transport.send(text);
    }

    private void publish(ConnectionState next) {
        state = next;
        listener.onState(next);
    }
}
