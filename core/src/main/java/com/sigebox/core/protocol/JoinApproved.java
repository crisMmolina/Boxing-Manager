package com.sigebox.core.protocol;

/**
 * JOIN_APPROVED (central → juez).
 *
 * <p>El organizador aprobó al juez.
 */
public record JoinApproved(
        int v,
        String id,
        String type,
        String fightId,
        Integer seat,
        String judgeName,
        String redCorner,
        String blueCorner,
        Integer rounds) implements Message {

    public static final String TYPE = "JOIN_APPROVED";

    public JoinApproved {
        Fields.base(v, id, type, TYPE);
        Fields.text(fightId, "fightId", 1, 64);
        Fields.range(seat, "seat", 1, 5);
        Fields.text(judgeName, "judgeName", 1, 80);
        Fields.text(redCorner, "redCorner", 1, 80);
        Fields.text(blueCorner, "blueCorner", 1, 80);
        Fields.range(rounds, "rounds", 1, 12);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public JoinApproved(
            String fightId,
            Integer seat,
            String judgeName,
            String redCorner,
            String blueCorner,
            Integer rounds) {
        this(VERSION, Fields.newId(), TYPE, fightId, seat, judgeName, redCorner, blueCorner, rounds);
    }
}
