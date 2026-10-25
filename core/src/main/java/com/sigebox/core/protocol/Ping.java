package com.sigebox.core.protocol;

/**
 * PING (ambos).
 *
 * <p>Latido de la conexión.
 */
public record Ping(int v, String id, String type, Long ts) implements Message {

    public static final String TYPE = "PING";

    public Ping {
        Fields.base(v, id, type, TYPE);
        Fields.require(ts, "ts");
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public Ping(Long ts) {
        this(VERSION, Fields.newId(), TYPE, ts);
    }
}
