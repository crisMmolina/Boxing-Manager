package com.sigebox.juez.score;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.FightEnded;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.RoundClosed;
import com.sigebox.core.protocol.RoundOpened;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.core.validation.ScoreValidator;
import com.sigebox.juez.net.ConnectionState;

import java.util.HashMap;
import java.util.Map;

/**
 * Captura de un round con una mano: ROJA y luego AZUL, 0–10, sin valor por defecto. Enviar solo con ambos valores
 * válidos ({@link ScoreValidator}) y el round abierto. Con el round abierto se puede corregir: cada corrección es un
 * {@code SUBMIT_ROUND} nuevo (RJ-05). Solo se llama desde el hilo principal.
 */
public class ScoreViewModel extends ViewModel {

    /** Quien entrega el mensaje a la bandeja de salida ({@code JudgeConnection::submit}). */
    public interface Submitter {
        void submit(Message message);
    }

    private final Submitter submitter;
    private final MutableLiveData<ScoreUi> ui = new MutableLiveData<>();

    private String fightId;
    private int seat;
    private String judgeName = "";
    private ConnectionState.Status connection = ConnectionState.Status.DISCONNECTED;
    private int round;
    private Integer rounds;
    private EstadoRound roundStatus = EstadoRound.PENDIENTE;
    private final Map<Integer, ScoreUi.Card> cards = new HashMap<>();
    /** id del mensaje sin ACK → round; y la tarjeta anterior para restaurarla si se rechaza. */
    private final Map<String, Integer> pending = new HashMap<>();
    private final Map<String, ScoreUi.Card> previous = new HashMap<>();
    private ScoreUi.Corner selected = ScoreUi.Corner.RED;
    private Integer red;
    private Integer blue;
    private boolean editing;
    private Ack.Error lastError;
    private boolean fightEnded;

    public ScoreViewModel(Submitter submitter) {
        this.submitter = submitter;
        publish();
    }

    public LiveData<ScoreUi> ui() {
        return ui;
    }

    public void setSession(String fightId, int seat, String judgeName) {
        this.fightId = fightId;
        this.seat = seat;
        this.judgeName = judgeName;
        publish();
    }

    public void onConnection(ConnectionState state) {
        connection = state.status;
        if (state.currentRound != null && state.currentRound != round) {
            newRound(state.currentRound);
        }
        if (state.roundStatus != null) {
            roundStatus = state.roundStatus;
        }
        publish();
    }

    /** Tarjetas guardadas en la bandeja (al abrir la pantalla o volver a ella). */
    public void onOwnCards(Map<Integer, OwnCards.Row> rows) {
        for (OwnCards.Row row : rows.values()) {
            cards.put(row.round, new ScoreUi.Card(row.red, row.blue, row.status));
            if (row.status == ScoreUi.CardStatus.SENDING) {
                pending.put(row.messageId, row.round);
            }
        }
        publish();
    }

    public void onMessage(Message message) {
        if (message instanceof Ack ack) {
            onAck(ack);
        } else if (message instanceof State state) {
            rounds = state.rounds();
            for (State.OwnCard own : state.cards()) {
                if (!pending.containsValue(own.round())) {
                    cards.put(own.round(),
                            new ScoreUi.Card(own.red(), own.blue(), ScoreUi.CardStatus.from(own.status())));
                }
            }
            if (state.currentRound() != null && state.currentRound() > 0 && state.currentRound() != round) {
                newRound(state.currentRound());
            }
            if (state.roundStatus() != null) {
                roundStatus = state.roundStatus();
            }
            boolean over = state.fightStatus() == EstadoPelea.TERMINADA
                    || state.fightStatus() == EstadoPelea.CERRADA;
            fightEnded = fightEnded || (over && !state.finalCardSent());
        } else if (message instanceof RoundOpened opened) {
            if (opened.round() != round) {
                newRound(opened.round());
            }
            roundStatus = EstadoRound.ABIERTO;
        } else if (message instanceof RoundClosed closed) {
            if (closed.round() == round) {
                roundStatus = EstadoRound.CERRADO;
                editing = false;
            }
        } else if (message instanceof FightEnded) {
            fightEnded = true;
        }
        publish();
    }

    public void selectCorner(ScoreUi.Corner corner) {
        selected = corner;
        publish();
    }

    /** Una tecla es el valor completo (el 10 tiene tecla propia); Roja pasa sola a Azul. */
    public void key(int value) {
        if (!current().inputEnabled() || ScoreValidator.validarValores(value, value).isPresent()) {
            return;
        }
        if (selected == ScoreUi.Corner.RED) {
            red = value;
            selected = ScoreUi.Corner.BLUE;
        } else {
            blue = value;
        }
        publish();
    }

    public void backspace() {
        if (selected == ScoreUi.Corner.RED) {
            red = null;
        } else {
            blue = null;
        }
        publish();
    }

    public void startCorrection() {
        ScoreUi.Card card = cards.get(round);
        if (card == null || !isRoundOpen()) {
            return;
        }
        editing = true;
        red = card.red;
        blue = card.blue;
        selected = ScoreUi.Corner.RED;
        publish();
    }

    public void submit() {
        if (!current().sendEnabled) {
            return;
        }
        SubmitRound message = new SubmitRound(fightId, seat, round, red, blue);
        previous.put(message.id(), cards.get(round));
        pending.put(message.id(), round);
        cards.put(round, new ScoreUi.Card(red, blue, ScoreUi.CardStatus.SENDING));
        editing = false;
        red = null;
        blue = null;
        selected = ScoreUi.Corner.RED;
        lastError = null;
        submitter.submit(message);
        publish();
    }

    private void onAck(Ack ack) {
        Integer ackRound = pending.remove(ack.ackId());
        if (ackRound == null) {
            return;
        }
        ScoreUi.Card before = previous.remove(ack.ackId());
        ScoreUi.Card sent = cards.get(ackRound);
        if (!ack.ok()) {
            lastError = ack.error();
            if (before == null) {
                cards.remove(ackRound);
            } else {
                cards.put(ackRound, before);
            }
            return;
        }
        ScoreUi.CardStatus status = ScoreUi.CardStatus.from(ack.status());
        if (before != null && status == ScoreUi.CardStatus.SENT) {
            status = ScoreUi.CardStatus.CORRECTED;
        }
        if (sent != null) {
            cards.put(ackRound, new ScoreUi.Card(sent.red, sent.blue, status));
        }
    }

    private void newRound(int number) {
        round = number;
        red = null;
        blue = null;
        selected = ScoreUi.Corner.RED;
        editing = false;
        lastError = null;
    }

    private boolean isRoundOpen() {
        return roundStatus == EstadoRound.ABIERTO && round > 0;
    }

    private ScoreUi current() {
        ScoreUi.Card card = cards.get(round);
        boolean open = isRoundOpen();
        boolean inputOpen = open && (card == null || editing);
        boolean send = fightId != null && inputOpen && ScoreValidator.validarValores(red, blue).isEmpty();
        boolean canCorrect = open && card != null && !editing;
        return new ScoreUi(judgeName, seat, connection, round, rounds, open, selected, red, blue, editing, send,
                canCorrect, card, lastError, fightEnded);
    }

    private void publish() {
        ui.setValue(current());
    }
}
