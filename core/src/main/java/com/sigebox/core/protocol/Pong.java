package com.sigebox.core.protocol;

/**
 * PONG (ambos).
 *
 * <p>Respuesta a {@code PING} con el mismo {@code ts}.
 */
public record Pong(int v, String id, String type, Long ts) implements Message {

    public static final String TYPE = "PONG";

    public Pong {
        Fields.base(v, id, type, TYPE);
        Fields.range(ts, "ts", 0, Long.MAX_VALUE);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public Pong(Long ts) {
        this(VERSION, Fields.newId(), TYPE, ts);
    }
}
