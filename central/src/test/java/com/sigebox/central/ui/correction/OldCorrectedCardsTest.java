package com.sigebox.central.ui.correction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.db.TarjetaRoundRepository;
import com.sigebox.central.export.ExportService;
import com.sigebox.central.service.CorrectionService;
import com.sigebox.central.service.ResultService;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaRound;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Sin la pantalla de Corrección, las tarjetas {@code CAPTURADA} y {@code CORREGIDA} que dejaron peleas viejas
 * siguen contando en el resultado y en el acta.
 */
class OldCorrectedCardsTest {

    private static final String F = "pelea-vieja";

    @TempDir
    Path dir;

    private Database db;

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        new PeleaRepository(db).save(new Pelea(F, "Rojo", "Azul", 2, 1, EstadoPelea.EN_CURSO, "2026-10-01"));
        new LugarJuezRepository(db).save(new LugarJuez(F, 1, "Ana", "A", "fp", EstadoConexion.CONECTADO,
                "central", 1L));
        var rounds = new RoundRepository(db);
        rounds.save(new Round(F, 1, EstadoRound.ABIERTO, 1L, null, false));
        rounds.save(new Round(F, 2, EstadoRound.ABIERTO, 1L, null, false));
        var correcciones = new CorrectionService(db, () -> 2_000L);
        // Round 1 capturado desde papel; round 2 llegó 9–10 y se corrigió a 10–8.
        correcciones.capture(F, 1, 1, 10, 9, "teléfono sin batería");
        new TarjetaRoundRepository(db).save(new TarjetaRound(F, 1, 2, 9, 10, EstadoTarjeta.ENVIADA,
                OrigenTarjeta.JUEZ, 1L));
        correcciones.correct(F, 1, 2, 10, 8, "coincide con el papel del juez");
        rounds.save(new Round(F, 1, EstadoRound.CERRADO, 1L, 2L, false));
        rounds.save(new Round(F, 2, EstadoRound.CERRADO, 1L, 2L, false));
        new PeleaRepository(db).updateEstado(F, EstadoPelea.TERMINADA);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void capturedAndCorrectedCardsStillCountInTheResultAndTheActa() {
        var cards = new TarjetaRoundRepository(db).byPelea(F);
        assertTrue(cards.stream().anyMatch(c -> c.estado() == EstadoTarjeta.CAPTURADA));
        assertTrue(cards.stream().anyMatch(c -> c.estado() == EstadoTarjeta.CORREGIDA));
        var results = new ResultService(db, new ResultService.Broadcaster() {
            @Override
            public void fightEnded(String fightId, MotivoTerminacion reason) {
            }

            @Override
            public void fightClosed(String fightId) {
            }
        });
        var r = results.compute(F);
        assertEquals(1, r.totales().size());
        assertEquals(20, r.totales().get(0).rojo());
        assertEquals(17, r.totales().get(0).azul());
        var csv = new ExportService().csv(results.sheet(F));
        assertTrue(csv.contains("20") && csv.contains("17"), csv);
    }
}
