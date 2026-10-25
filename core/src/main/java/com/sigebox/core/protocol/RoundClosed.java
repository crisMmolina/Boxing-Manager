package com.sigebox.core.protocol;

/**
 * ROUND_CLOSED (central → juez).
 *
 * <p>El organizador cerró un round. Con {@code mustSubmit} el teléfono vibra y exige calificar (envío tardío).
 */
public record RoundClosed(
        int v,
        String id,
        String type,
        String fightId,
        Integer round,
        Boolean forced,
        Boolean mustSubmit) implements Message {

    public static final String TYPE = "ROUND_CLOSED";

    public RoundClosed {
        Fields.base(v, id, type, TYPE);
        Fields.require(fightId, "fightId");
        Fields.require(round, "round");
        Fields.require(forced, "forced");
        Fields.require(mustSubmit, "mustSubmit");
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public RoundClosed(String fightId, Integer round, Boolean forced, Boolean mustSubmit) {
        this(VERSION, Fields.newId(), TYPE, fightId, round, forced, mustSubmit);
    }
}
