package com.sigebox.core.protocol;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import java.util.List;

/**
 * STATE (central → juez).
 *
 * <p>Estado de la pelea filtrado para un juez: solo sus propias tarjetas, nunca totales ni ganador.
 *
 * <p>Opcionales ({@code null} se omite): {@code roundStatus}.
 */
public record State(
        int v,
        String id,
        String type,
        String fightId,
        Integer seat,
        String judgeName,
        String redCorner,
        String blueCorner,
        Integer rounds,
        EstadoPelea fightStatus,
        Integer currentRound,
        EstadoRound roundStatus,
        List<State.OwnCard> cards,
        Boolean finalCardSent) implements Message {

    public static final String TYPE = "STATE";

    /** Una tarjeta propia del juez. */
    public record OwnCard(Integer round, Integer red, Integer blue, EstadoTarjeta status) {
        public OwnCard {
            Fields.range(round, "round", 1, 12);
            Fields.range(red, "red", 0, 10);
            Fields.range(blue, "blue", 0, 10);
            Fields.require(status, "status");
        }
    }

    public State {
        Fields.base(v, id, type, TYPE);
        Fields.text(fightId, "fightId", 1, 64);
        Fields.range(seat, "seat", 1, 5);
        Fields.text(judgeName, "judgeName", 1, 80);
        Fields.text(redCorner, "redCorner", 1, 80);
        Fields.text(blueCorner, "blueCorner", 1, 80);
        Fields.range(rounds, "rounds", 1, 12);
        Fields.require(fightStatus, "fightStatus");
        Fields.range(currentRound, "currentRound", 0, 12);
        cards = Fields.copy(Fields.require(cards, "cards"));
        Fields.require(finalCardSent, "finalCardSent");
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public State(
            String fightId,
            Integer seat,
            String judgeName,
            String redCorner,
            String blueCorner,
            Integer rounds,
            EstadoPelea fightStatus,
            Integer currentRound,
            EstadoRound roundStatus,
            List<State.OwnCard> cards,
            Boolean finalCardSent) {
        this(
                VERSION,
                Fields.newId(),
                TYPE,
                fightId,
                seat,
                judgeName,
                redCorner,
                blueCorner,
                rounds,
                fightStatus,
                currentRound,
                roundStatus,
                cards,
                finalCardSent);
    }
}
