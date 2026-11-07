package com.sigebox.central.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.FuncionRepository;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.service.FuncionService.Estado;
import com.sigebox.central.service.FuncionService.Fila;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Spec 005 §Evento: bajar los jueces o los rounds del evento cambia la base y todas las peleas sin empezar; las
 * peleas que ya empezaron conservan lo que tenían.
 */
class FuncionCambioTest {

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

    private List<Integer> jueces(String id) {
        return funciones.cartelera(id).stream().map(Fila::jueces).toList();
    }

    private List<Integer> rounds(String id) {
        return funciones.cartelera(id).stream().map(Fila::rounds).toList();
    }

    @Test
    void bajarLosJuecesDelEventoCambiaLaBaseYTodasLasProgramadas() {
        var id = evento();
        funciones.cambiar(id, 3, null, 5); // una con su propio valor también sigue al evento

        funciones.cambiarEvento(id, null, 2);

        assertEquals(2, funciones.funcion(id).baseJueces());
        assertEquals(List.of(2, 2, 2), jueces(id));
        assertEquals(List.of(10, 10, 10), rounds(id));
    }

    @Test
    void bajarLosRoundsDelEventoNoTocaLosJueces() {
        var id = evento();
        funciones.cambiarEvento(id, 6, null);
        assertEquals(6, funciones.funcion(id).baseRounds());
        assertEquals(List.of(6, 6, 6), rounds(id));
        assertEquals(List.of(3, 3, 3), jueces(id));
    }

    @Test
    void unaPeleaQueYaEmpezoConservaSusJuecesYRounds() {
        var id = evento();
        var primera = funciones.siguiente(id); // la pelea 1 ya existe con 10 rounds y 3 jueces

        funciones.cambiarEvento(id, 4, 2);

        var filas = funciones.cartelera(id);
        assertEquals(Estado.EN_CURSO, filas.get(0).estado());
        assertEquals(10, filas.get(0).rounds());
        assertEquals(3, filas.get(0).jueces());
        assertEquals(List.of(4, 4), filas.subList(1, 3).stream().map(Fila::rounds).toList());
        assertEquals(List.of(2, 2), filas.subList(1, 3).stream().map(Fila::jueces).toList());
        assertEquals(3, primera.jueces());
    }

    @Test
    void elCambioQuedaEnLaBitacora() {
        var id = evento();
        int antes = new FuncionRepository(db).bitacora(id).size();
        funciones.cambiarEvento(id, null, 2);
        var tipos = new FuncionRepository(db).bitacora(id);
        assertEquals(antes + 1, tipos.size());
        assertTrue(tipos.get(tipos.size() - 1).equals("CARTELERA_CAMBIADA"), tipos.toString());
    }

    @Test
    void fueraDeRangoNoCambiaNada() {
        var id = evento();
        assertThrows(IllegalArgumentException.class, () -> funciones.cambiarEvento(id, 13, null));
        assertThrows(IllegalArgumentException.class, () -> funciones.cambiarEvento(id, null, 0));
        assertEquals(3, funciones.funcion(id).baseJueces());
        assertEquals(10, funciones.funcion(id).baseRounds());
    }
}
