package com.sigebox.central.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.server.JudgeHubTest.FakeConn;
import com.sigebox.central.server.JudgeHubTest.Phone;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.RoundService;
import com.sigebox.central.service.TerminationService;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.CodeInfo;
import com.sigebox.core.protocol.CodeLookup;
import com.sigebox.core.protocol.CodeUnknown;
import com.sigebox.core.protocol.EventEnded;
import com.sigebox.core.protocol.FightClosed;
import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.JoinPending;
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

/** Unirse con el código de lugar y aviso de fin de evento (ADR 0004, plan 004 §Threat model). */
class JudgeHubCodeTest {

    private static final String F = "PELEA1";

    @TempDir
    Path dir;

    private Database db;
    private SeatTokens tokens;
    private JudgeHub hub;
    private final AtomicLong now = new AtomicLong(10_000);
    private FuncionService funciones;
    private RoundService rounds;
    private ResultService resultados;
    private TerminationService terminaciones;

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        new PeleaRepository(db).save(new Pelea(F, "Rojo", "Azul", 3, 3, EstadoPelea.CREADA, "2026-11-04"));
        tokens = new SeatTokens();
        hub = new JudgeHub(db, tokens, ServerEvents.NONE, Duration.ofSeconds(30), now::get);
        funciones = new FuncionService(db, new FightService(db, tokens, () -> LocalDate.of(2026, 11, 4)));
        funciones.alSiguiente(hub::nextFight);
        rounds = RoundService.forHub(db, hub);
        resultados = ResultService.forHub(db, hub);
        terminaciones = TerminationService.forHub(db, hub);
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

    private List<String> anonymousLog() {
        return db.inTransaction(c -> {
            var out = new ArrayList<String>();
            try (var ps = c.prepareStatement("SELECT type FROM event_log WHERE fight_id IS NULL ORDER BY id");
                    var rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
            return out;
        });
    }

    @Test
    void codigoDeLugarLibreDaPeleaLugarYEsquinasYSeUneConElJoinDeSiempre() throws Exception {
        var code = tokens.issueInvitation(F, 2).code();
        var c = open();
        lookup(c, code);

        var info = assertInstanceOf(CodeInfo.class, c.last());
        assertEquals(List.of(code, F, 2, "Rojo", "Azul"),
                List.of(info.code(), info.fightId(), info.seat(), info.redCorner(), info.blueCorner()));
        assertNull(c.closedWith);

        var phone = new Phone("A-1");
        send(c, phone.join(info.fightId(), info.seat(), code, c.nonce()));
        assertInstanceOf(JoinPending.class, c.last());
        assertTrue(hub.approve(F, 2), "la aprobación del organizador sigue siendo obligatoria");
        assertInstanceOf(JoinApproved.class, c.last());
    }

    @Test
    void codigoUsadoDesconocidoODePeleaCerradaDaCodeUnknownSinDecirCual() throws Exception {
        var used = tokens.issueInvitation(F, 1).code();
        var c = open();
        send(c, new Phone("A-1").join(F, 1, used, c.nonce()));
        assertInstanceOf(JoinPending.class, c.last());

        new PeleaRepository(db).save(new Pelea("CERRAD", "R", "A", 3, 3, EstadoPelea.CERRADA, "2026-11-04"));
        var closed = tokens.issueInvitation("CERRAD", 1).code();

        var other = open();
        for (var code : List.of(used, "000000".equals(used) ? "111111" : "000000", closed)) {
            lookup(other, code);
            var unknown = assertInstanceOf(CodeUnknown.class, other.last());
            assertEquals(code, unknown.code());
        }
        assertNull(other.closedWith);
    }

    @Test
    void unLugarYaRegistradoNoSeVuelveAOfrecerAunqueSuCodigoSigaVivo() throws Exception {
        var inv = tokens.issueInvitation(F, 1);
        var c = open();
        send(c, new Phone("A-1").join(F, 1, inv.token(), c.nonce()));
        assertInstanceOf(JoinPending.class, c.last());

        var other = open();
        lookup(other, inv.code());
        assertInstanceOf(CodeUnknown.class, other.last());
    }

    @Test
    void alQuintoCodeUnknownSeCierraLaConexionYQuedaEnLaBitacora() {
        tokens.issueInvitation(F, 1);
        var c = open();
        for (int i = 1; i <= 4; i++) {
            lookup(c, String.format("%06d", i));
            assertInstanceOf(CodeUnknown.class, c.last());
            assertNull(c.closedWith, "aún no: " + i);
        }
        lookup(c, "000005");
        assertInstanceOf(CodeUnknown.class, c.last());
        assertEquals(Conn.POLICY, c.closedWith);
        assertEquals(List.of("CODE_LOOKUP_LIMIT"), anonymousLog());
    }

    @Test
    void masDeUnaBusquedaPorSegundoNoSeConsultaYCuentaComoDesconocida() {
        var code = tokens.issueInvitation(F, 1).code();
        var c = open();
        send(c, new CodeLookup("999999"));
        send(c, new CodeLookup(code)); // mismo instante: no se consulta aunque sea bueno
        assertInstanceOf(CodeUnknown.class, c.last());

        now.addAndGet(1_000);
        send(c, new CodeLookup(code));
        assertInstanceOf(CodeInfo.class, c.last());
    }

    @Test
    void unaInundacionDeBusquedasCierraLaConexion() {
        tokens.issueInvitation(F, 1);
        var c = open();
        for (int i = 0; i < 5; i++) {
            send(c, new CodeLookup("123456"));
        }
        assertEquals(Conn.POLICY, c.closedWith);
        int answers = (int) c.sent.stream().filter(m -> m instanceof CodeUnknown).count();
        assertEquals(5, answers);
    }

    @Test
    void eventEndedQueMandaUnTelefonoSeRechaza() throws Exception {
        var c = open();
        send(c, new Phone("A-1").join(F, 1, tokens.issue(F, 1), c.nonce()));
        assertTrue(hub.approve(F, 1));

        send(c, new EventEnded(F));
        var ack = assertInstanceOf(Ack.class, c.last());
        assertFalse(ack.ok());

        var anon = open();
        send(anon, new EventEnded(F));
        assertEquals(Conn.POLICY, anon.closedWith, "sin autenticar se cierra");
    }

    /** Solo hay una pelea sin cerrar a la vez: la de {@code setUp} se cierra antes de crear la función. */
    private FuncionService.Funcion funcion(int jueces, String... parejas) {
        new PeleaRepository(db).updateEstado(F, EstadoPelea.CERRADA);
        var esquinas = new ArrayList<FuncionService.Esquinas>();
        for (int i = 0; i < parejas.length; i += 2) {
            esquinas.add(new FuncionService.Esquinas(parejas[i], parejas[i + 1]));
        }
        return funciones.crear(3, jueces, esquinas);
    }

    private FakeConn approvedJudge(String fightId, Phone phone, int seat) {
        var c = open();
        send(c, phone.join(fightId, seat, tokens.issue(fightId, seat), c.nonce()));
        assertTrue(hub.approve(fightId, seat));
        assertInstanceOf(JoinApproved.class, c.last());
        return c;
    }

    /** Termina la pelea en el round 1 con el servicio real y la cierra con {@link ResultService#close}. */
    private void cerrar(String peleaId) {
        rounds.openRound(peleaId, 1);
        terminaciones.terminate(peleaId, new TerminationService.Input(
                MotivoTerminacion.DECISION_TECNICA, null, null, 1, "1:30", null), TerminationService.CONFIRM_WORD);
        resultados.close(peleaId);
    }

    @Test
    void alCerrarLaUltimaPeleaDelEventoSeMandaEventEndedDespuesDeFightClosed() throws Exception {
        var f = funcion(1, "Ana", "Bea", "Caro", "Dani");
        var p1 = funciones.siguiente(f.id());
        var a = new Phone("A-1");
        var c1 = approvedJudge(p1.id(), a, 1);
        cerrar(p1.id());
        assertInstanceOf(FightClosed.class, c1.last(), "queda otra pelea: no termina el evento");

        var p2 = funciones.siguiente(f.id());
        var c2 = open(); // el lugar ya viene aprobado de la pelea anterior: vuelve con HELLO
        send(c2, a.hello(p2.id(), 1, c2.nonce()));
        assertInstanceOf(State.class, c2.last());
        cerrar(p2.id());

        var ended = assertInstanceOf(EventEnded.class, c2.last());
        assertEquals(p2.id(), ended.fightId());
        assertInstanceOf(FightClosed.class, c2.sent.get(c2.sent.size() - 2));
    }

    @Test
    void unaPeleaSaltadaNoCuentaComoPendiente() throws Exception {
        var f = funcion(1, "Ana", "Bea", "Caro", "Dani");
        var p1 = funciones.siguiente(f.id());
        funciones.saltar(f.id(), 2);
        var c1 = approvedJudge(p1.id(), new Phone("A-1"), 1);
        cerrar(p1.id());
        assertInstanceOf(EventEnded.class, c1.last());
    }

    @Test
    void alCerrarseUnaPeleaSusCodigosSinUsarDejanDeServir() throws Exception {
        var f = funcion(2, "Ana", "Bea");
        var p1 = funciones.siguiente(f.id());
        approvedJudge(p1.id(), new Phone("A-1"), 1);
        var free = tokens.issueInvitation(p1.id(), 2).code();
        assertTrue(tokens.lookup(free).isPresent());
        cerrar(p1.id());
        assertTrue(tokens.lookup(free).isEmpty());
    }
}
