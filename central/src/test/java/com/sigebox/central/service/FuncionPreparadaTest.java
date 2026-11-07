package com.sigebox.central.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.service.FuncionService.Estado;
import com.sigebox.central.service.FuncionService.Fila;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.Round;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Spec 005 §Evento (DSK-040): la pelea que ya espera en Jueces, sin round abierto, sigue los cambios del evento o
 * de su fila. Reemplaza a {@code FuncionCambioTest.unaPeleaQueYaEmpezoConservaSusJuecesYRounds} (decisión DSK-040).
 */
class FuncionPreparadaTest {

    @TempDir
    Path dir;

    private Database db;
    private FuncionService funciones;

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        funciones = new FuncionService(db, new FightService(db, new SeatTokens(), () -> LocalDate.of(2026, 11, 6)));
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private String evento() {
        return funciones.crear(10, 3, List.of(new Esquinas("Ana", "Bea"), new Esquinas("Caro", "Dani"),
                new Esquinas("Eli", "Fer"))).id();
    }

    private int juecesDe(String peleaId) {
        return new PeleaRepository(db).find(peleaId).orElseThrow().jueces();
    }

    private int roundsDe(String peleaId) {
        return new PeleaRepository(db).find(peleaId).orElseThrow().rounds();
    }

    @Test
    void unaPeleaConRoundAbiertoConservaSusJuecesYRounds() {
        var id = evento();
        var primera = funciones.siguiente(id);
        new RoundRepository(db).save(new Round(primera.id(), 1, EstadoRound.ABIERTO, 1_000L, null, false));

        funciones.cambiarEvento(id, 4, 2);

        var filas = funciones.cartelera(id);
        assertEquals(Estado.EN_CURSO, filas.get(0).estado());
        assertEquals(List.of(10, 3), List.of(filas.get(0).rounds(), filas.get(0).jueces()));
        assertEquals(List.of(4, 4), filas.subList(1, 3).stream().map(Fila::rounds).toList());
        assertEquals(List.of(2, 2), filas.subList(1, 3).stream().map(Fila::jueces).toList());
        assertEquals(List.of(10, 3), List.of(roundsDe(primera.id()), juecesDe(primera.id())));
        assertFalse(filas.get(0).preparada());
    }

    @Test
    void bajarLaBaseTambienBajaLaPeleaQueEsperaEnJueces() {
        var id = evento();
        var primera = funciones.siguiente(id);
        assertTrue(funciones.cartelera(id).get(0).preparada());

        funciones.cambiarEvento(id, 4, 2);

        var fila = funciones.cartelera(id).get(0);
        assertEquals(List.of(4, 2), List.of(fila.rounds(), fila.jueces()));
        assertFalse(fila.cambiada(), "sigue la base");
        assertEquals(List.of(4, 2), List.of(roundsDe(primera.id()), juecesDe(primera.id())));
    }

    @Test
    void subirLaBaseNoSubeLosJuecesDeLaPeleaEnJueces() {
        var id = evento();
        var primera = funciones.siguiente(id);

        funciones.cambiarEvento(id, 12, 5);

        var filas = funciones.cartelera(id);
        assertEquals(List.of(12, 3), List.of(filas.get(0).rounds(), filas.get(0).jueces()));
        assertEquals(List.of(12, 3), List.of(roundsDe(primera.id()), juecesDe(primera.id())));
        assertEquals(List.of(5, 5), filas.subList(1, 3).stream().map(Fila::jueces).toList());
    }

    @Test
    void laPeleaEnJuecesSoloCambiaDesdeLaBaseDelEvento() {
        var id = evento();
        var primera = funciones.siguiente(id);

        assertThrows(IllegalStateException.class, () -> funciones.cambiar(id, 1, null, 2));
        assertEquals(3, juecesDe(primera.id()));
    }

    @Test
    void siBajarDejariaFueraAUnJuezNoCambiaNadaDelEvento() {
        var id = evento();
        var primera = funciones.siguiente(id);
        new LugarJuezRepository(db).save(new LugarJuez(primera.id(), 3, "Carla", "D-3", "abcd1234ef567890",
                EstadoConexion.PENDIENTE, null, null));

        var e = assertThrows(IllegalStateException.class, () -> funciones.cambiarEvento(id, 4, 2));

        assertTrue(e.getMessage().contains("Carla"), e.getMessage());
        assertEquals(List.of(10, 3), List.of(funciones.funcion(id).baseRounds(), funciones.funcion(id).baseJueces()));
        assertEquals(List.of(10, 10, 10), funciones.cartelera(id).stream().map(Fila::rounds).toList());
        assertEquals(3, juecesDe(primera.id()));
    }

    @Test
    void laPeleaEnJuecesNoSeMueveNiSeQuita() {
        var id = evento();
        funciones.siguiente(id);

        assertThrows(IllegalStateException.class, () -> funciones.quitar(id, 1));
        assertThrows(IllegalStateException.class, () -> funciones.saltar(id, 1));
    }
}
