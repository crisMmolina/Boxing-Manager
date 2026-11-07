package com.sigebox.central.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.server.JudgeHubTest.FakeConn;
import com.sigebox.central.server.JudgeHubTest.Phone;
import com.sigebox.central.service.FightService;
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
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Spec 005 §Evento (DSK-040): bajar los jueces de la pelea en Jueces cierra el paso a los lugares que sobran. */
class JudgeHubCambioTest {

    @TempDir
    Path dir;

    private Database db;
    private SeatTokens tokens;
    private JudgeHub hub;
    private FightService fights;
    private final AtomicLong now = new AtomicLong(10_000);
    private String id;

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        tokens = new SeatTokens();
        hub = new JudgeHub(db, tokens, ServerEvents.NONE, Duration.ofSeconds(30), now::get);
        fights = new FightService(db, tokens, () -> LocalDate.of(2026, 11, 6));
        fights.puerta(hub);
        id = fights.create("Rojo", "Azul", 10, 3).id();
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

    @Test
    void elTelefonoQueBuscoElLugarQueSobraNoSeUneYSuConexionSeCierra() throws Exception {
        var code = fights.invite(id, 3, "192.168.0.2", 8765).code();
        var c = open();
        lookup(c, code);
        assertEquals(3, assertInstanceOf(CodeInfo.class, c.last()).seat());

        fights.cambiar(id, null, 2);

        send(c, new Phone("A-3").join(id, 3, code, c.nonce()));
        assertInstanceOf(JoinRejected.class, c.last());
        assertNotNull(c.closedWith, "la conexión se cierra");

        var other = open();
        lookup(other, code);
        assertInstanceOf(CodeUnknown.class, other.last());
    }

    @Test
    void alCambiarLosRoundsElJuezAprobadoRecibeUnStateConLosNuevos() throws Exception {
        var c = open();
        send(c, new Phone("A-1").join(id, 1, fights.invite(id, 1, "192.168.0.2", 8765).code(), c.nonce()));
        assertInstanceOf(JoinPending.class, c.last());
        assertTrue(hub.approve(id, 1));
        assertEquals(10, assertInstanceOf(JoinApproved.class, c.last()).rounds());

        fights.cambiar(id, 6, 2);

        var state = assertInstanceOf(State.class, c.last());
        assertEquals(6, state.rounds());
        assertEquals(null, c.closedWith, "el juez del lugar 1 sigue conectado");
    }

    @Test
    void bajarSoloLosJuecesNoLeMandaNadaAlJuezQueQueda() throws Exception {
        var c = open();
        send(c, new Phone("A-1").join(id, 1, fights.invite(id, 1, "192.168.0.2", 8765).code(), c.nonce()));
        hub.approve(id, 1);
        int before = c.sent.size();

        fights.cambiar(id, null, 1);

        assertEquals(before, c.sent.size());
        assertEquals(1, hub.connectedSeats(id).size());
    }
}
