package com.sigebox.core.protocol;

/**
 * CODE_LOOKUP (juez → central). <p>El teléfono pregunta qué lugar corresponde a un código de 6 dígitos. Va después de
 * CHALLENGE y antes de JOIN; la central contesta {@link CodeInfo} o {@link CodeUnknown}.
 */
public record CodeLookup(int v, String id, String type, String code) implements Message {

    public static final String TYPE = "CODE_LOOKUP";

    public CodeLookup {
        Fields.base(v, id, type, TYPE);
        Fields.match(code, "code", Fields.CODE);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public CodeLookup(String code) {
        this(VERSION, Fields.newId(), TYPE, code);
    }
}
