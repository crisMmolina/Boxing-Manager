package com.sigebox.core.protocol;

/**
 * FIGHT_CLOSED (central → juez).
 *
 * <p>El organizador cerró la pelea.
 */
public record FightClosed(int v, String id, String type, String fightId) implements Message {

    public static final String TYPE = "FIGHT_CLOSED";

    public FightClosed {
        Fields.base(v, id, type, TYPE);
        Fields.text(fightId, "fightId", 1, 64);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public FightClosed(String fightId) {
        this(VERSION, Fields.newId(), TYPE, fightId);
    }
}
