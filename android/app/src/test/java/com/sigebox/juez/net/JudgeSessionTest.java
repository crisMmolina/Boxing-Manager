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
import com.sigebox.core.protocol.Join;
import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.JoinPending;
import com.sigebox.core.protocol.JoinRejected;
import com.sigebox.core.protocol.JoinUri;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.Ping;
import com.sigebox.core.protocol.Pong;
import com.sigebox.core.protocol.RoundClosed;
import com.sigebox.core.protocol.RoundOpened;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.juez.data.Outbox;
import com.sigebox.juez.data.SessionStore;
import com.sigebox.juez.data.SigeboxDatabase;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class JudgeSessionTest {

    private SigeboxDatabase db;
    private SessionStore store;
    private Outbox outbox;
    private TestSigner signer;
    private long now = 10_000L;
    private final FakeTransport transport = new FakeTransport();
    private final List<Message> delivered = new ArrayList<>();
    private JudgeSession session;

    @Before
    public void setUp() throws Exception {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SigeboxDatabase.class)
                .allowMainThreadQueries()
                .build();
        store = new SessionStore(db.sessionDao());
        outbox = new Outbox(db.outboxDao(), () -> now);
        signer = new TestSigner();
        session = new JudgeSession(outbox, store, signer, () -> now, new JudgeListener() {
            @Override
            public void onMessage(Message message) {
                delivered.add(message);
            }
        });
    }

    @After
    public void tearDown() {
        db.close();
    }

    private String nonce() {
        return DeviceProof.newNonce(new SecureRandom());
    }

    private void approvedSession() {
        store.saveJoin("192.168.43.10", 8765, "F1", 2, "tok123", "María López");
        store.markApproved();
    }

    private State state(int round, EstadoRound status) {
        return new State("F1", 2, "María López", "Roja", "Azul", 4, EstadoPelea.EN_CURSO, round, status,
                List.of(), false);
    }

    private void handshake() {
        session.onOpen(transport);
        session.onText(Messages.encode(new Challenge(nonce())));
        session.onText(Messages.encode(state(3, EstadoRound.ABIERTO)));
    }

    @Test
    public void conSesionAprobadaRespondeAlChallengeConHelloFirmado() {
        approvedSession();
        session.onOpen(transport);
        assertEquals(ConnectionState.Status.CONNECTING, session.state().status);
        String nonce = nonce();
        session.onText(Messages.encode(new Challenge(nonce)));

        Hello hello = (Hello) transport.last();
        assertEquals("F1", hello.fightId());
        assertEquals(Integer.valueOf(2), hello.seat());
        assertEquals(nonce, hello.nonce());
        assertTrue(DeviceProof.verify(signer.publicKey(),
                DeviceProof.helloText("F1", 2, signer.deviceId(), nonce), hello.sig()));
    }

    @Test
    public void sinSesionNoMandaNadaHastaQueElJuezSeUne() {
        session.onOpen(transport);
        String nonce = nonce();
        session.onText(Messages.encode(new Challenge(nonce)));
        assertTrue(transport.sent.isEmpty());

        JoinUri uri = JoinUri.parse("sigebox://join?h=192.168.43.10&p=8765&s=F1&seat=2&t=tok123");
        session.join(uri, "María López");

        Join join = (Join) transport.last();
        assertEquals("María López", join.judgeName());
        assertEquals("tok123", join.token());
        assertEquals(signer.publicKey(), join.publicKey());
        assertTrue(DeviceProof.verify(signer.publicKey(),
                DeviceProof.joinText("F1", 2, "tok123", signer.deviceId(), nonce), join.sig()));
    }

    @Test
    public void joinPendienteYLuegoAprobado() {
        session.onOpen(transport);
        session.onText(Messages.encode(new Challenge(nonce())));
        session.join(JoinUri.parse("sigebox://join?h=10.0.0.2&p=8765&s=F1&seat=2&t=tok123"), "María López");

        session.onText(Messages.encode(new JoinPending("F1", 2)));
        assertEquals(ConnectionState.Status.WAITING_APPROVAL, session.state().status);

        session.onText(Messages.encode(new JoinApproved("F1", 2, "María López", "Roja", "Azul", 4)));
        assertEquals(ConnectionState.Status.CONNECTED, session.state().status);
        assertTrue(store.load().approved);
    }

    @Test
    public void joinRechazadoGuardaElMotivoYBorraLaSesion() {
        session.onOpen(transport);
        session.onText(Messages.encode(new Challenge(nonce())));
        session.join(JoinUri.parse("sigebox://join?h=10.0.0.2&p=8765&s=F1&seat=2&t=tok123"), "María López");

        session.onText(Messages.encode(new JoinRejected("F1", 2, JoinRejected.Reason.TOKEN_USED)));
        assertEquals(ConnectionState.Status.REJECTED, session.state().status);
        assertEquals(JoinRejected.Reason.TOKEN_USED, session.state().rejectReason);
        assertNull(store.load());
    }

    @Test
    public void lasCalificacionesSinConexionSeMandanEnOrdenAlRecibirState() {
        approvedSession();
        SubmitRound r1 = new SubmitRound("F1", 2, 1, 10, 9);
        SubmitRound r2 = new SubmitRound("F1", 2, 2, 9, 10);
        session.submit(r1);
        session.submit(r2);
        assertEquals(2, session.state().pendingAcks);

        handshake();

        assertEquals(ConnectionState.Status.CONNECTED, session.state().status);
        assertEquals(Integer.valueOf(3), session.state().currentRound);
        assertEquals(EstadoRound.ABIERTO, session.state().roundStatus);
        List<Message> sent = transport.messages();
        assertEquals(Hello.TYPE, sent.get(0).type());
        assertEquals(r1.id(), sent.get(1).id());
        assertEquals(r2.id(), sent.get(2).id());
    }

    @Test
    public void elAckQuitaLaCalificacionDePendientes() {
        approvedSession();
        handshake();
        SubmitRound submit = new SubmitRound("F1", 2, 3, 10, 9);
        session.submit(submit);
        assertEquals(submit.id(), transport.last().id());
        assertEquals(1, session.state().pendingAcks);

        session.onText(Messages.encode(new Ack(submit.id(), true, EstadoTarjeta.ENVIADA, null)));
        assertEquals(0, session.state().pendingAcks);
    }

    @Test
    public void respondePongAlPingDelServidor() {
        approvedSession();
        handshake();
        session.onText(Messages.encode(new Ping(123L)));
        Message last = transport.last();
        assertEquals(Pong.TYPE, last.type());
        assertEquals(Long.valueOf(123L), ((Pong) last).ts());
    }

    @Test
    public void roundsAbiertosYCerradosActualizanElEstadoYSeEntreganALaUi() {
        approvedSession();
        handshake();
        session.onText(Messages.encode(new RoundClosed("F1", 3, true, true)));
        assertEquals(EstadoRound.CERRADO, session.state().roundStatus);
        session.onText(Messages.encode(new RoundOpened("F1", 4)));
        assertEquals(Integer.valueOf(4), session.state().currentRound);
        assertEquals(EstadoRound.ABIERTO, session.state().roundStatus);
        assertEquals(RoundClosed.TYPE, delivered.get(delivered.size() - 2).type());
        assertEquals(RoundOpened.TYPE, delivered.get(delivered.size() - 1).type());
    }

    @Test
    public void tickMandaPingReenviaSinAckYDetectaCaida() {
        approvedSession();
        handshake();
        SubmitRound submit = new SubmitRound("F1", 2, 3, 10, 9);
        session.submit(submit);
        int before = transport.sent.size();

        now += JudgeSession.RESEND_AFTER_MS + 1;
        assertTrue(session.tick());
        List<Message> sent = transport.messages().subList(before, transport.sent.size());
        assertTrue(sent.stream().anyMatch(m -> m.type().equals(Ping.TYPE)));
        assertTrue("reenvía lo que no tiene ACK", sent.stream().anyMatch(m -> m.id().equals(submit.id())));

        now += JudgeSession.DEAD_AFTER_MS + 1;
        assertFalse("sin mensajes del servidor por mucho tiempo = conexión caída", session.tick());
    }

    @Test
    public void unMensajeMalformadoSeIgnora() {
        approvedSession();
        handshake();
        session.onText("{\"type\":\"SUBMIT_ROUND\",\"red\":\"10\"}");
        session.onText("no es json");
        assertEquals(ConnectionState.Status.CONNECTED, session.state().status);
    }

    @Test
    public void alCerrarseQuedaDesconectadoYGuardaLoQueSeCalifique() {
        approvedSession();
        handshake();
        session.onClosed();
        assertEquals(ConnectionState.Status.DISCONNECTED, session.state().status);
        int before = transport.sent.size();
        session.submit(new SubmitRound("F1", 2, 3, 10, 9));
        assertEquals(before, transport.sent.size());
        assertEquals(1, session.state().pendingAcks);
    }

    static final class FakeTransport implements Transport {
        final List<String> sent = new ArrayList<>();

        @Override
        public boolean send(String text) {
            sent.add(text);
            return true;
        }

        @Override
        public void close() {}

        List<Message> messages() {
            List<Message> out = new ArrayList<>();
            for (String s : sent) {
                out.add(Messages.decode(s));
            }
            return out;
        }

        Message last() {
            return Messages.decode(sent.get(sent.size() - 1));
        }
    }

    static final class TestSigner implements DeviceSigner {
        private final KeyPair keys;

        TestSigner() throws Exception {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            keys = gen.generateKeyPair();
        }

        @Override
        public String deviceId() {
            return DeviceProof.fingerprint(publicKey());
        }

        @Override
        public String publicKey() {
            return DeviceProof.encodePublicKey(keys.getPublic());
        }

        @Override
        public String sign(String text) {
            return DeviceProof.sign(keys.getPrivate(), text);
        }
    }
}
