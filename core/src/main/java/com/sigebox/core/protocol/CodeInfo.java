package com.sigebox.core.protocol;

/**
 * CODE_INFO (central → juez). <p>El código corresponde a un lugar libre: el teléfono muestra pelea y esquinas, y sigue
 * con JOIN usando ese código como {@code token}. No lleva puntajes.
 */
public record CodeInfo(int v, String id, String type, String code, String fightId, int seat, String redCorner,
        String blueCorner) implements Message {

    public static final String TYPE = "CODE_INFO";

    public CodeInfo {
        Fields.base(v, id, type, TYPE);
        Fields.match(code, "code", Fields.CODE);
        Fields.text(fightId, "fightId", 1, 64);
        Fields.range(seat, "seat", 1, 5);
        Fields.text(redCorner, "redCorner", 1, 80);
        Fields.text(blueCorner, "blueCorner", 1, 80);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public CodeInfo(String code, String fightId, int seat, String redCorner, String blueCorner) {
        this(VERSION, Fields.newId(), TYPE, code, fightId, seat, redCorner, blueCorner);
    }
}
