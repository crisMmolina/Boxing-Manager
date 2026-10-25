package com.sigebox.core.protocol;

/**
 * SUBMIT_ROUND (juez → central).
 *
 * <p>Calificación de un round. Reenviar con el round abierto es una corrección; la central decide si es tardía.
 */
public record SubmitRound(
        int v,
        String id,
        String type,
        String fightId,
        Integer seat,
        Integer round,
        Integer red,
        Integer blue) implements Message {

    public static final String TYPE = "SUBMIT_ROUND";

    public SubmitRound {
        Fields.base(v, id, type, TYPE);
        Fields.require(fightId, "fightId");
        Fields.require(seat, "seat");
        Fields.require(round, "round");
        Fields.require(red, "red");
        Fields.require(blue, "blue");
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public SubmitRound(String fightId, Integer seat, Integer round, Integer red, Integer blue) {
        this(VERSION, Fields.newId(), TYPE, fightId, seat, round, red, blue);
    }
}
