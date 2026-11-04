package com.sigebox.central.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.TarjetaFinalRepository;
import com.sigebox.central.db.TarjetaRoundRepository;
import com.sigebox.central.server.JudgeHubTest.FakeConn;
import com.sigebox.central.server.JudgeHubTest.Phone;
import com.sigebox.central.service.CorrectionService;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.RoundService;
import com.sigebox.central.service.TerminationService;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Resultado;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.FinalCardHash;
import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.JoinPending;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.NextFight;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitFinalCard;
import com.sigebox.core.protocol.SubmitRound;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * El traspaso con la pelea cerrada de verdad: terminación y {@link ResultService#close}, no un cambio de estado a
 * mano. Lo que entrega el teléfono que vuelve queda guardado sin mover el resultado.
 */
class JudgeHubHandoffTest {

    @TempDir
    Path dir;

    private Database db;
    private SeatTokens tokens;
    private JudgeHub hub;
    private FuncionService funciones;
    private RoundService rounds;
    private ResultService resultados;
    private TerminationService terminaciones;
    private CorrectionService correcciones;
    private TarjetaRoundRepository tarjetas;
    private String funcionId;

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        tokens = new SeatTokens();
        hub = new JudgeHub(db, tokens, ServerEvents.NONE, Duration.ofSeconds(30), () -> 1_000L);
        funciones = new FuncionService(db, new FightService(db, tokens, () -> LocalDate.of(2026, 10, 26)));
        funciones.alSiguiente(hub::nextFight);
        rounds = RoundService.forHub(db, hub);
        resultados = ResultService.forHub(db, hub);
        terminaciones = TerminationService.forHub(db, hub);
        correcciones = new CorrectionService(db, () -> 2_000L);
        tarjetas = new TarjetaRoundRepository(db);
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

    private static SubmitRound submit(String fightId, int seat, int round, int red, int blue) {
        return new SubmitRound(1, UUID.randomUUID().toString(), SubmitRound.TYPE, fightId, seat, round, red, blue);
    }

    private Pelea primeraPelea() {
        var f = funciones.crear(3, 2, List.of(new FuncionService.Esquinas("Ana", "Bea"),
                new FuncionService.Esquinas("Caro", "Dani")));
        funcionId = f.id();
        return funciones.siguiente(f.id());
    }

    /**
     * Round 1 con las dos tarjetas, decisión técnica en el round 2 (que no cuenta) y cierre con el servicio real. El
     * teléfono {@code a} se fue antes del round 2 y la función ya pasó a la siguiente pelea.
     */
    private Resultado cerradaPorTerminacion(String peleaId, Phone a, Phone b) {
        var c1 = approvedJudge(peleaId, a, 1);
        var c2 = approvedJudge(peleaId, b, 2);
        rounds.openRound(peleaId, 1);
        send(c1, submit(peleaId, 1, 1, 10, 9));
        send(c2, submit(peleaId, 2, 1, 10, 8));
        rounds.closeRound(peleaId, 1, null);
        hub.onClose(c1.id());
        rounds.openRound(peleaId, 2);
        terminaciones.terminate(peleaId, new TerminationService.Input(
                MotivoTerminacion.DECISION_TECNICA, null, null, 2, "1:30", null), TerminationService.CONFIRM_WORD);
        var resultado = resultados.close(peleaId);
        assertEquals(EstadoPelea.CERRADA, new PeleaRepository(db).find(peleaId).orElseThrow().estado());
        funciones.siguiente(funcionId);
        return resultado;
    }

    private FakeConn vuelve(String peleaId, Phone phone, int seat) {
        var c = open();
        send(c, phone.hello(peleaId, seat, c.nonce()));
        assertInstanceOf(NextFight.class, c.last());
        return c;
    }

    private List<String> bitacora(String peleaId, String tipo) {
        return new EventLog(db).byPelea(peleaId).stream()
                .filter(e -> e.tipo().equals(tipo))
                .map(e -> e.detalle())
                .toList();
    }

    @Test
    void elTelefonoQueVuelveEntregaComoTardiaUnRoundQueNoContoSinMoverElResultado() throws Exception {
        var p1 = primeraPelea();
        var a = new Phone("A-1");
        var guardado = cerradaPorTerminacion(p1.id(), a, new Phone("B-2"));

        var c = vuelve(p1.id(), a, 1);
        send(c, submit(p1.id(), 1, 2, 7, 10));

        var ack = assertInstanceOf(Ack.class, c.last());
        assertTrue(ack.ok(), String.valueOf(ack.error()));
        assertEquals(EstadoTarjeta.TARDIA, tarjetas.find(p1.id(), 1, 2).orElseThrow().estado());
        assertEquals(guardado, resultados.stored(p1.id()).orElseThrow());
        assertEquals(EstadoPelea.CERRADA, new PeleaRepository(db).find(p1.id()).orElseThrow().estado());
    }

    @Test
    void laTarjetaFinalLlegaPorElTraspasoTrasUnaTerminacion() throws Exception {
        var p1 = primeraPelea();
        var a = new Phone("A-1");
        var guardado = cerradaPorTerminacion(p1.id(), a, new Phone("B-2"));

        var c = vuelve(p1.id(), a, 1);
        send(c, submit(p1.id(), 1, 2, 7, 10));
        var own = List.of(new State.OwnCard(1, 10, 9, EstadoTarjeta.ENVIADA),
                new State.OwnCard(2, 7, 10, EstadoTarjeta.TARDIA));
        var hash = FinalCardHash.of(p1.id(), 1, own);
        var sig = DeviceProof.sign(a.keys.getPrivate(), DeviceProof.finalCardText(p1.id(), 1, own));
        send(c, new SubmitFinalCard(p1.id(), 1, hash, sig));

        var ack = assertInstanceOf(Ack.class, c.last());
        assertTrue(ack.ok(), String.valueOf(ack.error()));
        assertEquals(1, new TarjetaFinalRepository(db).byPelea(p1.id()).size());
        assertEquals(sig, new TarjetaFinalRepository(db).signature(p1.id(), 1).orElseThrow());
        assertEquals(1, bitacora(p1.id(), "SUBMIT_FINAL_CARD").size());
        assertEquals(guardado, resultados.stored(p1.id()).orElseThrow());
    }
}
