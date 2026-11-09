package com.sigebox.central.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.db.TerminacionRepository;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Round;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Spec 005 §En vivo y terminación (DSK-036): terminar desde En vivo toma solo el motivo; el round y el tiempo los pone
 * la central y la pelea queda cerrada con su resultado, sin pulsar "Cerrar pelea".
 */
class TerminationAutoCloseTest {

    private static final String F = "pelea-1";

    @TempDir
    Path dir;

    private Database db;
    private RoundRepository rounds;
    private TerminationService terminaciones;
    private ResultService resultados;
    private final AtomicLong now = new AtomicLong(1_000_000L);
    private final List<String> avisos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        new PeleaRepository(db).save(new Pelea(F, "Rojo", "Azul", 4, 3, EstadoPelea.EN_CURSO, "2026-11-09"));
        rounds = new RoundRepository(db);
        terminaciones = new TerminationService(db, (f, m) -> avisos.add("FIGHT_ENDED " + m), now::get);
        resultados = new ResultService(db, new ResultService.Broadcaster() {
            @Override
            public void fightEnded(String fightId, MotivoTerminacion reason) {
                avisos.add("FIGHT_ENDED " + reason);
            }

            @Override
            public void fightClosed(String fightId) {
                avisos.add("FIGHT_CLOSED");
            }
        });
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void conUnRoundAbiertoElPuntoEsEseRoundConElTiempoQueLleva() {
        rounds.save(new Round(F, 1, EstadoRound.CERRADO, 1L, 2L, false));
        rounds.save(new Round(F, 2, EstadoRound.ABIERTO, now.get() - 84_000L, null, false));

        assertEquals(new TerminationService.Punto(2, "1:24"), terminaciones.punto(F));
    }

    @Test
    void entreRoundsElPuntoEsElSiguienteAlUltimoCerradoACero() {
        rounds.save(new Round(F, 1, EstadoRound.CERRADO, 1L, 2L, false));
        rounds.save(new Round(F, 2, EstadoRound.CERRADO, 3L, 4L, false));

        assertEquals(new TerminationService.Punto(3, "0:00"), terminaciones.punto(F));
    }

    @Test
    void antesDelPrimerRoundElPuntoEsElRoundUnoACero() {
        assertEquals(new TerminationService.Punto(1, "0:00"), terminaciones.punto(F));
    }

    @Test
    void terminarDesdeEnVivoCierraLaPeleaConSuResultadoSinMasPasos() {
        rounds.save(new Round(F, 1, EstadoRound.ABIERTO, now.get() - 30_000L, null, false));
        assertTrue(terminaciones.sePuedeTerminar(F));

        var resultado = terminaciones.terminarYCerrar(F, MotivoTerminacion.KO, null, Esquina.ROJA, resultados);

        assertEquals(EstadoPelea.CERRADA, new PeleaRepository(db).find(F).orElseThrow().estado());
        assertTrue(resultados.stored(F).isPresent(), "el resultado queda guardado");
        assertEquals(resultado, resultados.stored(F).orElseThrow());
        var t = new TerminacionRepository(db).find(F).orElseThrow();
        assertEquals(List.of(1, "0:30"), List.of(t.round(), t.tiempo()));
        assertEquals(List.of("FIGHT_ENDED KO", "FIGHT_CLOSED"), avisos);
        assertFalse(terminaciones.sePuedeTerminar(F));
    }

    @Test
    void sinMotivoNoTerminaNiCierra() {
        assertThrows(IllegalArgumentException.class,
                () -> terminaciones.terminarYCerrar(F, null, null, null, resultados));
        assertEquals(EstadoPelea.EN_CURSO, new PeleaRepository(db).find(F).orElseThrow().estado());
        assertTrue(avisos.isEmpty());
    }

    @Test
    void unaPeleaQueNoEstaEnCursoNoSeTermina() {
        new PeleaRepository(db).updateEstado(F, EstadoPelea.CREADA);
        assertFalse(terminaciones.sePuedeTerminar(F));
    }
}
