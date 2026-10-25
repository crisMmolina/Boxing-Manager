package com.sigebox.central.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.db.TerminacionRepository;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EventoBitacora;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Round;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TerminationServiceTest {

    private static final String F = "pelea-1";
    private static final String OK = TerminationService.CONFIRM_WORD;

    @TempDir
    Path dir;

    private Database db;
    private TerminationService service;
    private final List<String> ended = new ArrayList<>();

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        new PeleaRepository(db).save(new Pelea(F, "Rojo", "Azul", 4, 3, EstadoPelea.EN_CURSO, "2026-10-25"));
        var rounds = new RoundRepository(db);
        rounds.save(new Round(F, 1, EstadoRound.CERRADO, 1L, 2L, false));
        rounds.save(new Round(F, 2, EstadoRound.ABIERTO, 3L, null, false));
        service = new TerminationService(db, (f, m) -> {
            // Persistido antes de avisar a los jueces.
            assertEquals(EstadoPelea.TERMINADA, new PeleaRepository(db).find(f).orElseThrow().estado());
            ended.add(f + " " + m);
        }, () -> 9L);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private static TerminationService.Input input(MotivoTerminacion m, String otro, Esquina g, int round) {
        return new TerminationService.Input(m, otro, g, round, "1:42", null);
    }

    @Test
    void otroWithoutReasonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.terminate(F, input(MotivoTerminacion.OTRO, null, null, 2), OK));
        assertThrows(IllegalArgumentException.class,
                () -> service.terminate(F, input(MotivoTerminacion.OTRO, "   ", null, 2), OK));
        assertTrue(new TerminacionRepository(db).find(F).isEmpty());
        assertEquals(EstadoPelea.EN_CURSO, new PeleaRepository(db).find(F).orElseThrow().estado());

        var t = service.terminate(F, input(MotivoTerminacion.OTRO, "Corte de luz en la sede", null, 2), OK);
        assertEquals("Corte de luz en la sede", t.textoOtro());
    }

    @Test
    void koNeedsAWinnerAndTechnicalDecisionMustNotHaveOne() {
        assertThrows(IllegalArgumentException.class,
                () -> service.terminate(F, input(MotivoTerminacion.KO, null, null, 2), OK));
        assertThrows(IllegalArgumentException.class,
                () -> service.terminate(F, input(MotivoTerminacion.DECISION_TECNICA, null, Esquina.ROJA, 2), OK));
        assertThrows(IllegalArgumentException.class,
                () -> service.terminate(F, input(MotivoTerminacion.SIN_DECISION, null, Esquina.AZUL, 2), OK));
        var t = service.terminate(F, input(MotivoTerminacion.DECISION_TECNICA, null, null, 2), OK);
        assertNull(t.ganadora());
    }

    @Test
    void requiresTheConfirmationWord() {
        var in = input(MotivoTerminacion.KO, null, Esquina.ROJA, 2);
        assertThrows(IllegalStateException.class, () -> service.terminate(F, in, null));
        assertThrows(IllegalStateException.class, () -> service.terminate(F, in, "terminar"));
        assertTrue(ended.isEmpty());
        service.terminate(F, in, " TERMINAR ");
        assertEquals(List.of(F + " KO"), ended);
    }

    @Test
    void validatesRoundAndTime() {
        // Rounds alcanzados: 1 cerrado y 2 abierto; el 3 cuenta como abandono entre rounds.
        assertThrows(IllegalArgumentException.class,
                () -> service.terminate(F, input(MotivoTerminacion.ABANDONO, null, Esquina.AZUL, 4), OK));
        assertThrows(IllegalArgumentException.class,
                () -> service.terminate(F, input(MotivoTerminacion.ABANDONO, null, Esquina.AZUL, 0), OK));
        assertThrows(IllegalArgumentException.class, () -> service.terminate(F,
                new TerminationService.Input(MotivoTerminacion.KO, null, Esquina.ROJA, 2, "90", null), OK));
        assertThrows(IllegalArgumentException.class, () -> service.terminate(F,
                new TerminationService.Input(MotivoTerminacion.KO, null, Esquina.ROJA, 2, "1:42", "x".repeat(501)),
                OK));
        var t = service.terminate(F, input(MotivoTerminacion.ABANDONO, null, Esquina.AZUL, 3), OK);
        assertEquals(3, t.round());
    }

    @Test
    void terminationClosesTheOpenRoundAndIsLogged() {
        var t = service.terminate(F, new TerminationService.Input(MotivoTerminacion.MEDICO, null, Esquina.ROJA, 2,
                " 2:05 ", "Corte en la ceja"), OK);

        assertEquals(t, new TerminacionRepository(db).find(F).orElseThrow());
        assertEquals("2:05", t.tiempo());
        var r2 = new RoundRepository(db).find(F, 2).orElseThrow();
        assertEquals(EstadoRound.CERRADO, r2.estado());
        assertTrue(r2.cierreForzado());
        assertEquals(EstadoPelea.TERMINADA, new PeleaRepository(db).find(F).orElseThrow().estado());
        var types = new EventLog(db).byPelea(F).stream().map(EventoBitacora::tipo).toList();
        assertTrue(types.contains("FIGHT_TERMINATED"), types.toString());
        assertTrue(types.contains("ROUND_CLOSED_FORCED"), types.toString());

        assertThrows(IllegalStateException.class,
                () -> service.terminate(F, input(MotivoTerminacion.KO, null, Esquina.ROJA, 2), OK)); // ya terminada
    }

    @Test
    void labelsCoverEveryReason() {
        for (var m : MotivoTerminacion.values()) {
            assertTrue(!TerminationService.label(m).isBlank());
        }
        assertEquals("Detenida por el médico", TerminationService.label(MotivoTerminacion.MEDICO));
        assertTrue(TerminationService.requiresCorner(MotivoTerminacion.KOT));
    }
}
