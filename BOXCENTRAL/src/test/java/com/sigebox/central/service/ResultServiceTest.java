package com.sigebox.central.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.sigebox.central.db.Database;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.db.TarjetaFinalRepository;
import com.sigebox.central.db.TarjetaRoundRepository;
import com.sigebox.central.db.TerminacionRepository;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.Ganador;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Resultado;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaFinal;
import com.sigebox.core.model.TarjetaRound;
import com.sigebox.core.model.Terminacion;
import com.sigebox.core.model.TipoDecision;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ResultServiceTest {

    /** demo_data/pelea_referencia.json (IDEA §3, calculada a mano). */
    public record Reference(Pelea pelea, List<LugarJuez> lugares, List<Round> rounds, List<TarjetaRound> tarjetas,
            Resultado esperado) {

        public static Reference load() throws Exception {
            var dir = Path.of("").toAbsolutePath();
            while (dir != null && !Files.exists(dir.resolve("demo_data/pelea_referencia.json"))) {
                dir = dir.getParent();
            }
            if (dir == null) {
                throw new IllegalStateException("no se encontró demo_data/pelea_referencia.json");
            }
            var json = Files.readString(dir.resolve("demo_data/pelea_referencia.json"), StandardCharsets.UTF_8);
            return new Gson().fromJson(json, Reference.class);
        }

        /** Carga la pelea en {@code db} con estado {@code estado}. */
        public void into(Database db, EstadoPelea estado) {
            var p = pelea;
            new PeleaRepository(db).save(new Pelea(p.id(), p.esquinaRoja(), p.esquinaAzul(), p.rounds(), p.jueces(),
                    estado, p.fecha()));
            lugares.forEach(new LugarJuezRepository(db)::save);
            rounds.forEach(new RoundRepository(db)::save);
            tarjetas.forEach(new TarjetaRoundRepository(db)::save);
        }
    }

    @TempDir
    Path dir;

    private Database db;
    private Reference ref;
    private String fight;
    private ResultService service;
    private final List<String> broadcasts = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        db = Database.open(dir.resolve("sigebox.db"));
        ref = Reference.load();
        fight = ref.pelea().id();
        service = new ResultService(db, new ResultService.Broadcaster() {
            @Override
            public void fightEnded(String fightId, MotivoTerminacion reason) {
                broadcasts.add("ended");
            }

            @Override
            public void fightClosed(String fightId) {
                assertEquals(EstadoPelea.CERRADA, new PeleaRepository(db).find(fightId).orElseThrow().estado());
                broadcasts.add("closed");
            }
        });
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private void finalCards(int... seats) {
        for (int s : seats) {
            new TarjetaFinalRepository(db).save(new TarjetaFinal(fight, s, "ab".repeat(32), 1L), "firma");
        }
    }

    @Test
    void referenceFightMatchesTheHandCalculation() {
        ref.into(db, EstadoPelea.TERMINADA);
        var r = service.compute(fight);
        assertEquals(Ganador.ROJA, r.ganador());
        assertEquals(TipoDecision.DIVIDIDA, r.tipo());
        assertEquals(List.of("39–37", "37–39", "39–37"),
                r.totales().stream().map(t -> t.rojo() + "–" + t.azul()).toList());
        assertEquals(new Gson().toJson(ref.esperado().conteo()), new Gson().toJson(r.conteo()));
        assertEquals("Gana Roja por decisión dividida 2–1", ResultService.headline(ref.pelea(), r, Optional.empty()));
    }

    @Test
    void closeNeedsEveryFinalCard() {
        ref.into(db, EstadoPelea.TERMINADA);
        finalCards(1, 3);
        assertFalse(service.canClose(fight));
        assertEquals(List.of(2), service.missingFinalCards(fight));
        var ex = assertThrows(IllegalStateException.class, () -> service.close(fight));
        assertTrue(ex.getMessage().contains("[2]"), ex.getMessage());

        finalCards(2);
        var r = service.close(fight);
        assertEquals(Ganador.ROJA, r.ganador());
        assertEquals(EstadoPelea.CERRADA, new PeleaRepository(db).find(fight).orElseThrow().estado());
        assertEquals(Ganador.ROJA, service.stored(fight).orElseThrow().ganador());
        assertEquals(List.of("closed"), broadcasts);
        assertEquals(r.ganador(), service.close(fight).ganador()); // cerrar de nuevo no cambia nada
        assertEquals(List.of("closed"), broadcasts);
    }

    @Test
    void terminationAllowsClosingWithoutFinalCards() {
        ref.into(db, EstadoPelea.TERMINADA);
        new TerminacionRepository(db).save(new Terminacion(fight, MotivoTerminacion.KO, null, Esquina.AZUL, 3,
                "1:42", null));
        assertTrue(service.canClose(fight));
        var r = service.close(fight);
        assertEquals(Ganador.AZUL, r.ganador());
        var sheet = service.sheet(fight);
        assertEquals("Gana Azul por KO (round 3, 1:42)", sheet.headline());
        // Rounds 3 y 4 no cuentan: se detuvo en el 3.
        var juez1 = sheet.rows().get(0);
        assertEquals(10, juez1.cells().get(0).red());
        assertEquals(null, juez1.cells().get(2).red());
        assertEquals(null, juez1.cells().get(3).red());
    }

    @Test
    void endFightNeedsEveryRoundClosed() {
        ref.into(db, EstadoPelea.EN_CURSO);
        var rounds = new RoundRepository(db);
        rounds.save(new Round(fight, 4, EstadoRound.ABIERTO, 1L, null, false));
        assertThrows(IllegalStateException.class, () -> service.endFight(fight));
        rounds.save(new Round(fight, 4, EstadoRound.CERRADO, 1L, 2L, false));
        service.endFight(fight);
        assertEquals(EstadoPelea.TERMINADA, new PeleaRepository(db).find(fight).orElseThrow().estado());
        assertEquals(List.of("ended"), broadcasts);
        assertThrows(IllegalStateException.class, () -> service.endFight(fight));
    }

    @Test
    void sheetHasNamesCardsAndTotals() {
        ref.into(db, EstadoPelea.TERMINADA);
        var sheet = service.sheet(fight);
        assertEquals(3, sheet.rows().size());
        var juez2 = sheet.rows().get(1);
        assertEquals("María López", juez2.name());
        assertEquals(37, juez2.totalRed());
        assertEquals(39, juez2.totalBlue());
        assertEquals("Azul", juez2.winner());
        assertEquals("TARDIA", juez2.cells().get(2).status());
        assertEquals(4, juez2.cells().size());
    }

    @Test
    void headlinesForOtherOutcomes() {
        var p = ref.pelea();
        var unanime = new Resultado(p.id(), List.of(
                new Resultado.TotalJuez(1, "A", 40, 36, Ganador.AZUL),
                new Resultado.TotalJuez(2, "B", 40, 36, Ganador.AZUL),
                new Resultado.TotalJuez(3, "C", 40, 36, Ganador.AZUL)), Ganador.AZUL, TipoDecision.UNANIME, null, null);
        assertEquals("Gana Azul por decisión unánime 3–0", ResultService.headline(p, unanime, Optional.empty()));

        var empate = new Resultado(p.id(), List.of(
                new Resultado.TotalJuez(1, "A", 38, 38, Ganador.EMPATE)), Ganador.EMPATE, TipoDecision.UNANIME, null,
                null);
        assertEquals("Empate", ResultService.headline(p, empate, Optional.empty()));

        var sinDecision = new Resultado(p.id(), List.of(), Ganador.SIN_DECISION, null, MotivoTerminacion.SIN_DECISION,
                null);
        assertEquals("Sin decisión · sin decisión", ResultService.headline(p, sinDecision, Optional.of(
                new Terminacion(p.id(), MotivoTerminacion.SIN_DECISION, null, null, 2, "0:30", null))));
    }
}
