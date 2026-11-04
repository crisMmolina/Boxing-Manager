package com.sigebox.juez.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Challenge;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.Hello;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.NextFight;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.juez.data.Outbox;
import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.data.SessionStore;
import com.sigebox.juez.data.SigeboxDatabase;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** AND-013: {@code NEXT_FIGHT} lleva al juez aprobado a la siguiente pelea sin escanear ni confirmar. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class JudgeSessionNextFightTest {

    private SigeboxDatabase db;
    private SessionStore store;
    private Outbox outbox;
    private JudgeSessionTest.TestSigner signer;
    private long now = 10_000L;
    private final CloseTrackingTransport transport = new CloseTrackingTransport();
    private final List<Message> delivered = new ArrayList<>();
    private JudgeSession session;

    @Before
    public void setUp() throws Exception {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SigeboxDatabase.class)
                .allowMainThreadQueries().build();
        store = new SessionStore(db.sessionDao());
        outbox = new Outbox(db.outboxDao(), () -> now);
        signer = new JudgeSessionTest.TestSigner();
        session = new JudgeSession(outbox, store, signer, () -> now, new JudgeListener() {
            @Override
            public void onMessage(Message message) {
                delivered.add(message);
            }
        });
        store.saveJoin("192.168.43.10", 8765, "F1", 2, "tok123", "María López");
        store.markApproved();
    }

    @After
    public void tearDown() {
        db.close();
    }

    private static String nonce() {
        return DeviceProof.newNonce(new SecureRandom());
    }

    private void sync() {
        session.onOpen(transport);
        session.onText(Messages.encode(new Challenge(nonce())));
        session.onText(Messages.encode(new State("F1", 2, "María López", "Roja", "Azul", 4, EstadoPelea.EN_CURSO, 3,
                EstadoRound.ABIERTO, List.of(), false)));
    }

    private static NextFight next(Integer seat) {
        return new NextFight("F1", "F2", seat, "Rojo 2", "Azul 2", 6);
    }

    @Test
    public void conLugarGuardaLaPeleaNuevaSigueAprobadoYCierraElSocket() {
        sync();
        session.onText(Messages.encode(next(3)));

        SessionEntity saved = store.load();
        assertEquals("F2", saved.fightId);
        assertEquals(3, saved.seat);
        assertTrue(saved.approved);
        assertEquals("tok123", saved.token);
        assertTrue(transport.closed);
        assertTrue(delivered.get(delivered.size() - 1) instanceof NextFight);
    }

    @Test
    public void elSiguienteHelloLlevaNextFightIdYElMismoDeviceId() {
        sync();
        String deviceId = signer.deviceId();
        session.onText(Messages.encode(next(3)));
        session.onClosed();

        CloseTrackingTransport second = new CloseTrackingTransport();
        session.onOpen(second);
        String nonce = nonce();
        session.onText(Messages.encode(new Challenge(nonce)));

        Hello hello = (Hello) second.last();
        assertEquals("F2", hello.fightId());
        assertEquals(Integer.valueOf(3), hello.seat());
        assertEquals(deviceId, hello.deviceId());
        assertTrue(DeviceProof.verify(signer.publicKey(),
                DeviceProof.helloText("F2", 3, deviceId, nonce), hello.sig()));
    }

    @Test
    public void sinLugarNoCambiaDePeleaNiCierraYAvisaALaPantalla() {
        sync();
        session.onText(Messages.encode(next(null)));

        SessionEntity saved = store.load();
        assertEquals("F1", saved.fightId);
        assertTrue(saved.approved);
        assertFalse(transport.closed);
        NextFight seen = (NextFight) delivered.get(delivered.size() - 1);
        assertNull(seen.seat());
    }

    @Test
    public void unNextFightRepetidoDeLaMismaPeleaNoReconectaDeNuevo() {
        sync();
        session.onText(Messages.encode(next(3)));
        session.onClosed();
        session.onOpen(transport);
        transport.closed = false;
        session.onText(Messages.encode(new Challenge(nonce())));
        session.onText(Messages.encode(next(3)));

        assertEquals("F2", store.load().fightId);
        assertFalse(transport.closed);
    }

    @Test
    public void loQueQuedabaEnLaBandejaSeEntregaALaPeleaAnteriorAntesDeCambiar() {
        sync();
        SubmitRound pending = new SubmitRound("F1", 2, 3, 10, 9);
        session.submit(pending);
        assertEquals(1, session.state().pendingAcks);

        session.onText(Messages.encode(next(3)));
        // Todavía hay algo sin ACK: sigue en F1, el socket abierto y la pantalla sin aviso.
        assertEquals("F1", store.load().fightId);
        assertFalse(transport.closed);
        assertFalse(delivered.stream().anyMatch(m -> m instanceof NextFight));

        session.onText(Messages.encode(new Ack(pending.id(), true, EstadoTarjeta.ENVIADA, null)));
        assertEquals("F2", store.load().fightId);
        assertTrue(transport.closed);
        assertTrue(delivered.stream().anyMatch(m -> m instanceof NextFight));
        assertEquals(0, outbox.pendingCount());
    }

    @Test
    public void siLaCentralNoConfirmaLaBandejaElCambioNoSeQuedaTrabado() {
        sync();
        session.submit(new SubmitRound("F1", 2, 3, 10, 9));
        session.onText(Messages.encode(next(3)));
        assertEquals("F1", store.load().fightId);

        now += JudgeSession.SWITCH_WAIT_MS + 1;
        session.tick();

        assertEquals("F2", store.load().fightId);
        assertTrue(transport.closed);
        assertEquals("el mensaje sigue guardado, no se pierde", 1, outbox.pendingCount());
    }

    @Test
    public void unHelloDePeleaCerradaRespondidoConNextFightCambiaAunSinSincronizar() {
        session.onOpen(transport);
        session.onText(Messages.encode(new Challenge(nonce())));
        session.onText(Messages.encode(next(2)));

        assertEquals("F2", store.load().fightId);
        assertTrue(transport.closed);
    }

    static final class CloseTrackingTransport implements Transport {
        final List<String> sent = new ArrayList<>();
        boolean closed;

        @Override
        public boolean send(String text) {
            sent.add(text);
            return true;
        }

        @Override
        public void close() {
            closed = true;
        }

        Message last() {
            return Messages.decode(sent.get(sent.size() - 1));
        }
    }
}
