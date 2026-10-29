package com.sigebox.juez.data;

import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;

import java.util.List;

/**
 * Bandeja de salida: cada mensaje se escribe en Room antes de enviarse y queda pendiente hasta su {@code ACK}. Un
 * {@code ACK} con error también lo saca (reenviar no lo arreglaría) y guarda el error para la UI.
 */
public final class Outbox {
    private final OutboxDao dao;
    private final Clock clock;

    public Outbox(OutboxDao dao, Clock clock) {
        this.dao = dao;
        this.clock = clock;
    }

    public void enqueue(Message message) {
        OutboxEntity entity = new OutboxEntity();
        entity.messageId = message.id();
        entity.type = message.type();
        entity.payload = Messages.encode(message);
        entity.createdAt = clock.now();
        dao.insert(entity);
    }

    public List<OutboxEntity> pending() {
        return dao.unacked();
    }

    public int pendingCount() {
        return dao.countUnacked();
    }

    public void markSent(String messageId) {
        dao.markSent(messageId, clock.now());
    }

    /** Pendientes nunca enviados o enviados hace más de {@code afterMs}. */
    public List<OutboxEntity> dueForResend(long afterMs) {
        return dao.unackedSentBefore(clock.now() - afterMs);
    }

    /** @return {@code true} si el {@code ACK} correspondía a un mensaje pendiente */
    public boolean onAck(Ack ack) {
        String status = ack.status() == null ? null : ack.status().name();
        String error = ack.error() == null ? null : ack.error().name();
        return dao.markAcked(ack.ackId(), ack.ok(), status, error) > 0;
    }
}
