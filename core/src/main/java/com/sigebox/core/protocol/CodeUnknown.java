package com.sigebox.core.protocol;

/**
 * CODE_UNKNOWN (central → juez). <p>El código no sirve: no existe, ya se usó o la pelea cerró. A propósito no dice cuál
 * de las tres, para no ayudar a adivinar códigos.
 */
public record CodeUnknown(int v, String id, String type, String code) implements Message {

    public static final String TYPE = "CODE_UNKNOWN";

    public CodeUnknown {
        Fields.base(v, id, type, TYPE);
        Fields.match(code, "code", Fields.CODE);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public CodeUnknown(String code) {
        this(VERSION, Fields.newId(), TYPE, code);
    }
}
