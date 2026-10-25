package com.sigebox.core.protocol;

/**
 * CHALLENGE (central → juez).
 *
 * <p>Primer mensaje de la central en cada conexión: el teléfono firma {@code nonce} en su {@code JOIN} o {@code HELLO}. Un nonce sirve una sola vez.
 */
public record Challenge(int v, String id, String type, String nonce) implements Message {

    public static final String TYPE = "CHALLENGE";

    public Challenge {
        Fields.base(v, id, type, TYPE);
        Fields.text(nonce, "nonce", 22, 128);
        Fields.match(nonce, "nonce", Fields.NONCE);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public Challenge(String nonce) {
        this(VERSION, Fields.newId(), TYPE, nonce);
    }
}
