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
            Fields.require(round, "round");
            Fields.require(red, "red");
            Fields.require(blue, "blue");
            Fields.require(status, "status");
        }
    }

    public State {
        Fields.base(v, id, type, TYPE);
        Fields.require(fightId, "fightId");
        Fields.require(seat, "seat");
        Fields.require(judgeName, "judgeName");
        Fields.require(redCorner, "redCorner");
        Fields.require(blueCorner, "blueCorner");
        Fields.require(rounds, "rounds");
        Fields.require(fightStatus, "fightStatus");
        Fields.require(currentRound, "currentRound");
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
