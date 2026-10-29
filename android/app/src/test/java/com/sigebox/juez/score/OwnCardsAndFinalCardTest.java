package com.sigebox.juez.score;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.FinalCardHash;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitFinalCard;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.juez.data.Outbox;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.finalcard.FinalCard;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class OwnCardsAndFinalCardTest {

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

    private SubmitRound send(int round, int red, int blue, EstadoTarjeta ackStatus) {
        SubmitRound submit = new SubmitRound("F1", 2, round, red, blue);
        outbox.enqueue(submit);
        if (ackStatus != null) {
            outbox.onAck(new Ack(submit.id(), true, ackStatus, null));
        }
        return submit;
    }

    @Test
    public void lasTarjetasPropiasSalenDeLaBandejaConLaUltimaVersion() {
        send(1, 10, 9, EstadoTarjeta.ENVIADA);
        send(2, 9, 10, EstadoTarjeta.ENVIADA);
        send(2, 10, 9, EstadoTarjeta.CORREGIDA);
        send(3, 10, 9, null);
        SubmitRound rechazada = new SubmitRound("F1", 2, 4, 10, 9);
        outbox.enqueue(rechazada);
        outbox.onAck(new Ack(rechazada.id(), false, null, Ack.Error.ROUND_NOT_OPEN));

        Map<Integer, OwnCards.Row> cards = OwnCards.load(db.getOpenHelper().getReadableDatabase(), "F1");
        assertEquals(3, cards.size());
        assertEquals(ScoreUi.CardStatus.SENT, cards.get(1).status);
        assertEquals(Integer.valueOf(10), cards.get(2).red);
        assertEquals(ScoreUi.CardStatus.CORRECTED, cards.get(2).status);
        assertEquals(ScoreUi.CardStatus.SENDING, cards.get(3).status);
        assertFalse("la rechazada no cuenta", cards.containsKey(4));
    }

    @Test
    public void soloLasTarjetasDeEstaPelea() {
        send(1, 10, 9, EstadoTarjeta.ENVIADA);
        outbox.enqueue(new SubmitRound("OTRA", 2, 1, 9, 10));
        assertEquals(1, OwnCards.load(db.getOpenHelper().getReadableDatabase(), "F1").size());
    }

    @Test
    public void laTarjetaFinalLlevaHashYFirmaDeSusRounds() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keys = gen.generateKeyPair();
        List<State.OwnCard> cards = List.of(
                new State.OwnCard(1, 10, 9, EstadoTarjeta.ENVIADA),
                new State.OwnCard(2, 9, 10, EstadoTarjeta.TARDIA));

        SubmitFinalCard msg = FinalCard.build("F1", 2, cards, text -> DeviceProof.sign(keys.getPrivate(), text));

        assertEquals(FinalCardHash.of("F1", 2, cards), msg.hash());
        assertTrue(DeviceProof.verify(DeviceProof.encodePublicKey(keys.getPublic()),
                DeviceProof.finalCardText("F1", 2, cards), msg.sig()));
    }

    @Test
    public void filasDeLaTarjetaFinalEnOrdenYSinTotales() {
        Map<Integer, OwnCards.Row> rows = Map.of(
                2, new OwnCards.Row(2, 9, 10, ScoreUi.CardStatus.LATE, "b"),
                1, new OwnCards.Row(1, 10, 9, ScoreUi.CardStatus.SENT, "a"));
        List<State.OwnCard> cards = FinalCard.toOwnCards(rows);
        assertEquals(Integer.valueOf(1), cards.get(0).round());
        assertEquals(EstadoTarjeta.TARDIA, cards.get(1).status());
    }
}
