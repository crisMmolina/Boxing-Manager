package com.sigebox.central.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.db.TarjetaRoundRepository;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.EventoBitacora;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.TarjetaRound;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RoundServiceTest {

    private static final String F = "pelea-1";

    @TempDir
    Path dir;

    private Database db;
    private RoundService service;
    private final List<String> broadcasts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        new PeleaRepository(db).save(new Pelea(F, "Rojo", "Azul", 3, 2, EstadoPelea.CREADA, "2026-10-25"));
        var seats = new LugarJuezRepository(db);
        seats.save(new LugarJuez(F, 1, "Ana", "A", "fp", EstadoConexion.CONECTADO, "central", 1L));
        seats.save(new LugarJuez(F, 2, "María López", "B", "fp", EstadoConexion.CONECTADO, "central", 1L));
        var rounds = new RoundRepository(db);
        service = new RoundService(db, new RoundService.Broadcaster() {
            @Override
            public void roundOpened(String fightId, int round) {
                // Persistido antes de difundir.
                assertEquals(EstadoRound.ABIERTO, rounds.find(fightId, round).orElseThrow().estado());
                broadcasts.add("open " + round);
            }

            @Override
            public void roundClosed(String fightId, int round, boolean forced) {
                assertEquals(EstadoRound.CERRADO, rounds.find(fightId, round).orElseThrow().estado());
                broadcasts.add("close " + round + (forced ? " forced" : ""));
            }
        }, () -> 5_000L);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private void card(int seat, int round, int red, int blue, EstadoTarjeta estado) {
        new TarjetaRoundRepository(db).save(new TarjetaRound(F, seat, round, red, blue, estado,
                estado == EstadoTarjeta.CORREGIDA ? OrigenTarjeta.ORGANIZADOR : OrigenTarjeta.JUEZ, 1L));
    }

    private List<String> logTypes() {
        return new EventLog(db).byPelea(F).stream().map(EventoBitacora::tipo).toList();
    }

    @Test
    void closeRoundWithMissingJudgeRequiresTheWord() {
        service.openRound(F, 1);
        card(1, 1, 10, 9, EstadoTarjeta.ENVIADA);

        var ex = assertThrows(RoundService.ConfirmationRequired.class, () -> service.closeRound(F, 1, null));
        assertEquals(List.of(2), ex.missing());
        assertThrows(RoundService.ConfirmationRequired.class, () -> service.closeRound(F, 1, "avanzar"));
        assertEquals(EstadoRound.ABIERTO, new RoundRepository(db).find(F, 1).orElseThrow().estado());

        assertEquals(List.of(2), service.closeRound(F, 1, " AVANZAR "));
        var round = new RoundRepository(db).find(F, 1).orElseThrow();
        assertEquals(EstadoRound.CERRADO, round.estado());
        assertTrue(round.cierreForzado());
        assertTrue(logTypes().contains("ROUND_CLOSED_FORCED"));
        assertEquals(List.of("open 1", "close 1 forced"), broadcasts);
    }

    @Test
    void closeRoundWithEveryoneIsNotForced() {
        service.openRound(F, 1);
        card(1, 1, 10, 9, EstadoTarjeta.ENVIADA);
        card(2, 1, 10, 9, EstadoTarjeta.ENVIADA);
        assertTrue(service.closeRound(F, 1, null).isEmpty());
        assertFalse(new RoundRepository(db).find(F, 1).orElseThrow().cierreForzado());
        assertEquals(List.of("open 1", "close 1"), broadcasts);
    }

    @Test
    void roundsOpenOneAtATimeAndInOrder() {
        assertThrows(IllegalStateException.class, () -> service.openRound(F, 2)); // falta cerrar el 1
        service.openRound(F, 1);
        assertEquals(EstadoPelea.EN_CURSO, new PeleaRepository(db).find(F).orElseThrow().estado());
        assertThrows(IllegalStateException.class, () -> service.openRound(F, 1)); // ya abierto
        assertThrows(IllegalArgumentException.class, () -> service.openRound(F, 4)); // 3 rounds
        service.closeRound(F, 1, RoundService.CONFIRM_WORD);
        assertThrows(IllegalStateException.class, () -> service.openRound(F, 1)); // ya cerrado
        assertThrows(IllegalStateException.class, () -> service.closeRound(F, 2, null)); // no abierto
        assertEquals(2, service.currentRound(F));
    }

    @Test
    void boardShowsStatusPerJudgeWithoutValues() {
        service.openRound(F, 1);
        assertEquals(List.of(1, 2), service.board(F, 1).missing());
        card(1, 1, 10, 9, EstadoTarjeta.TARDIA);
        card(2, 1, 9, 10, EstadoTarjeta.CORREGIDA);

        var board = service.board(F, 1);
        assertEquals(EstadoRound.ABIERTO, board.estado());
        assertEquals(List.of(
                new RoundService.Slot(1, "Ana", RoundService.Casilla.TARDIO),
                new RoundService.Slot(2, "María López", RoundService.Casilla.CORREGIDO)), board.slots());
        assertTrue(board.missing().isEmpty());
    }

    @Test
    void partialSummaryNeedsConfirmationAndIsLogged() {
        service.openRound(F, 1);
        card(1, 1, 10, 9, EstadoTarjeta.ENVIADA);
        card(2, 1, 10, 8, EstadoTarjeta.ENVIADA);
        service.closeRound(F, 1, null);
        service.openRound(F, 2);
        card(1, 2, 9, 10, EstadoTarjeta.ENVIADA); // round abierto: no cuenta

        assertThrows(IllegalStateException.class, () -> service.partialSummary(F, false));
        assertFalse(logTypes().contains("PARTIAL_SUMMARY_VIEWED"));

        var totals = service.partialSummary(F, true);
        assertEquals(List.of(
                new RoundService.PartialTotal(1, "Ana", 10, 9),
                new RoundService.PartialTotal(2, "María López", 10, 8)), totals);
        assertTrue(logTypes().contains("PARTIAL_SUMMARY_VIEWED"));
        assertEquals(15, RoundService.SUMMARY_VISIBLE_SECONDS);
    }

    @Test
    void currentRoundIsZeroWhenAllRoundsAreClosed() {
        for (int n = 1; n <= 3; n++) {
            service.openRound(F, n);
            service.closeRound(F, n, RoundService.CONFIRM_WORD);
        }
        assertEquals(0, service.currentRound(F));
    }
}
