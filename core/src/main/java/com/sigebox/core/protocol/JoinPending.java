package com.sigebox.core.protocol;

/**
 * JOIN_PENDING (central → juez).
 *
 * <p>El lugar espera la aprobación del organizador.
 */
public record JoinPending(int v, String id, String type, String fightId, Integer seat) implements Message {

    public static final String TYPE = "JOIN_PENDING";

    public JoinPending {
        Fields.base(v, id, type, TYPE);
        Fields.text(fightId, "fightId", 1, 64);
        Fields.range(seat, "seat", 1, 5);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public JoinPending(String fightId, Integer seat) {
        this(VERSION, Fields.newId(), TYPE, fightId, seat);
    }
}
