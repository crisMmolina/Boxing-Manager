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
        Fields.text(fightId, "fightId", 1, 64);
        Fields.range(round, "round", 1, 12);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public RoundOpened(String fightId, Integer round) {
        this(VERSION, Fields.newId(), TYPE, fightId, round);
    }
}
