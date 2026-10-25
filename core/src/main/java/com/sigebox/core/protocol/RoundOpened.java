package com.sigebox.core.protocol;

/**
 * ROUND_OPENED (central → juez).
 *
 * <p>El organizador abrió un round.
 */
public record RoundOpened(int v, String id, String type, String fightId, Integer round) implements Message {

    public static final String TYPE = "ROUND_OPENED";

    public RoundOpened {
        Fields.base(v, id, type, TYPE);
        Fields.require(fightId, "fightId");
        Fields.require(round, "round");
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public RoundOpened(String fightId, Integer round) {
        this(VERSION, Fields.newId(), TYPE, fightId, round);
    }
}
