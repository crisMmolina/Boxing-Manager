package com.sigebox.central.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.protocol.JoinUri;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FightServiceTest {

    @TempDir
    Path dir;

    private Database db;
    private SeatTokens tokens;
    private FightService service;

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        tokens = new SeatTokens();
        service = new FightService(db, tokens, () -> LocalDate.of(2026, 10, 25));
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void createRejectsThirteenRounds() {
        var ex = assertThrows(IllegalArgumentException.class, () -> service.create("Roja", "Azul", 13, 3));
        assertTrue(ex.getMessage().contains("1 a 12"), ex.getMessage());
        assertTrue(service.active().isEmpty());
    }

    @Test
    void createValidatesJudgesAndNames() {
        assertThrows(IllegalArgumentException.class, () -> service.create("Roja", "Azul", 0, 3));
        assertThrows(IllegalArgumentException.class, () -> service.create("Roja", "Azul", 4, 0));
        assertThrows(IllegalArgumentException.class, () -> service.create("Roja", "Azul", 4, 6));
        assertThrows(IllegalArgumentException.class, () -> service.create("  ", "Azul", 4, 3));
        assertThrows(IllegalArgumentException.class, () -> service.create("Roja", "x".repeat(81), 4, 3));
        assertThrows(IllegalArgumentException.class, () -> service.create("Ro\nja", "Azul", 4, 3));
    }

    @Test
    void createsAShortIdFightAndOnlyOneActive() {
        var p = service.create(" M. Hernández ", "J. Ortega", 10, 3);
        assertTrue(p.id().length() <= 8, p.id());
        assertTrue(p.id().chars().allMatch(c -> FightService.ID_ALPHABET.indexOf(c) >= 0), p.id());
        assertEquals("M. Hernández", p.esquinaRoja());
        assertEquals(EstadoPelea.CREADA, p.estado());
        assertEquals("2026-10-25", p.fecha());
        assertEquals(p, new PeleaRepository(db).find(p.id()).orElseThrow());

        assertThrows(IllegalStateException.class, () -> service.create("A", "B", 3, 1));
        new PeleaRepository(db).updateEstado(p.id(), EstadoPelea.CERRADA);
        assertNotEquals(p.id(), service.create("A", "B", 3, 1).id());
    }

    @Test
    void invitationHasQrUriAndSixDigitCodeThatBothJoin() {
        var p = service.create("Roja", "Azul", 4, 3);
        var inv = service.invite(p.id(), 2, "192.168.43.1", 8765);
        assertTrue(inv.code().matches("\\d{6}"), inv.code());
        var uri = JoinUri.parse(inv.uri());
        assertEquals("192.168.43.1", uri.host());
        assertEquals(8765, uri.port());
        assertEquals(p.id(), uri.fightId());
        assertEquals(2, uri.seat());
        assertTrue(uri.token().length() >= 22);

        assertEquals(SeatTokens.Check.OK, tokens.consume(p.id(), 2, inv.code()));
        assertEquals(SeatTokens.Check.USED, tokens.consume(p.id(), 2, uri.token())); // uno usa los dos

        var again = service.invite(p.id(), 3, "192.168.43.1", 8765);
        var newer = service.invite(p.id(), 3, "192.168.43.1", 8765);
        assertEquals(SeatTokens.Check.INVALID, tokens.consume(p.id(), 3, JoinUri.parse(again.uri()).token()));
        assertEquals(SeatTokens.Check.OK, tokens.consume(p.id(), 3, JoinUri.parse(newer.uri()).token()));
        assertThrows(IllegalArgumentException.class, () -> service.invite(p.id(), 4, "h", 8765));
    }

    @Test
    void seatsShowStateNameAndShortFingerprint() {
        var p = service.create("Roja", "Azul", 4, 3);
        var seats = new LugarJuezRepository(db);
        seats.save(new LugarJuez(p.id(), 1, "Ana", "A37", "ab12cd34ef" + "0".repeat(54), EstadoConexion.PENDIENTE,
                null, null));
        seats.save(new LugarJuez(p.id(), 2, "María", "A37", "ff" + "0".repeat(62), EstadoConexion.CONECTADO,
                "central", 5L));

        var list = service.seats(p.id(), List.of(2));
        assertEquals(new FightService.Seat(1, FightService.SeatState.ESPERANDO_APROBACION, "Ana", "AB12 CD34"),
                list.get(0));
        assertEquals(FightService.SeatState.CONECTADO, list.get(1).state());
        assertEquals(FightService.SeatState.SIN_CONECTAR, list.get(2).state());
        assertEquals(FightService.SeatState.SIN_CONEXION, service.seats(p.id(), List.of()).get(1).state());
        assertEquals(2, service.pendingSeats(p.id(), List.of(2)));
        assertNull(FightService.shortFingerprint(null));
    }

    @Test
    void localAddressesPreferPrivateNetworks() {
        assertEquals(List.of("192.168.43.1", "10.0.0.5", "172.20.1.2", "8.8.8.8"),
                FightService.rankAddresses(List.of("8.8.8.8", "127.0.0.1", "169.254.3.4", "172.20.1.2", "10.0.0.5",
                        "192.168.43.1")));
        assertEquals(List.of("127.0.0.1"), FightService.rankAddresses(List.of()));
        assertTrue(FightService.localAddresses().size() >= 1);
    }
}
