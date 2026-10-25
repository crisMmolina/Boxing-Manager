package com.sigebox.juez.score;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.FightEnded;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.RoundOpened;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.juez.net.ConnectionState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ScoreViewModelTest {

    private final List<Message> submitted = new ArrayList<>();
    private ScoreViewModel vm;

    @Before
    public void setUp() {
        vm = new ScoreViewModel(submitted::add);
        vm.setSession("F1", 2, "María López");
        vm.onConnection(new ConnectionState(ConnectionState.Status.CONNECTED, 3, EstadoRound.ABIERTO, 0, null));
    }

    private ScoreUi ui() {
        return vm.ui().getValue();
    }

    @Test
    public void conSoloRojaElBotonEnviarEstaDeshabilitado() {
        vm.key(10);
        assertEquals(Integer.valueOf(10), ui().red);
        assertNull("sin valor por defecto", ui().blue);
        assertFalse(ui().sendEnabled);
    }

    @Test
    public void rojaLuegoAzulYSeHabilitaEnviar() {
        assertEquals(ScoreUi.Corner.RED, ui().selected);
        vm.key(10);
        assertEquals("pasa sola a Azul", ScoreUi.Corner.BLUE, ui().selected);
        vm.key(9);
        assertEquals(Integer.valueOf(9), ui().blue);
        assertTrue(ui().sendEnabled);
    }

    @Test
    public void borrarLimpiaElPanelSeleccionado() {
        vm.key(10);
        vm.key(9);
        vm.backspace();
        assertNull(ui().blue);
        assertFalse(ui().sendEnabled);
        vm.selectCorner(ScoreUi.Corner.RED);
        vm.key(8);
        assertEquals(Integer.valueOf(8), ui().red);
    }

    @Test
    public void conElRoundCerradoNoSeEnvia() {
        vm.onConnection(new ConnectionState(ConnectionState.Status.CONNECTED, 3, EstadoRound.CERRADO, 0, null));
        vm.key(10);
        vm.key(9);
        assertFalse(ui().sendEnabled);
    }

    @Test
    public void enviarMandaSubmitRoundYQuedaEnviandoHastaElAck() {
        vm.key(10);
        vm.key(9);
        vm.submit();

        SubmitRound sent = (SubmitRound) submitted.get(0);
        assertEquals("F1", sent.fightId());
        assertEquals(Integer.valueOf(2), sent.seat());
        assertEquals(Integer.valueOf(3), sent.round());
        assertEquals(Integer.valueOf(10), sent.red());
        assertEquals(Integer.valueOf(9), sent.blue());
        assertEquals(ScoreUi.CardStatus.SENDING, ui().card.status);
        assertFalse(ui().sendEnabled);
        assertTrue("se puede corregir con el round abierto", ui().canCorrect);

        vm.onMessage(new Ack(sent.id(), true, EstadoTarjeta.ENVIADA, null));
        assertEquals(ScoreUi.CardStatus.SENT, ui().card.status);
        assertEquals(Integer.valueOf(10), ui().card.red);
    }

    @Test
    public void corregirMandaOtroSubmitRoundConOtroId() {
        vm.key(10);
        vm.key(9);
        vm.submit();
        vm.onMessage(new Ack(submitted.get(0).id(), true, EstadoTarjeta.ENVIADA, null));

        vm.startCorrection();
        assertEquals("parte de lo enviado", Integer.valueOf(10), ui().red);
        vm.selectCorner(ScoreUi.Corner.BLUE);
        vm.key(8);
        vm.submit();

        SubmitRound correction = (SubmitRound) submitted.get(1);
        assertFalse(correction.id().equals(submitted.get(0).id()));
        assertEquals(Integer.valueOf(8), correction.blue());
        vm.onMessage(new Ack(correction.id(), true, EstadoTarjeta.CORREGIDA, null));
        assertEquals(ScoreUi.CardStatus.CORRECTED, ui().card.status);
    }

    @Test
    public void unAckRechazadoDejaElRoundSinTarjeta() {
        vm.key(10);
        vm.key(9);
        vm.submit();
        vm.onMessage(new Ack(submitted.get(0).id(), false, null, Ack.Error.ROUND_NOT_OPEN));
        assertNull(ui().card);
        assertEquals(Ack.Error.ROUND_NOT_OPEN, ui().lastError);
    }

    @Test
    public void unRoundNuevoEmpiezaVacioEnRoja() {
        vm.key(10);
        vm.onMessage(new RoundOpened("F1", 4));
        vm.onConnection(new ConnectionState(ConnectionState.Status.CONNECTED, 4, EstadoRound.ABIERTO, 0, null));
        assertNull(ui().red);
        assertNull(ui().card);
        assertEquals(ScoreUi.Corner.RED, ui().selected);
        assertEquals(4, ui().round);
    }

    @Test
    public void elStateDaLosRoundsYLasTarjetasPropias() {
        vm.onMessage(new State("F1", 2, "María López", "Roja", "Azul", 4, EstadoPelea.EN_CURSO, 3,
                EstadoRound.ABIERTO, List.of(new State.OwnCard(3, 9, 10, EstadoTarjeta.TARDIA)), false));
        assertEquals(Integer.valueOf(4), ui().rounds);
        assertEquals(ScoreUi.CardStatus.LATE, ui().card.status);
        assertEquals(Integer.valueOf(9), ui().card.red);
    }

    @Test
    public void laTarjetaPropiaGuardadaSeRecuperaTrasCerrarLaApp() {
        vm.onOwnCards(Collections.singletonMap(3, new OwnCards.Row(3, 10, 9, ScoreUi.CardStatus.SENDING, "x")));
        assertEquals(ScoreUi.CardStatus.SENDING, ui().card.status);
    }

    @Test
    public void alTerminarLaPeleaSePideLaTarjetaFinal() {
        assertFalse(ui().fightEnded);
        vm.onMessage(new FightEnded("F1", null));
        assertTrue(ui().fightEnded);
    }

    @Test
    public void nuncaHayTotales() {
        // ScoreUi no tiene campos de total ni ganador: esta prueba falla si alguien los agrega.
        for (java.lang.reflect.Field field : ScoreUi.class.getFields()) {
            String name = field.getName().toLowerCase();
            assertFalse(name, name.contains("total") || name.contains("winner") || name.contains("ganador"));
        }
    }
}
