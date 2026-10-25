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
        Fields.require(fightId, "fightId");
        Fields.require(seat, "seat");
        Fields.require(judgeName, "judgeName");
        Fields.require(redCorner, "redCorner");
        Fields.require(blueCorner, "blueCorner");
        Fields.require(rounds, "rounds");
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
