package com.sigebox.core.protocol;

/**
 * SUBMIT_FINAL_CARD (juez → central).
 *
 * <p>Tarjeta final del juez: {@code hash} sale de {@link FinalCardHash} y {@code sig} firma {@link DeviceProof#finalCardText}.
 */
public record SubmitFinalCard(
        int v,
        String id,
        String type,
        String fightId,
        Integer seat,
        String hash,
        String sig) implements Message {

    public static final String TYPE = "SUBMIT_FINAL_CARD";

    public SubmitFinalCard {
        Fields.base(v, id, type, TYPE);
        Fields.text(fightId, "fightId", 1, 64);
        Fields.range(seat, "seat", 1, 5);
        Fields.match(hash, "hash", Fields.HEX64);
        Fields.text(sig, "sig", 40, 200);
        Fields.match(sig, "sig", Fields.BASE64);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public SubmitFinalCard(String fightId, Integer seat, String hash, String sig) {
        this(VERSION, Fields.newId(), TYPE, fightId, seat, hash, sig);
    }
}
