package com.sigebox.core.protocol;

/**
 * JOIN_REJECTED (central → juez).
 *
 * <p>La unión fue rechazada.
 */
public record JoinRejected(
        int v,
        String id,
        String type,
        String fightId,
        Integer seat,
        Reason reason) implements Message {

    public static final String TYPE = "JOIN_REJECTED";

    /** Motivo del rechazo. */
    public enum Reason {
        TOKEN_INVALID, TOKEN_USED, SEAT_TAKEN, REJECTED
    }

    public JoinRejected {
        Fields.base(v, id, type, TYPE);
        Fields.require(fightId, "fightId");
        Fields.require(seat, "seat");
        Fields.require(reason, "reason");
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public JoinRejected(String fightId, Integer seat, Reason reason) {
        this(VERSION, Fields.newId(), TYPE, fightId, seat, reason);
    }
}
