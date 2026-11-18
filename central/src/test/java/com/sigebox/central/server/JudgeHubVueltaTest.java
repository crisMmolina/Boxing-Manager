package com.sigebox.central.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.server.JudgeHubTest.FakeConn;
import com.sigebox.central.server.JudgeHubTest.Phone;
import com.sigebox.central.service.FightService;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.protocol.CodeInfo;
import com.sigebox.core.protocol.CodeLookup;
import com.sigebox.core.protocol.CodeUnknown;
import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.JoinPending;
import com.sigebox.core.protocol.JoinRejected;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.State;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** DSK-055 (spec 008 §Volver al lugar): un juez aprobado y desconectado vuelve a su lugar con un código nuevo. */
class JudgeHubVueltaTest {

    private static final String F = "PELEA1";
    private static final String HOST = "192.168.0.2";

    @TempDir
    Path dir;

    private Database db;
    private SeatTokens tokens;
    private JudgeHub hub;
    private FightService fights;
    private final AtomicLong now = new AtomicLong(10_000);
    private Phone phone;

    @BeforeEach
    void setUp() throws Exception {
        db = Database.open(dir.resolve("sigebox.db"));
        new PeleaRepository(db).save(new Pelea(F, "Rojo", "Azul", 3, 3, EstadoPelea.CREADA, "2026-11-17"));
        tokens = new SeatTokens();
        hub = new JudgeHub(db, tokens, ServerEvents.NONE, Duration.ofSeconds(30), now::get);
        fights = new FightService(db, tokens, () -> LocalDate.of(2026, 11, 17));
        phone = new Phone("A-1");
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

    private void lookup(FakeConn c, String code) {
        send(c, new CodeLookup(code));
        now.addAndGet(1_000);
    }

    /** El lugar 1 aprobado para {@link #phone} y sin conexión; devuelve el código con el que se unió. */
    private String approvedAndDisconnected() {
        var code = fights.invite(F, 1, HOST, 8765).code();
        var c = open();
        send(c, phone.join(F, 1, code, c.nonce()));
        assertInstanceOf(JoinPending.class, c.last());
        assertTrue(hub.approve(F, 1));
        hub.onClose(c.id());
        assertFalse(hub.connectedSeats(F).contains(1));
        return code;
    }

    private List<String> log() {
        return db.inTransaction(c -> {
            var out = new ArrayList<String>();
            try (var ps = c.prepareStatement("SELECT type || ' · ' || detail FROM event_log WHERE fight_id = ? ORDER BY id")) {
                ps.setString(1, F);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(rs.getString(1));
                    }
                }
            }
            return out;
        });
    }

    @Test
    void elMismoTelefonoVuelveConElCodigoNuevoSinPedirAprobacion() throws Exception {
        approvedAndDisconnected();
        var code = fights.reinvite(F, 1, HOST, 8765).code();

        var c = open();
        lookup(c, code);
        var info = assertInstanceOf(CodeInfo.class, c.last());
        assertEquals(List.of(F, 1), List.of(info.fightId(), info.seat()));

        send(c, phone.join(F, 1, code, c.nonce()));
        assertInstanceOf(JoinApproved.class, c.last());
        assertTrue(c.sent.stream().anyMatch(State.class::isInstance));
        assertTrue(c.sent.stream().noneMatch(JoinPending.class::isInstance), "sin volver a pedir aprobación");
        assertTrue(hub.connectedSeats(F).contains(1));

        var other = open();
        lookup(other, code);
        assertInstanceOf(CodeUnknown.class, other.last(), "el código ya se usó");
    }

    @Test
    void unCodigoViejoONormalDeUnLugarRegistradoSigueDesconocido() throws Exception {
        var used = approvedAndDisconnected();
        var normal = fights.invite(F, 1, HOST, 8765).code(); // invitación de lugar libre, no de vuelta

        var c = open();
        lookup(c, used);
        assertInstanceOf(CodeUnknown.class, c.last());
        lookup(c, normal);
        assertInstanceOf(CodeUnknown.class, c.last());
    }

    @Test
    void elCodigoNuevoAnteriorDejaDeServirAlPedirOtro() throws Exception {
        approvedAndDisconnected();
        var first = fights.reinvite(F, 1, HOST, 8765).code();
        var second = fights.reinvite(F, 1, HOST, 8765).code();

        var c = open();
        lookup(c, first);
        assertInstanceOf(CodeUnknown.class, c.last());
        lookup(c, second);
        assertInstanceOf(CodeInfo.class, c.last());
    }

    @Test
    void siElTelefonoVuelveSoloConHelloElCodigoNuevoMuere() throws Exception {
        approvedAndDisconnected();
        var code = fights.reinvite(F, 1, HOST, 8765).code();

        var back = open();
        send(back, phone.hello(F, 1, back.nonce()));
        assertTrue(hub.connectedSeats(F).contains(1));

        var other = open();
        lookup(other, code);
        assertInstanceOf(CodeUnknown.class, other.last());
    }

    @Test
    void unLugarConectadoNoSeOfreceConCodigoNuevo() throws Exception {
        approvedAndDisconnected();
        var back = open();
        send(back, phone.hello(F, 1, back.nonce()));
        var code = fights.reinvite(F, 1, HOST, 8765).code();

        var other = open();
        lookup(other, code);
        assertInstanceOf(CodeUnknown.class, other.last());
    }

    @Test
    void otroTelefonoQuedaEnEsperaYAlCambiarloReemplazaLaLlave() throws Exception {
        approvedAndDisconnected();
        var oldKey = new LugarJuezRepository(db).publicKey(F, 1).orElseThrow();
        var code = fights.reinvite(F, 1, HOST, 8765).code();
        var nuevo = new Phone("B-2");

        var c = open();
        lookup(c, code);
        assertInstanceOf(CodeInfo.class, c.last());
        send(c, nuevo.join(F, 1, code, c.nonce()));
        assertInstanceOf(JoinPending.class, c.last());
        assertNull(c.closedWith);
        assertEquals(oldKey, new LugarJuezRepository(db).publicKey(F, 1).orElseThrow(), "sin aprobar no cambia la llave");
        assertTrue(hub.replacementFingerprint(F, 1).isPresent(), "el operador ve la huella nueva");

        assertTrue(hub.approveReplacement(F, 1));
        assertInstanceOf(JoinApproved.class, c.last());
        assertEquals(nuevo.publicKey(), new LugarJuezRepository(db).publicKey(F, 1).orElseThrow());
        assertTrue(hub.replacementFingerprint(F, 1).isEmpty());
        assertTrue(log().stream().anyMatch(l -> l.startsWith("JOIN_APPROVED") && l.contains("teléfono cambiado")),
                log().toString());

        var old = open();
        send(old, phone.hello(F, 1, old.nonce()));
        assertEquals(Conn.POLICY, old.closedWith, "el teléfono anterior ya no entra");
    }

    @Test
    void rechazarElCambioDejaElLugarAlTelefonoOriginal() throws Exception {
        approvedAndDisconnected();
        var code = fights.reinvite(F, 1, HOST, 8765).code();
        var c = open();
        lookup(c, code);
        send(c, new Phone("B-2").join(F, 1, code, c.nonce()));
        assertInstanceOf(JoinPending.class, c.last());

        assertTrue(hub.rejectReplacement(F, 1));
        assertInstanceOf(JoinRejected.class, c.last());
        assertTrue(hub.replacementFingerprint(F, 1).isEmpty());

        var back = open();
        send(back, phone.hello(F, 1, back.nonce()));
        assertTrue(hub.connectedSeats(F).contains(1), "el original sigue entrando");
    }

    @Test
    void otroTelefonoSinCodigoDeVueltaSigueRecibiendoLugarOcupado() throws Exception {
        var used = approvedAndDisconnected();
        var c = open();
        send(c, new Phone("B-2").join(F, 1, used, c.nonce()));
        assertEquals(JoinRejected.Reason.SEAT_TAKEN, assertInstanceOf(JoinRejected.class, c.last()).reason());
    }

    /** Revisión de seguridad: un código de vuelta no se adivina probando JOIN tras JOIN. */
    @Test
    void cincoCodigosEquivocadosDeOtroTelefonoDescartanElCodigoDeVuelta() throws Exception {
        approvedAndDisconnected();
        var code = fights.reinvite(F, 1, HOST, 8765).code();
        var intruso = new Phone("B-2");
        for (int i = 0; i < SeatTokens.MAX_FAILURES; i++) {
            var wrong = String.format("%06d", (Integer.parseInt(code) + 1 + i) % 1_000_000);
            var c = open();
            send(c, intruso.join(F, 1, wrong, c.nonce()));
            assertEquals(JoinRejected.Reason.SEAT_TAKEN, assertInstanceOf(JoinRejected.class, c.last()).reason());
        }

        var c = open();
        lookup(c, code);
        assertInstanceOf(CodeUnknown.class, c.last(), "tras los intentos el código ya no sirve");
        assertTrue(hub.replacementFingerprint(F, 1).isEmpty());
    }

    /** Revisión de seguridad: un candidato cuya conexión se cerró no queda pendiente ni se puede aprobar. */
    @Test
    void unCandidatoQueSeCaeNoQuedaPendiente() throws Exception {
        approvedAndDisconnected();
        var code = fights.reinvite(F, 1, HOST, 8765).code();
        var nuevo = new Phone("B-2");
        var c = open();
        send(c, nuevo.join(F, 1, code, c.nonce()));
        assertInstanceOf(JoinPending.class, c.last());

        send(c, nuevo.join(F, 1, code, c.nonce())); // segundo JOIN en la misma conexión: se cierra
        assertEquals(Conn.POLICY, c.closedWith);
        assertTrue(hub.replacementFingerprint(F, 1).isEmpty());
        assertFalse(hub.approveReplacement(F, 1));
        assertEquals(phone.publicKey(), new LugarJuezRepository(db).publicKey(F, 1).orElseThrow());
    }

    /** Revisión de seguridad: al cerrar la pelea no queda ningún cambio de teléfono por aprobar. */
    @Test
    void alCerrarLaPeleaElCambioPendienteSeDescarta() throws Exception {
        approvedAndDisconnected();
        var code = fights.reinvite(F, 1, HOST, 8765).code();
        var c = open();
        send(c, new Phone("B-2").join(F, 1, code, c.nonce()));
        assertInstanceOf(JoinPending.class, c.last());

        new PeleaRepository(db).updateEstado(F, EstadoPelea.CERRADA);
        assertFalse(hub.approveReplacement(F, 1), "pelea cerrada");
        hub.fightClosed(F);
        assertTrue(hub.replacementFingerprint(F, 1).isEmpty());
        assertEquals(phone.publicKey(), new LugarJuezRepository(db).publicKey(F, 1).orElseThrow());
    }
}
