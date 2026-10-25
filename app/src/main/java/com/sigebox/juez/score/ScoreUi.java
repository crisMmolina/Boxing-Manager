package com.sigebox.juez.score;

import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.protocol.Ack;
import com.sigebox.juez.net.ConnectionState;

/** Lo que muestra la pantalla de puntuación. A propósito no tiene totales ni ganador (DESIGN.md §1). */
public final class ScoreUi {

    public enum Corner { RED, BLUE }

    /** Estado de la tarjeta del juez en un round (chip de DESIGN.md §2.3). */
    public enum CardStatus {
        SENDING, SENT, CORRECTED, LATE;

        public static CardStatus from(EstadoTarjeta status) {
            if (status == null) {
                return SENT;
            }
            switch (status) {
                case TARDIA:
                    return LATE;
                case CORREGIDA:
                case CAPTURADA:
                    return CORRECTED;
                case ENVIADA:
                default:
                    return SENT;
            }
        }
    }

    /** Lo que el juez ya envió en el round actual. */
    public static final class Card {
        public final Integer red;
        public final Integer blue;
        public final CardStatus status;

        public Card(Integer red, Integer blue, CardStatus status) {
            this.red = red;
            this.blue = blue;
            this.status = status;
        }
    }

    public final String judgeName;
    public final int seat;
    public final ConnectionState.Status connection;
    public final int round;
    /** {@code null} hasta recibir {@code STATE}. */
    public final Integer rounds;
    public final boolean roundOpen;
    public final Corner selected;
    public final Integer red;
    public final Integer blue;
    public final boolean editing;
    public final boolean sendEnabled;
    public final boolean canCorrect;
    /** Tarjeta enviada en el round actual, o {@code null}. */
    public final Card card;
    public final Ack.Error lastError;
    public final boolean fightEnded;
    /** Round cerrado sin calificar que la central exige ahora; {@code null} si no hay. */
    public final Integer lateRound;
    /** Esta pantalla es la captura tardía (AND-006). */
    public final boolean late;

    ScoreUi(String judgeName, int seat, ConnectionState.Status connection, int round, Integer rounds,
            boolean roundOpen, Corner selected, Integer red, Integer blue, boolean editing, boolean sendEnabled,
            boolean canCorrect, Card card, Ack.Error lastError, boolean fightEnded, Integer lateRound,
            boolean late) {
        this.judgeName = judgeName;
        this.seat = seat;
        this.connection = connection;
        this.round = round;
        this.rounds = rounds;
        this.roundOpen = roundOpen;
        this.selected = selected;
        this.red = red;
        this.blue = blue;
        this.editing = editing;
        this.sendEnabled = sendEnabled;
        this.canCorrect = canCorrect;
        this.card = card;
        this.lastError = lastError;
        this.fightEnded = fightEnded;
        this.lateRound = lateRound;
        this.late = late;
    }

    /** El teclado acepta números: round abierto y sin tarjeta, o corrigiendo. */
    public boolean inputEnabled() {
        return roundOpen && (card == null || editing);
    }
}
