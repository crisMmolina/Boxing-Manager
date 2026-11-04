package com.sigebox.juez.finalcard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.FightEnded;
import com.sigebox.core.protocol.FinalCardHash;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.RoundClosed;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitFinalCard;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.juez.data.Outbox;
import com.sigebox.juez.data.OutboxEntity;
import com.sigebox.juez.data.SessionStore;
import com.sigebox.juez.data.SigeboxDatabase;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Spec 004 §Cierre sin pedir tarjetas: la tarjeta final sale sola, firmada, sin pantalla ni acción del juez. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class AutoFinalCardTest {

    private SigeboxDatabase db;
    private Outbox outbox;
    private KeyPair keys;
    private AutoFinalCard auto;
    private SessionStore store;
    private long now = 1_000;

    @Before
    public void setUp() throws Exception {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SigeboxDatabase.class)
                .allowMainThreadQueries()
                .build();
        outbox = new Outbox(db.outboxDao(), () -> now++);
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        keys = gen.generateKeyPair();
        store = new SessionStore(db.sessionDao());
        store.saveJoin("10.0.0.1", 8443, "F1", 2, "tok123", "María López");
        store.markApproved();
        auto = newAuto();
    }

    @After
    public void tearDown() {
        db.close();
    }

    private AutoFinalCard newAuto() {
        return new AutoFinalCard(() -> db.getOpenHelper().getReadableDatabase(), store::load, this::sign,
                outbox::enqueue, Runnable::run);
    }

    private String sign(String text) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(keys.getPrivate());
            signature.update(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private SubmitRound send(int round, boolean acked) {
        SubmitRound submit = new SubmitRound("F1", 2, round, 10, 9);
        outbox.enqueue(submit);
        if (acked) {
            outbox.onAck(new Ack(submit.id(), true, EstadoTarjeta.ENVIADA, null));
        }
        return submit;
    }

    private static State state(EstadoPelea fight, int current, EstadoRound round, boolean finalSent) {
        return new State("F1", 2, "María López", "Roja", "Azul", 3, fight, current, round, List.of(), finalSent);
    }

    private List<SubmitFinalCard> finals() {
        List<SubmitFinalCard> found = new ArrayList<>();
        for (OutboxEntity row : db.outboxDao().unacked()) {
            if (SubmitFinalCard.TYPE.equals(row.type)) {
                found.add((SubmitFinalCard) Messages.decode(row.payload));
            }
        }
        return found;
    }

    @Test
    public void elUltimoRoundClosedDejaLaTarjetaFinalFirmadaEnLaBandeja() {
        send(1, true);
        send(2, true);
        send(3, true);
        auto.onMessage(state(EstadoPelea.EN_CURSO, 3, EstadoRound.ABIERTO, false));
        assertEquals("con el round abierto no se envía", 0, finals().size());

        auto.onMessage(new RoundClosed("F1", 3, false, false));

        List<SubmitFinalCard> sent = finals();
        assertEquals(1, sent.size());
        SubmitFinalCard card = sent.get(0);
        assertEquals("F1", card.fightId());
        assertEquals(2, (int) card.seat());
        List<State.OwnCard> own = List.of(new State.OwnCard(1, 10, 9, EstadoTarjeta.ENVIADA),
                new State.OwnCard(2, 10, 9, EstadoTarjeta.ENVIADA), new State.OwnCard(3, 10, 9, EstadoTarjeta.ENVIADA));
        assertEquals(FinalCardHash.of("F1", 2, own), card.hash());
        assertTrue("firmada con la llave del teléfono", DeviceProof.verify(
                DeviceProof.encodePublicKey(keys.getPublic()), DeviceProof.finalCardText("F1", 2, own), card.sig()));
    }

    @Test
    public void unStateTerminadoSinTarjetaFinalLaEnviaAlReconectar() {
        send(1, true);
        send(2, true);
        send(3, true);
        auto.onMessage(state(EstadoPelea.TERMINADA, 3, EstadoRound.CERRADO, false));
        assertEquals(1, finals().size());
    }

    @Test
    public void unRegistroTardioVeElUltimoRoundCerradoEnElStateRepetido() {
        send(1, true);
        send(2, true);
        send(3, true);
        auto.onMessage(state(EstadoPelea.EN_CURSO, 3, EstadoRound.CERRADO, false));
        assertEquals(1, finals().size());
    }

    @Test
    public void noSeMandaDosVeces() {
        send(1, true);
        send(2, true);
        send(3, true);
        auto.onMessage(state(EstadoPelea.EN_CURSO, 3, EstadoRound.ABIERTO, false));
        auto.onMessage(new RoundClosed("F1", 3, false, false));
        auto.onMessage(state(EstadoPelea.TERMINADA, 3, EstadoRound.CERRADO, false));

        AutoFinalCard otroProceso = newAuto();
        otroProceso.onMessage(state(EstadoPelea.TERMINADA, 3, EstadoRound.CERRADO, false));
        assertEquals(1, finals().size());
    }

    @Test
    public void esperaElAckDelUltimoRoundYLuegoEnvia() {
        send(1, true);
        send(2, true);
        SubmitRound tercero = send(3, false);
        auto.onMessage(state(EstadoPelea.EN_CURSO, 3, EstadoRound.ABIERTO, false));
        auto.onMessage(new RoundClosed("F1", 3, false, false));
        assertEquals("el round 3 sigue sin ACK", 0, finals().size());

        Ack ack = new Ack(tercero.id(), true, EstadoTarjeta.ENVIADA, null);
        outbox.onAck(ack);
        auto.onMessage(ack);
        assertEquals(1, finals().size());
    }

    @Test
    public void unaTerminacionAnticipadaNoMandaTarjeta() {
        send(1, true);
        auto.onMessage(state(EstadoPelea.TERMINADA, 2, EstadoRound.CERRADO, false));
        assertEquals(0, finals().size());
    }

    @Test
    public void unaTarjetaRechazadaNoSeReintentaSola() {
        send(1, true);
        send(2, true);
        send(3, true);
        auto.onMessage(state(EstadoPelea.TERMINADA, 3, EstadoRound.CERRADO, false));
        SubmitFinalCard card = finals().get(0);
        outbox.onAck(new Ack(card.id(), false, null, Ack.Error.BAD_SIGNATURE));

        AutoFinalCard otroProceso = newAuto();
        otroProceso.onMessage(state(EstadoPelea.TERMINADA, 3, EstadoRound.CERRADO, false));
        assertEquals(0, finals().size());
    }

    @Test
    public void siLaPeleaAunNoTerminabaLaReenviaConFightEndedSinBucle() {
        send(1, true);
        send(2, true);
        send(3, true);
        auto.onMessage(state(EstadoPelea.EN_CURSO, 3, EstadoRound.ABIERTO, false));
        auto.onMessage(new RoundClosed("F1", 3, false, false));
        SubmitFinalCard first = finals().get(0);
        Ack rejected = new Ack(first.id(), false, null, Ack.Error.FIGHT_NOT_ACTIVE);
        outbox.onAck(rejected);
        auto.onMessage(rejected);
        assertEquals("su propio rechazo no la vuelve a mandar", 0, finals().size());

        auto.onMessage(new FightEnded("F1", null));
        List<SubmitFinalCard> again = finals();
        assertEquals(1, again.size());
        assertNotEquals(first.id(), again.get(0).id());
        auto.onMessage(state(EstadoPelea.TERMINADA, 3, EstadoRound.CERRADO, false));
        assertEquals("ya va en camino: no otra", 1, finals().size());
    }

    @Test
    public void unStateDeOtroLugarNoHaceFirmarNada() {
        send(1, true);
        send(2, true);
        send(3, true);
        auto.onMessage(new State("F1", 3, "María López", "Roja", "Azul", 3, EstadoPelea.TERMINADA, 3,
                EstadoRound.CERRADO, List.of(), false));
        auto.onMessage(new State("F9", 2, "María López", "Roja", "Azul", 3, EstadoPelea.TERMINADA, 3,
                EstadoRound.CERRADO, List.of(), false));
        assertEquals(0, finals().size());
    }

    @Test
    public void conLaTarjetaYaRecibidaNoSeManda() {
        send(1, true);
        send(2, true);
        send(3, true);
        auto.onMessage(state(EstadoPelea.TERMINADA, 3, EstadoRound.CERRADO, true));
        assertEquals(0, finals().size());
    }
}
