package com.sigebox.central.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.db.TarjetaFinalRepository;
import com.sigebox.central.db.TarjetaRoundRepository;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaRound;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Challenge;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.FightEnded;
import com.sigebox.core.protocol.FinalCardHash;
import com.sigebox.core.protocol.Hello;
import com.sigebox.core.protocol.Join;
import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.JoinPending;
import com.sigebox.core.protocol.JoinRejected;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.Ping;
import com.sigebox.core.protocol.Pong;
import com.sigebox.core.protocol.RoundClosed;
import com.sigebox.core.protocol.RoundOpened;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitFinalCard;
import com.sigebox.core.protocol.SubmitRound;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JudgeHubTest {

    private static final String F = "pelea-1";
    private static final AtomicInteger IDS = new AtomicInteger();

    @TempDir
    Path dir;

    private Database db;
    private SeatTokens tokens;
    private JudgeHub hub;
    private final List<String> eventsSeen = new ArrayList<>();

    /** Conexión falsa: guarda lo enviado y el cierre. */
    static final class FakeConn implements Conn {
        final String id = "c" + IDS.incrementAndGet();
        final List<Message> sent = new ArrayList<>();
        Integer closedWith;

        @Override
        public String id() {
            return id;
        }

        @Override
        public void send(String text) {
            sent.add(Messages.decode(text));
        }

        @Override
        public void close(int code, String reason) {
            if (closedWith == null) {
                closedWith = code;
            }
        }

        Message last() {
            return sent.get(sent.size() - 1);
        }

        String nonce() {
            return ((Challenge) sent.get(0)).nonce();
        }
    }

    /** Un teléfono de juez con su par de llaves EC P-256. */
    static final class Phone {
        final KeyPair keys;
        final String deviceId;

        Phone(String deviceId) throws Exception {
            var gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            this.keys = gen.generateKeyPair();
            this.deviceId = deviceId;
        }

        String publicKey() {
            return DeviceProof.encodePublicKey(keys.getPublic());
        }

        Join join(String fightId, int seat, String token, String nonce) {
            var sig = DeviceProof.sign(keys.getPrivate(), DeviceProof.joinText(fightId, seat, token, deviceId, nonce));
            return new Join(fightId, seat, token, "Juez " + seat, deviceId, publicKey(), nonce, sig);
        }

        Hello hello(String fightId, int seat, String nonce) {
            var sig = DeviceProof.sign(keys.getPrivate(), DeviceProof.helloText(fightId, seat, deviceId, nonce));
            return new Hello(fightId, seat, deviceId, nonce, sig);
        }
    }

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        new PeleaRepository(db).save(new Pelea(F, "Rojo", "Azul", 3, 2, EstadoPelea.EN_CURSO, "2026-10-25"));
        tokens = new SeatTokens();
        hub = new JudgeHub(db, tokens, new ServerEvents() {
            @Override
            public void onJoinRequested(JoinRequest r) {
                eventsSeen.add("join " + r.seat());
            }

            @Override
            public void onCardReceived(TarjetaRound card) {
                eventsSeen.add("card " + card.juez() + "/" + card.round());
            }
        }, Duration.ofSeconds(30), () -> 1_000L);
    }

    @AfterEach
    void tearDown() {
        hub.close();
        db.close();
    }

    private FakeConn open() {
        var c = new FakeConn();
        hub.onOpen(c);
        return c;
    }

    private void send(FakeConn c, Message m) {
        hub.onMessage(c.id(), Messages.encode(m));
    }

    /** Une y aprueba al juez {@code seat}; devuelve su conexión. */
    private FakeConn approvedJudge(Phone phone, int seat) {
        var c = open();
        send(c, phone.join(F, seat, tokens.issue(F, seat), c.nonce()));
        assertInstanceOf(JoinPending.class, c.last());
        assertTrue(hub.approve(F, seat));
        assertInstanceOf(JoinApproved.class, c.last());
        return c;
    }

    private void setRound(int n, EstadoRound estado) {
        new RoundRepository(db).save(new Round(F, n, estado, 1L, estado == EstadoRound.CERRADO ? 2L : null, false));
    }

    private static SubmitRound submit(String id, int seat, int round, int red, int blue) {
        return new SubmitRound(1, id, SubmitRound.TYPE, F, seat, round, red, blue);
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    @Test
    void sendsChallengeFirst() {
        var c = open();
        assertInstanceOf(Challenge.class, c.sent.get(0));
    }

    @Test
    void joinWithTokenIsPendingThenApproved() throws Exception {
        var phone = new Phone("A37-1");
        var c = open();
        send(c, phone.join(F, 1, tokens.issue(F, 1), c.nonce()));

        assertInstanceOf(JoinPending.class, c.last());
        var lugar = new LugarJuezRepository(db).find(F, 1).orElseThrow();
        assertEquals(EstadoConexion.PENDIENTE, lugar.conexion());
        assertEquals(DeviceProof.fingerprint(phone.publicKey()), lugar.huellaCredencial());
        assertEquals(phone.publicKey(), new LugarJuezRepository(db).publicKey(F, 1).orElseThrow());
        assertEquals(List.of("join 1"), eventsSeen);

        assertTrue(hub.approve(F, 1));
        var approved = assertInstanceOf(JoinApproved.class, c.last());
        assertEquals("Rojo", approved.redCorner());
        assertEquals(EstadoConexion.CONECTADO, new LugarJuezRepository(db).find(F, 1).orElseThrow().conexion());
        assertFalse(hub.approve(F, 1)); // ya aprobado
    }

    @Test
    void rejectClosesThePendingJudge() throws Exception {
        var c = open();
        send(c, new Phone("A37-1").join(F, 1, tokens.issue(F, 1), c.nonce()));
        assertTrue(hub.reject(F, 1));
        assertEquals(JoinRejected.Reason.REJECTED, ((JoinRejected) c.last()).reason());
        assertEquals(1000, c.closedWith);
    }

    @Test
    void joinBranches() throws Exception {
        var phone = new Phone("A37-1");
        var token = tokens.issue(F, 1);
        var first = open();
        send(first, phone.join(F, 1, token, first.nonce()));

        // Mismo teléfono reenvía el JOIN tras reconectar, aún pendiente: sin consultar el token.
        var again = open();
        send(again, phone.join(F, 1, token, again.nonce()));
        assertInstanceOf(JoinPending.class, again.last());
        assertEquals(1000, first.closedWith); // la conexión vieja se reemplaza

        // Otro teléfono con el mismo token: el lugar ya tiene llave.
        var thief = open();
        send(thief, new Phone("otro").join(F, 1, token, thief.nonce()));
        assertEquals(JoinRejected.Reason.SEAT_TAKEN, ((JoinRejected) thief.last()).reason());
        assertEquals(Conn.POLICY, thief.closedWith);
        assertEquals(phone.publicKey(), new LugarJuezRepository(db).publicKey(F, 1).orElseThrow());

        // Aprobado y perdió el JOIN_APPROVED: reenvía y lo recibe.
        assertTrue(hub.approve(F, 1));
        var third = open();
        send(third, phone.join(F, 1, token, third.nonce()));
        assertInstanceOf(JoinApproved.class, third.last());

        // Token equivocado en otro lugar.
        var wrong = open();
        tokens.issue(F, 2);
        send(wrong, new Phone("B").join(F, 2, "x".repeat(22), wrong.nonce()));
        assertEquals(JoinRejected.Reason.TOKEN_INVALID, ((JoinRejected) wrong.last()).reason());
    }

    @Test
    void badSignatureOrReplayedNonceCloses() throws Exception {
        var phone = new Phone("A37-1");
        var c = open();
        var join = phone.join(F, 1, tokens.issue(F, 1), c.nonce());
        var forged = new Join(F, 1, join.token(), join.judgeName(), join.deviceId(), new Phone("x").publicKey(),
                join.nonce(), join.sig());
        send(c, forged);
        assertEquals(JoinRejected.Reason.BAD_SIGNATURE, ((JoinRejected) c.last()).reason());
        assertEquals(Conn.POLICY, c.closedWith);

        var other = open();
        send(other, phone.join(F, 1, "y".repeat(22), c.nonce())); // nonce de otra conexión
        assertEquals(Conn.POLICY, other.closedWith);
    }

    @Test
    void anythingBeforeAuthenticationCloses() {
        var c = open();
        send(c, new Ping(5L));
        assertEquals(Conn.POLICY, c.closedWith);

        var garbage = open();
        hub.onMessage(garbage.id(), "{no es json");
        assertEquals(Conn.POLICY, garbage.closedWith);
    }

    @Test
    void unauthenticatedConnectionTimesOut() throws Exception {
        hub.close();
        hub = new JudgeHub(db, tokens, ServerEvents.NONE, Duration.ofMillis(50), () -> 1L);
        var c = open();
        for (int i = 0; i < 100 && c.closedWith == null; i++) {
            Thread.sleep(20);
        }
        assertEquals(Conn.POLICY, c.closedWith);
    }

    @Test
    void pendingJudgeGetsPongButCannotSubmit() throws Exception {
        var c = open();
        send(c, new Phone("A37-1").join(F, 1, tokens.issue(F, 1), c.nonce()));
        setRound(1, EstadoRound.ABIERTO);

        send(c, new Ping(42L));
        assertEquals(42L, ((Pong) c.last()).ts());
        var id = newId();
        send(c, submit(id, 1, 1, 10, 9));
        assertEquals(Ack.Error.NOT_APPROVED, ((Ack) c.last()).error());
        assertNull(c.closedWith);
    }

    @Test
    void duplicateSubmitStoresOnceAndAcksTwice() throws Exception {
        var c = approvedJudge(new Phone("A37-1"), 1);
        setRound(1, EstadoRound.ABIERTO);
        var id = newId();

        send(c, submit(id, 1, 1, 10, 9));
        var first = (Ack) c.last();
        // Se perdió el ACK y el round se cerró antes del reenvío: mismo ACK, no se vuelve TARDIA.
        setRound(1, EstadoRound.CERRADO);
        send(c, submit(id, 1, 1, 10, 9));
        var second = (Ack) c.last();

        assertEquals(id, first.ackId());
        assertEquals(id, second.ackId());
        assertTrue(first.ok());
        assertTrue(second.ok());
        assertEquals(EstadoTarjeta.ENVIADA, first.status());
        assertEquals(EstadoTarjeta.ENVIADA, second.status());
        var cards = new TarjetaRoundRepository(db).byPelea(F);
        assertEquals(1, cards.size());
        assertEquals(EstadoTarjeta.ENVIADA, cards.get(0).estado());
        assertEquals(List.of("card 1/1"), eventsSeen.stream().filter(e -> e.startsWith("card")).toList());
    }

    @Test
    void seatAndFightComeFromTheSessionNotTheMessage() throws Exception {
        var c = approvedJudge(new Phone("A37-1"), 1);
        setRound(1, EstadoRound.ABIERTO);

        send(c, submit(newId(), 2, 1, 10, 9)); // dice ser el lugar 2
        assertEquals(Ack.Error.WRONG_SESSION, ((Ack) c.last()).error());
        send(c, new SubmitRound(1, newId(), SubmitRound.TYPE, "otra-pelea", 1, 1, 10, 9));
        assertEquals(Ack.Error.WRONG_SESSION, ((Ack) c.last()).error());
        assertTrue(new TarjetaRoundRepository(db).byPelea(F).isEmpty());
    }

    @Test
    void roundStateDecidesSentLateOrRejected() throws Exception {
        var c = approvedJudge(new Phone("A37-1"), 1);

        send(c, submit(newId(), 1, 1, 10, 9)); // round 1 sin abrir
        assertEquals(Ack.Error.ROUND_NOT_OPEN, ((Ack) c.last()).error());

        setRound(1, EstadoRound.CERRADO); // cerrado sin tarjeta: tardía
        send(c, submit(newId(), 1, 1, 10, 9));
        assertEquals(EstadoTarjeta.TARDIA, ((Ack) c.last()).status());

        send(c, submit(newId(), 1, 1, 9, 10)); // cerrado y ya calificado
        assertEquals(Ack.Error.ROUND_NOT_OPEN, ((Ack) c.last()).error());

        // Un 11 manipulado no pasa Messages.decode: no se guarda ni se confirma, y la sesión sigue.
        setRound(2, EstadoRound.ABIERTO);
        int before = c.sent.size();
        var tampered = Messages.encode(submit(newId(), 1, 2, 10, 9)).replace("\"red\":10", "\"red\":11");
        hub.onMessage(c.id(), tampered);
        assertEquals(before, c.sent.size());
        assertNull(c.closedWith);
        assertTrue(new TarjetaRoundRepository(db).find(F, 1, 2).isEmpty());
    }

    @Test
    void helloGetsStateWithOnlyOwnCards() throws Exception {
        var phone = new Phone("A37-1");
        approvedJudge(phone, 1);
        var cards = new TarjetaRoundRepository(db);
        cards.save(new TarjetaRound(F, 1, 1, 10, 9, EstadoTarjeta.ENVIADA, OrigenTarjeta.JUEZ, 1L));
        cards.save(new TarjetaRound(F, 2, 1, 9, 10, EstadoTarjeta.ENVIADA, OrigenTarjeta.JUEZ, 1L));
        setRound(1, EstadoRound.CERRADO);
        setRound(2, EstadoRound.ABIERTO);

        var c = open();
        send(c, phone.hello(F, 1, c.nonce()));
        var state = assertInstanceOf(State.class, c.last());
        assertEquals(1, state.seat());
        assertEquals(2, state.currentRound());
        assertEquals(List.of(new State.OwnCard(1, 10, 9, EstadoTarjeta.ENVIADA)), state.cards());
        assertFalse(state.finalCardSent());
        assertFalse(Messages.encode(state).contains("total"));
    }

    @Test
    void helloWithWrongKeyOrUnapprovedSeatCloses() throws Exception {
        var phone = new Phone("A37-1");
        approvedJudge(phone, 1);
        var c = open();
        send(c, new Phone("A37-1").hello(F, 1, c.nonce())); // mismo deviceId, otra llave
        assertEquals(Conn.POLICY, c.closedWith);

        var pending = open();
        send(pending, new Phone("B").join(F, 2, tokens.issue(F, 2), pending.nonce()));
        var c2 = open();
        send(c2, new Phone("B").hello(F, 2, c2.nonce()));
        assertEquals(Conn.POLICY, c2.closedWith);
    }

    @Test
    void broadcastsReachApprovedJudgesWithMustSubmit() throws Exception {
        var j1 = approvedJudge(new Phone("A"), 1);
        var j2 = approvedJudge(new Phone("B"), 2);
        setRound(1, EstadoRound.ABIERTO);
        hub.roundOpened(F, 1);
        assertEquals(1, ((RoundOpened) j1.last()).round());
        send(j1, submit(newId(), 1, 1, 10, 9));

        setRound(1, EstadoRound.CERRADO);
        hub.roundClosed(F, 1, true);
        assertFalse(((RoundClosed) j1.last()).mustSubmit());
        assertTrue(((RoundClosed) j2.last()).mustSubmit());
        assertTrue(((RoundClosed) j2.last()).forced());

        hub.fightEnded(F, MotivoTerminacion.KO);
        assertEquals(MotivoTerminacion.KO, ((FightEnded) j2.last()).reason());
        assertEquals(List.of(1, 2), hub.connectedSeats(F));
    }

    @Test
    void finalCardIsVerifiedAgainstStoredCards() throws Exception {
        var phone = new Phone("A");
        var c = approvedJudge(phone, 1);
        setRound(1, EstadoRound.ABIERTO);
        send(c, submit(newId(), 1, 1, 10, 9));
        new PeleaRepository(db).updateEstado(F, EstadoPelea.TERMINADA);

        var own = List.of(new State.OwnCard(1, 10, 9, EstadoTarjeta.ENVIADA));
        var hash = FinalCardHash.of(F, 1, own);
        var sig = DeviceProof.sign(phone.keys.getPrivate(), DeviceProof.finalCardText(F, 1, own));

        var lying = List.of(new State.OwnCard(1, 9, 10, EstadoTarjeta.ENVIADA));
        var badSig = DeviceProof.sign(phone.keys.getPrivate(), DeviceProof.finalCardText(F, 1, lying));
        send(c, new SubmitFinalCard(F, 1, FinalCardHash.of(F, 1, lying), badSig));
        assertEquals(Ack.Error.BAD_SIGNATURE, ((Ack) c.last()).error());
        assertTrue(new TarjetaFinalRepository(db).byPelea(F).isEmpty());

        var id = newId();
        send(c, new SubmitFinalCard(1, id, SubmitFinalCard.TYPE, F, 1, hash, sig));
        assertTrue(((Ack) c.last()).ok());
        send(c, new SubmitFinalCard(1, id, SubmitFinalCard.TYPE, F, 1, hash, sig)); // mismo id
        assertTrue(((Ack) c.last()).ok());
        send(c, new SubmitFinalCard(F, 1, hash, sig)); // otro id
        assertEquals(Ack.Error.ALREADY_FINAL, ((Ack) c.last()).error());

        assertEquals(hash, new TarjetaFinalRepository(db).byPelea(F).get(0).huella());
        assertEquals(sig, new TarjetaFinalRepository(db).signature(F, 1).orElseThrow());
    }

    @Test
    void controlMessagesFromJudgesAreRejected() throws Exception {
        var c = approvedJudge(new Phone("A"), 1);
        var fake = new RoundOpened(F, 1);
        send(c, fake);
        var ack = (Ack) c.last();
        assertEquals(fake.id(), ack.ackId());
        assertEquals(Ack.Error.MALFORMED, ack.error());
        assertTrue(new RoundRepository(db).byPelea(F).isEmpty());
    }

    @Test
    void oversizedMessageCloses() throws Exception {
        var c = approvedJudge(new Phone("A"), 1);
        hub.onMessage(c.id(), "x".repeat(JudgeHub.MAX_MESSAGE_CHARS + 1));
        assertEquals(Conn.TOO_BIG, c.closedWith);
    }

    @Test
    void disconnectMarksSeatAndReconnectRestores() throws Exception {
        var phone = new Phone("A");
        var c = approvedJudge(phone, 1);
        hub.onClose(c.id());
        assertEquals(EstadoConexion.DESCONECTADO, new LugarJuezRepository(db).find(F, 1).orElseThrow().conexion());

        var back = open();
        send(back, phone.hello(F, 1, back.nonce()));
        assertInstanceOf(State.class, back.last());
        assertEquals(EstadoConexion.CONECTADO, new LugarJuezRepository(db).find(F, 1).orElseThrow().conexion());
    }

    @Test
    void tokenFailuresAreCountedPerSeat() {
        var t = new SeatTokens();
        var token = t.issue(F, 1);
        for (int i = 0; i < SeatTokens.MAX_FAILURES; i++) {
            assertEquals(SeatTokens.Check.INVALID, t.consume(F, 1, "malo-" + i));
        }
        assertEquals(SeatTokens.Check.INVALID, t.consume(F, 1, token)); // descartado tras agotar intentos

        var fresh = t.issue(F, 1);
        assertTrue(fresh.length() >= 22);
        assertEquals(SeatTokens.Check.OK, t.consume(F, 1, fresh));
        assertEquals(SeatTokens.Check.USED, t.consume(F, 1, fresh));
    }
}
