package com.sigebox.core.protocol;

/**
 * EVENT_ENDED (central → juez). <p>Va después de FIGHT_CLOSED cuando esa era la última pelea del evento: el teléfono
 * vuelve al inicio.
 */
public record EventEnded(int v, String id, String type, String fightId) implements Message {

    public static final String TYPE = "EVENT_ENDED";

    public EventEnded {
        Fields.base(v, id, type, TYPE);
        Fields.text(fightId, "fightId", 1, 64);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public EventEnded(String fightId) {
        this(VERSION, Fields.newId(), TYPE, fightId);
    }
}
