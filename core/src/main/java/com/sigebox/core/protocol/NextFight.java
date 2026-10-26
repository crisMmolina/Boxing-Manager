package com.sigebox.core.protocol;

/**
 * NEXT_FIGHT (central → juez).
 *
 * <p>Terminó {@code fightId} y la función sigue con {@code nextFightId}. Con {@code seat}, el teléfono
 * guarda la pelea nueva, reconecta con {@code HELLO} firmado con su misma llave y pasa directo a
 * puntuar; sin {@code seat} (se omite), no participa en esta pelea y espera el siguiente aviso.
 * Sin puntos, totales ni datos de otros jueces (spec 002, ADR 0002).
 */
public record NextFight(int v, String id, String type, String fightId, String nextFightId, Integer seat,
        String redCorner, String blueCorner, int rounds) implements Message {

    public static final String TYPE = "NEXT_FIGHT";

    public NextFight {
        Fields.base(v, id, type, TYPE);
        Fields.text(fightId, "fightId", 1, 64);
        Fields.text(nextFightId, "nextFightId", 1, 64);
        if (seat != null) {
            Fields.range(seat, "seat", 1, 5);
        }
        Fields.text(redCorner, "redCorner", 1, 80);
        Fields.text(blueCorner, "blueCorner", 1, 80);
        Fields.range(rounds, "rounds", 1, 12);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio; {@code seat} null = no participa. */
    public NextFight(String fightId, String nextFightId, Integer seat, String redCorner, String blueCorner,
            int rounds) {
        this(VERSION, Fields.newId(), TYPE, fightId, nextFightId, seat, redCorner, blueCorner, rounds);
    }
}
