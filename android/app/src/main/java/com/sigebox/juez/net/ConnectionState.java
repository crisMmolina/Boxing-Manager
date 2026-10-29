package com.sigebox.juez.net;

import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.protocol.JoinRejected;

/** Lo que la UI necesita de la conexión. Inmutable; nunca trae tarjetas de otros jueces ni totales. */
public final class ConnectionState {

    public enum Status {
        /** Sin socket; reintentando solo. */
        DISCONNECTED,
        /** Socket abierto, todavía sin {@code STATE} ni aprobación. */
        CONNECTING,
        /** Se mandó {@code JOIN} y la central respondió {@code JOIN_PENDING}. */
        WAITING_APPROVAL,
        /** {@code JOIN_REJECTED}; ver {@link #rejectReason}. */
        REJECTED,
        /** Aprobado y sincronizado: lo de la bandeja ya se envió. */
        CONNECTED
    }

    public static final ConnectionState INITIAL = new ConnectionState(Status.DISCONNECTED, null, null, 0, null);

    public final Status status;
    public final Integer currentRound;
    public final EstadoRound roundStatus;
    /** Mensajes guardados sin {@code ACK} ("Enviando…" en la UI). */
    public final int pendingAcks;
    public final JoinRejected.Reason rejectReason;

    public ConnectionState(Status status, Integer currentRound, EstadoRound roundStatus, int pendingAcks,
            JoinRejected.Reason rejectReason) {
        this.status = status;
        this.currentRound = currentRound;
        this.roundStatus = roundStatus;
        this.pendingAcks = pendingAcks;
        this.rejectReason = rejectReason;
    }

    ConnectionState withStatus(Status newStatus) {
        return new ConnectionState(newStatus, currentRound, roundStatus, pendingAcks,
                newStatus == Status.REJECTED ? rejectReason : null);
    }

    ConnectionState withRound(Integer round, EstadoRound status) {
        return new ConnectionState(this.status, round, status, pendingAcks, rejectReason);
    }

    ConnectionState withPendingAcks(int pending) {
        return new ConnectionState(status, currentRound, roundStatus, pending, rejectReason);
    }

    ConnectionState rejected(JoinRejected.Reason reason) {
        return new ConnectionState(Status.REJECTED, null, null, pendingAcks, reason);
    }

    @Override
    public String toString() {
        return status + " round=" + currentRound + "/" + roundStatus + " pending=" + pendingAcks;
    }
}
