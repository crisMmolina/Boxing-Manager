package com.sigebox.juez.score;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sigebox.juez.net.ConnectionState;

import org.junit.Test;

/**
 * AND-032: «¿La pelea sigue?» se calla solo mientras el juez califica. En «Round N enviado» el round sigue abierto en
 * la central, pero el juez ya no califica: ahí sí puede preguntar.
 */
public class ScoreStillThereTest {

    private static ScoreUi ui(boolean roundOpen, ScoreUi.Card card, boolean editing) {
        return new ScoreUi("Ana", 1, ConnectionState.Status.CONNECTED, 2, 3, roundOpen, null, null, null, editing, false,
                false, card, null, false, null, false);
    }

    @Test
    public void conRoundAbiertoYSinTarjetaElJuezCalifica() {
        assertTrue(ScoreActivity.judgeIsScoring(ui(true, null, false)));
    }

    @Test
    public void conRoundEnviadoElJuezYaNoCalifica() {
        assertFalse(ScoreActivity.judgeIsScoring(
                ui(true, new ScoreUi.Card(10, 9, ScoreUi.CardStatus.SENT), false)));
    }

    @Test
    public void corrigiendoElJuezCalifica() {
        assertTrue(ScoreActivity.judgeIsScoring(
                ui(true, new ScoreUi.Card(10, 9, ScoreUi.CardStatus.SENT), true)));
    }

    @Test
    public void sinRoundAbiertoNoCalifica() {
        assertFalse(ScoreActivity.judgeIsScoring(ui(false, null, false)));
        assertFalse(ScoreActivity.judgeIsScoring(null));
    }
}
