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
        Fields.text(fightId, "fightId", 1, 64);
        Fields.range(seat, "seat", 1, 5);
        Fields.range(round, "round", 1, 12);
        Fields.range(red, "red", 0, 10);
        Fields.range(blue, "blue", 0, 10);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public SubmitRound(String fightId, Integer seat, Integer round, Integer red, Integer blue) {
        this(VERSION, Fields.newId(), TYPE, fightId, seat, round, red, blue);
    }
}
