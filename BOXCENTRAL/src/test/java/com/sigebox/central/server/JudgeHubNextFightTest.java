package com.sigebox.central.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.FuncionRepository;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.server.JudgeHubTest.FakeConn;
import com.sigebox.central.server.JudgeHubTest.Phone;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.FuncionService;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.JoinPending;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.NextFight;
import com.sigebox.core.protocol.State;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Spec 002 / ADR 0002: los jueces aprobados pasan a la siguiente pelea de la función sin QR. */
class JudgeHubNextFightTest {

    @TempDir
    Path dir;

    private Database db;
    private SeatTokens tokens;
    private JudgeHub hub;
    private FuncionService funciones;
    private LugarJuezRepository lugares;

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        tokens = new SeatTokens();
        hub = new JudgeHub(db, tokens, ServerEvents.NONE, Duration.ofSeconds(30), () -> 1_000L);
        funciones = new FuncionService(db, new FightService(db, tokens, () -> LocalDate.of(2026, 10, 26)));
        funciones.alSiguiente(hub::nextFight);
        lugares = new LugarJuezRepository(db);
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

    private FakeConn approvedJudge(String fightId, Phone phone, int seat) {
        var c = open();
        send(c, phone.join(fightId, seat, tokens.issue(fightId, seat), c.nonce()));
        assertInstanceOf(JoinPending.class, c.last());
        assertTrue(hub.approve(fightId, seat));
        assertInstanceOf(JoinApproved.class, c.last());
        return c;
    }

    private void cerrar(String peleaId) {
        new PeleaRepository(db).updateEstado(peleaId, EstadoPelea.CERRADA);
    }

    private List<String> bitacora(String peleaId) {
        return new EventLog(db).byPelea(peleaId).stream().map(e -> e.tipo()).toList();
    }

    @Test
    void alIniciarLaSiguienteAmbosJuecesRecibenSuLugarYSuHelloSeAceptaSinAprobacion() throws Exception {
        var f = funciones.crear(3, 2, List.of(new FuncionService.Esquinas("Ana", "Bea"),
                new FuncionService.Esquinas("Caro", "Dani")));
        var p1 = funciones.siguiente(f.id());
        var a = new Phone("A-1");
        var b = new Phone("B-2");
        var c1 = approvedJudge(p1.id(), a, 1);
        var c2 = approvedJudge(p1.id(), b, 2);
        cerrar(p1.id());

        var p2 = funciones.siguiente(f.id());

        var n1 = assertInstanceOf(NextFight.class, c1.last());
        var n2 = assertInstanceOf(NextFight.class, c2.last());
        assertEquals(p1.id(), n1.fightId());
        assertEquals(p2.id(), n1.nextFightId());
        assertEquals(1, n1.seat());
        assertEquals(2, n2.seat());
        assertEquals(List.of("Caro", "Dani", 3), List.of(n1.redCorner(), n1.blueCorner(), n1.rounds()));

        // Copiados con su nombre, dispositivo, huella y llave; aprobados y sin conectar.
        var copiado = lugares.find(p2.id(), 1).orElseThrow();
        var original = lugares.find(p1.id(), 1).orElseThrow();
        assertEquals(original.nombre(), copiado.nombre());
        assertEquals(original.dispositivo(), copiado.dispositivo());
        assertEquals(original.huellaCredencial(), copiado.huellaCredencial());
        assertEquals(EstadoConexion.DESCONECTADO, copiado.conexion());
        assertNotNull(copiado.aprobadoEnMs());
        assertEquals(a.publicKey(), lugares.publicKey(p2.id(), 1).orElseThrow());

        // Reconecta a la pelea nueva con HELLO y entra sin aprobación.
        var r = open();
        send(r, a.hello(p2.id(), 1, r.nonce()));
        var state = assertInstanceOf(State.class, r.last());
        assertEquals(p2.id(), state.fightId());
        assertEquals(EstadoConexion.CONECTADO, lugares.find(p2.id(), 1).orElseThrow().conexion());

        assertTrue(bitacora(p1.id()).contains("SIGUIENTE_PELEA"));
        assertTrue(bitacora(p2.id()).contains("SIGUIENTE_PELEA"));
    }

    @Test
    void conMenosJuecesLosSobrantesRecibenSeatNuloYLosNuevosQuedanParaQr() throws Exception {
        var f = funciones.crear(3, 3, List.of(new FuncionService.Esquinas("Ana", "Bea"),
                new FuncionService.Esquinas("Caro", "Dani")));
        funciones.cambiar(f.id(), 2, null, 2);
        var p1 = funciones.siguiente(f.id());
        var c1 = approvedJudge(p1.id(), new Phone("A-1"), 1);
        var c3 = approvedJudge(p1.id(), new Phone("C-3"), 3);
        cerrar(p1.id());

        var p2 = funciones.siguiente(f.id());

        assertEquals(1, assertInstanceOf(NextFight.class, c1.last()).seat());
        assertNull(assertInstanceOf(NextFight.class, c3.last()).seat());
        assertTrue(lugares.find(p2.id(), 3).isEmpty());
        assertTrue(lugares.find(p2.id(), 2).isEmpty(), "el lugar 2 no estaba aprobado: queda para el QR");
    }

    @Test
    void unTelefonoDesconectadoSePoneAlDiaConHelloDeLaPeleaCerrada() throws Exception {
        var f = funciones.crear(3, 2, List.of(new FuncionService.Esquinas("Ana", "Bea"),
                new FuncionService.Esquinas("Caro", "Dani")));
        var p1 = funciones.siguiente(f.id());
        var a = new Phone("A-1");
        var c1 = approvedJudge(p1.id(), a, 1);
        hub.onClose(c1.id());
        cerrar(p1.id());
        var p2 = funciones.siguiente(f.id());

        var r = open();
        send(r, a.hello(p1.id(), 1, r.nonce()));
        var n = assertInstanceOf(NextFight.class, r.last());
        assertEquals(p2.id(), n.nextFightId());
        assertEquals(1, n.seat());

        // Otra llave con el mismo lugar y dispositivo: se rechaza como hoy.
        var intruso = new Phone("A-1");
        var x = open();
        send(x, intruso.hello(p1.id(), 1, x.nonce()));
        assertEquals(1008, x.closedWith);
        assertFalse(x.sent.stream().anyMatch(NextFight.class::isInstance));
    }

    @Test
    void unHelloDeLaPeleaCerradaSinFuncionActivaSeAtiendeComoHoy() throws Exception {
        var f = funciones.crear(3, 2, List.of(new FuncionService.Esquinas("Ana", "Bea")));
        var p1 = funciones.siguiente(f.id());
        var a = new Phone("A-1");
        approvedJudge(p1.id(), a, 1);
        cerrar(p1.id());

        var r = open();
        send(r, a.hello(p1.id(), 1, r.nonce()));
        assertInstanceOf(State.class, r.last());
    }

    @Test
    void unTelefonoNoPuedeMandarNextFight() throws Exception {
        var f = funciones.crear(3, 2, List.of(new FuncionService.Esquinas("Ana", "Bea")));
        var p1 = funciones.siguiente(f.id());
        var c1 = approvedJudge(p1.id(), new Phone("A-1"), 1);

        var falso = new NextFight(p1.id(), "otra", 1, "Rojo", "Azul", 3);
        send(c1, falso);

        var ack = assertInstanceOf(Ack.class, c1.last());
        assertFalse(ack.ok());
        assertEquals(falso.id(), ack.ackId());
        assertEquals(0, c1.sent.stream().filter(m -> m instanceof NextFight).count(), "no hay eco");
    }
}
