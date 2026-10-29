package com.sigebox.central.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.db.TarjetaRoundRepository;
import com.sigebox.central.db.VersionTarjetaRepository;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.EventoBitacora;
import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaRound;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CorrectionServiceTest {

    private static final String F = "pelea-1";

    @TempDir
    Path dir;

    private Database db;
    private CorrectionService service;

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        new PeleaRepository(db).save(new Pelea(F, "Rojo", "Azul", 3, 2, EstadoPelea.EN_CURSO, "2026-10-25"));
        new RoundRepository(db).save(new Round(F, 1, EstadoRound.CERRADO, 1L, 2L, true));
        new TarjetaRoundRepository(db).save(
                new TarjetaRound(F, 1, 1, 10, 9, EstadoTarjeta.ENVIADA, OrigenTarjeta.JUEZ, 1L));
        service = new CorrectionService(db, () -> 50L);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void correctionNeedsReasonAndCreatesAVersion() {
        assertThrows(IllegalArgumentException.class, () -> service.correct(F, 1, 1, 10, 8, null));
        assertThrows(IllegalArgumentException.class, () -> service.correct(F, 1, 1, 10, 8, "  "));

        var card = service.correct(F, 1, 1, 10, 8, "Coincide con el papel del juez");
        assertEquals(EstadoTarjeta.CORREGIDA, card.estado());
        assertEquals(OrigenTarjeta.ORGANIZADOR, card.origen());
        assertEquals(card, new TarjetaRoundRepository(db).find(F, 1, 1).orElseThrow());

        var versions = new VersionTarjetaRepository(db).of(F, 1, 1);
        assertEquals(1, versions.size());
        assertEquals(9, versions.get(0).azulAnterior());
        assertEquals(8, versions.get(0).azulNuevo());
        assertEquals(OrigenTarjeta.ORGANIZADOR, versions.get(0).quien());
        assertEquals("Coincide con el papel del juez", versions.get(0).motivo());
        assertTrue(new EventLog(db).byPelea(F).stream().map(EventoBitacora::tipo).toList().contains("CARD_CORRECTED"));
    }

    @Test
    void captureFromPaperWhenTheCardNeverArrived() {
        assertThrows(IllegalStateException.class, () -> service.correct(F, 2, 1, 9, 10, "papel")); // no existe
        var card = service.capture(F, 2, 1, 9, 10, "Teléfono del juez 2 sin batería");
        assertEquals(EstadoTarjeta.CAPTURADA, card.estado());
        var versions = new VersionTarjetaRepository(db).of(F, 2, 1);
        assertNull(versions.get(0).rojoAnterior());
        assertTrue(new EventLog(db).byPelea(F).stream().map(EventoBitacora::tipo).toList().contains("CARD_CAPTURED"));

        assertThrows(IllegalStateException.class, () -> service.capture(F, 2, 1, 9, 10, "otra vez")); // ya existe
        assertThrows(IllegalStateException.class, () -> service.capture(F, 2, 2, 9, 10, "papel")); // round sin abrir
    }

    @Test
    void rejectsInvalidValuesSameValuesAndClosedFights() {
        assertThrows(IllegalArgumentException.class, () -> service.correct(F, 1, 1, 11, 9, "papel"));
        assertThrows(IllegalArgumentException.class, () -> service.capture(F, 3, 1, 10, 9, "papel")); // 2 jueces
        assertThrows(IllegalArgumentException.class, () -> service.correct(F, 1, 1, 10, 9, "papel")); // sin cambio
        new PeleaRepository(db).updateEstado(F, EstadoPelea.CERRADA);
        assertThrows(IllegalStateException.class, () -> service.correct(F, 1, 1, 10, 8, "papel"));
        assertEquals(9, new TarjetaRoundRepository(db).find(F, 1, 1).orElseThrow().azul());
    }
}
