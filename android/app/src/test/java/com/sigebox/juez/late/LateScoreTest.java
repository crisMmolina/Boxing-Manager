package com.sigebox.juez.late;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.RoundClosed;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.juez.net.ConnectionState;
import com.sigebox.juez.score.ScoreUi;
import com.sigebox.juez.score.ScoreViewModel;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class LateScoreTest {

    private final List<Message> submitted = new ArrayList<>();

    @Test
    public void roundCerradoSinEnvioPropioPideLaCapturaTardia() {
        ScoreViewModel score = new ScoreViewModel(submitted::add);
        score.setSession("F1", 2, "María López");
        score.onConnection(new ConnectionState(ConnectionState.Status.CONNECTED, 3, EstadoRound.ABIERTO, 0, null));
        score.onMessage(new RoundClosed("F1", 3, true, true));
        assertEquals(Integer.valueOf(3), score.ui().getValue().lateRound);
    }

    @Test
    public void noSePuedeSalirHastaEnviar() {
        LateGate gate = new LateGate();
        ScoreViewModel late = new ScoreViewModel(submitted::add);
        late.setSession("F1", 2, "María López");
        late.openLate(3);
        assertFalse(gate.canLeave(late.ui().getValue()));
        late.key(10);
        assertFalse("falta Azul", gate.canLeave(late.ui().getValue()));

        late.key(9);
        late.submit();
        SubmitRound sent = (SubmitRound) submitted.get(0);
        assertEquals(Integer.valueOf(3), sent.round());
        // Ya está en la bandeja de salida: se reenvía solo aunque se caiga la red.
        assertTrue(gate.canLeave(late.ui().getValue()));
        assertFalse(gate.isConfirmed(late.ui().getValue()));

        late.onMessage(new Ack(sent.id(), true, EstadoTarjeta.TARDIA, null));
        assertTrue(gate.isConfirmed(late.ui().getValue()));
        assertEquals(ScoreUi.CardStatus.LATE, late.ui().getValue().card.status);
    }

    @Test
    public void siLaCentralRechazaElTardioSePuedeSalir() {
        LateGate gate = new LateGate();
        ScoreViewModel late = new ScoreViewModel(submitted::add);
        late.setSession("F1", 2, "María López");
        late.openLate(3);
        late.key(10);
        late.key(9);
        late.submit();
        late.onMessage(new Ack(submitted.get(0).id(), false, null, Ack.Error.FIGHT_NOT_ACTIVE));
        assertTrue(gate.canLeave(late.ui().getValue()));
    }

    @Test
    public void vibraUnaVezAlAbrir() {
        LateGate gate = new LateGate();
        assertTrue(gate.shouldVibrate());
        assertFalse(gate.shouldVibrate());
    }
}
