package com.sigebox.central.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.Pelea;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DescuentoRepositoryTest {

    private static final String F = "pelea-1";

    @TempDir
    Path dir;

    private Database db;
    private DescuentoRepository repo;

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        var peleas = new PeleaRepository(db);
        peleas.save(new Pelea(F, "Rojo", "Azul", 3, 2, EstadoPelea.EN_CURSO, "2026-11-04"));
        peleas.save(new Pelea("pelea-2", "Otro", "Más", 3, 2, EstadoPelea.EN_CURSO, "2026-11-04"));
        repo = new DescuentoRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void guardaYEncuentraPorId() {
        long id = repo.save(F, 2, Esquina.AZUL, 1_000L);

        var d = repo.find(id).orElseThrow();
        assertEquals(F, d.fightId());
        assertEquals(2, d.round());
        assertEquals(Esquina.AZUL, d.esquina());
        assertEquals(1, d.puntos());
        assertFalse(d.anulado());
    }

    @Test
    void unIdQueNoExisteNoSeEncuentra() {
        assertTrue(repo.find(99L).isEmpty());
    }

    @Test
    void listaSoloLosDeLaPeleaEnOrden() {
        long a = repo.save(F, 1, Esquina.ROJA, 1_000L);
        repo.save("pelea-2", 1, Esquina.ROJA, 1_500L);
        long b = repo.save(F, 1, Esquina.ROJA, 2_000L);

        var ids = repo.byPelea(F).stream().map(d -> d.id()).toList();
        assertEquals(java.util.List.of(a, b), ids);
    }

    @Test
    void anularConservaElRenglonConSuMotivo() {
        long id = repo.save(F, 1, Esquina.ROJA, 1_000L);

        repo.anular(id, "error", 2_000L);

        var d = repo.find(id).orElseThrow();
        assertTrue(d.anulado());
        assertEquals("error", d.motivoAnulacion());
        assertEquals(1, repo.byPelea(F).size());
    }
}
