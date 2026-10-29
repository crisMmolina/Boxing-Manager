package com.sigebox.juez.finalcard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.SubmitFinalCard;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.juez.data.Outbox;
import com.sigebox.juez.data.SigeboxDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * AND-015: el estado de la tarjeta final sale de la bandeja de salida, así que sobrevive a recrear la pantalla o a
 * matar el proceso. "Recrear" aquí es volver a leer la base, que es lo que hace FinalCardActivity en onStart.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class FinalCardStatusTest {

    private static final String HASH = "a".repeat(64);
    private static final String SIG = "MEUCIQDx".repeat(6);

    private SigeboxDatabase db;
    private Outbox outbox;
    private long now = 1_000;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SigeboxDatabase.class)
                .allowMainThreadQueries()
                .build();
        outbox = new Outbox(db.outboxDao(), () -> now++);
    }

    @After
    public void tearDown() {
        db.close();
    }

    private FinalCardStatus recreate() {
        return FinalCardStatus.load(db.getOpenHelper().getReadableDatabase(), "F1");
    }

    private SubmitFinalCard send() {
        SubmitFinalCard card = new SubmitFinalCard("F1", 2, HASH, SIG);
        outbox.enqueue(card);
        return card;
    }

    @Test
    public void sinEnviarSePuedeEnviar() {
        outbox.enqueue(new SubmitRound("F1", 2, 1, 10, 9));
        FinalCardStatus status = recreate();
        assertEquals(FinalCardStatus.State.NOT_SENT, status.state);
        assertNull(status.messageId);
    }

    @Test
    public void alRecrearAntesDelAckSigueEnviando() {
        SubmitFinalCard card = send();
        FinalCardStatus status = recreate();
        assertEquals(FinalCardStatus.State.SENDING, status.state);
        assertEquals("para reconocer su ACK al llegar", card.id(), status.messageId);
    }

    @Test
    public void alRecrearDespuesDelAckQuedaEnviada() {
        SubmitFinalCard card = send();
        outbox.onAck(new Ack(card.id(), true, null, null));
        assertEquals(FinalCardStatus.State.SENT, recreate().state);
    }

    @Test
    public void alreadyFinalCuentaComoEnviada() {
        SubmitFinalCard card = send();
        outbox.onAck(new Ack(card.id(), false, null, Ack.Error.ALREADY_FINAL));
        assertEquals(FinalCardStatus.State.SENT, recreate().state);
    }

    @Test
    public void otroErrorPermiteReintentarYElReintentoManda() {
        SubmitFinalCard first = send();
        outbox.onAck(new Ack(first.id(), false, null, Ack.Error.BAD_SIGNATURE));
        assertEquals(FinalCardStatus.State.NOT_SENT, recreate().state);

        SubmitFinalCard retry = send();
        FinalCardStatus status = recreate();
        assertEquals(FinalCardStatus.State.SENDING, status.state);
        assertEquals(retry.id(), status.messageId);
    }

    @Test
    public void laTarjetaDeOtraPeleaNoCuenta() {
        SubmitFinalCard other = new SubmitFinalCard("F0", 2, HASH, SIG);
        outbox.enqueue(other);
        outbox.onAck(new Ack(other.id(), true, EstadoTarjeta.ENVIADA, null));
        assertEquals(FinalCardStatus.State.NOT_SENT, recreate().state);
    }
}
