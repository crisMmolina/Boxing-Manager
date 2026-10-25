package com.sigebox.core.protocol;

/**
 * SUBMIT_FINAL_CARD (juez → central).
 *
 * <p>El juez firma su tarjeta final; {@code hash} sale de {@link FinalCardHash}.
 */
public record SubmitFinalCard(
        int v,
        String id,
        String type,
        String fightId,
        Integer seat,
        String hash) implements Message {

    public static final String TYPE = "SUBMIT_FINAL_CARD";

    public SubmitFinalCard {
        Fields.base(v, id, type, TYPE);
        Fields.require(fightId, "fightId");
        Fields.require(seat, "seat");
        Fields.require(hash, "hash");
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public SubmitFinalCard(String fightId, Integer seat, String hash) {
        this(VERSION, Fields.newId(), TYPE, fightId, seat, hash);
    }
}
