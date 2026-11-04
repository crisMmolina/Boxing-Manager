package com.sigebox.central.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.FuncionRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.core.model.EstadoPelea;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Spec 002: la función guarda la cartelera y crea las peleas en orden con la base o sus cambios. */
class FuncionServiceTest {

    @TempDir
    Path dir;

    private Database db;
    private FuncionService funciones;

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        funciones = service(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private static FuncionService service(Database db) {
        return new FuncionService(db, new FightService(db, new SeatTokens(), () -> LocalDate.of(2026, 10, 26)));
    }

    private static FuncionService.Esquinas pelea(String roja, String azul) {
        return new FuncionService.Esquinas(roja, azul);
    }

    private void cerrar(String peleaId) {
        new PeleaRepository(db).updateEstado(peleaId, EstadoPelea.CERRADA);
    }

    @Test
    void siguienteCreaLasPeleasEnOrdenConLaBaseOSusCambios() {
        var f = funciones.crear(4, 3, List.of(pelea("Ana", "Bea"), pelea("Caro", "Dani"), pelea("Eli", "Fer")));
        funciones.cambiar(f.id(), 3, 6, null);

        var p1 = funciones.siguiente(f.id());
        assertEquals(List.of("Ana", "Bea", 4, 3), List.of(p1.esquinaRoja(), p1.esquinaAzul(), p1.rounds(), p1.jueces()));
        assertThrows(IllegalStateException.class, () -> funciones.siguiente(f.id()), "una sola pelea activa");
        cerrar(p1.id());

        var p2 = funciones.siguiente(f.id());
        assertEquals(List.of("Caro", 4, 3), List.of(p2.esquinaRoja(), p2.rounds(), p2.jueces()));
        cerrar(p2.id());

        var p3 = funciones.siguiente(f.id());
        assertEquals(List.of("Eli", 6, 3), List.of(p3.esquinaRoja(), p3.rounds(), p3.jueces()));
        cerrar(p3.id());

        assertThrows(IllegalStateException.class, () -> funciones.siguiente(f.id()), "ya no quedan peleas");
        assertEquals(List.of(FuncionService.Estado.CERRADA, FuncionService.Estado.CERRADA, FuncionService.Estado.CERRADA),
                funciones.cartelera(f.id()).stream().map(FuncionService.Fila::estado).toList());
    }

    @Test
    void agregarQuitarReordenarYSaltarSoloAntesDeEmpezar() {
        var f = funciones.crear(4, 3, List.of(pelea("Ana", "Bea"), pelea("Caro", "Dani")));
        funciones.agregar(f.id(), pelea("Eli", "Fer"));
        funciones.mover(f.id(), 3, 1); // Eli vs Fer pasa al principio
        funciones.quitar(f.id(), 3); // quita Caro vs Dani, que quedó tercera
        assertEquals(List.of("Eli", "Ana"), funciones.cartelera(f.id()).stream().map(FuncionService.Fila::roja).toList());

        funciones.saltar(f.id(), 1);
        var p = funciones.siguiente(f.id());
        assertEquals("Ana", p.esquinaRoja());
        assertEquals(List.of(FuncionService.Estado.SALTADA, FuncionService.Estado.EN_CURSO),
                funciones.cartelera(f.id()).stream().map(FuncionService.Fila::estado).toList());

        // La pelea ya empezó: no se cambia ni se quita.
        assertThrows(IllegalStateException.class, () -> funciones.cambiar(f.id(), 2, 6, null));
        assertThrows(IllegalStateException.class, () -> funciones.quitar(f.id(), 2));
        assertThrows(IllegalStateException.class, () -> funciones.saltar(f.id(), 2));
    }

    @Test
    void validaRangosYNombres() {
        assertThrows(IllegalArgumentException.class, () -> funciones.crear(0, 3, List.of()));
        assertThrows(IllegalArgumentException.class, () -> funciones.crear(4, 6, List.of()));
        var f = funciones.crear(4, 3, List.of());
        assertThrows(IllegalArgumentException.class, () -> funciones.agregar(f.id(), pelea(" ", "Bea")));
        funciones.agregar(f.id(), pelea("Ana", "Bea"));
        assertThrows(IllegalArgumentException.class, () -> funciones.cambiar(f.id(), 1, 13, null));
        assertThrows(IllegalArgumentException.class, () -> funciones.cambiar(f.id(), 1, null, 0));
        assertThrows(IllegalArgumentException.class, () -> funciones.quitar(f.id(), 2));
    }

    @Test
    void laCarteleraSobreviveAUnCierreForzadoYQuedaEnLaBitacora() {
        var f = funciones.crear(4, 3, List.of(pelea("Ana", "Bea"), pelea("Caro", "Dani")));
        funciones.cambiar(f.id(), 2, null, 5);
        var p1 = funciones.siguiente(f.id());
        db.close(); // la central se cierra a la fuerza

        db = Database.open(dir.resolve("sigebox.db"));
        funciones = service(db);
        assertEquals(f, funciones.funcion(f.id()));
        var filas = funciones.cartelera(f.id());
        assertEquals(List.of(FuncionService.Estado.EN_CURSO, FuncionService.Estado.PROGRAMADA),
                filas.stream().map(FuncionService.Fila::estado).toList());
        assertEquals(p1.id(), filas.get(0).peleaId());
        assertEquals(List.of(4, 4), filas.stream().map(FuncionService.Fila::rounds).toList());
        assertEquals(List.of(3, 5), filas.stream().map(FuncionService.Fila::jueces).toList());

        var tipos = new FuncionRepository(db).bitacora(f.id());
        assertTrue(tipos.containsAll(List.of("FUNCION_CREADA", "CARTELERA_CAMBIADA", "SIGUIENTE_PELEA")), tipos.toString());
    }

    @Test
    void siguienteCopiaSoloLosLugaresAprobadosHastaElNumeroDeJuecesYLoAvisa() {
        var f = funciones.crear(3, 3, List.of(pelea("Ana", "Bea"), pelea("Caro", "Dani")));
        funciones.cambiar(f.id(), 2, null, 2);
        var p1 = funciones.siguiente(f.id());
        var lugares = new com.sigebox.central.db.LugarJuezRepository(db);
        for (int n = 1; n <= 3; n++) {
            lugares.save(new com.sigebox.core.model.LugarJuez(p1.id(), n, "Juez " + n, "dev-" + n, "huella-" + n,
                    com.sigebox.core.model.EstadoConexion.CONECTADO, "central", n == 2 ? null : 5L));
            lugares.savePublicKey(p1.id(), n, "key-" + n);
        }
        cerrar(p1.id());
        var avisos = new java.util.ArrayList<String>();
        funciones.alSiguiente((anterior, nueva) -> avisos.add(anterior + ">" + nueva));

        var p2 = funciones.siguiente(f.id());

        assertEquals(List.of(p1.id() + ">" + p2.id()), avisos);
        var copiados = lugares.byPelea(p2.id());
        assertEquals(1, copiados.size(), "el 2 no estaba aprobado y el 3 sobra");
        assertEquals("Juez 1", copiados.get(0).nombre());
        assertEquals("huella-1", copiados.get(0).huellaCredencial());
        assertEquals("key-1", lugares.publicKey(p2.id(), 1).orElseThrow());
        var log = new com.sigebox.central.db.EventLog(db);
        assertTrue(log.byPelea(p1.id()).stream().anyMatch(e -> e.tipo().equals("SIGUIENTE_PELEA")));
        assertTrue(log.byPelea(p2.id()).stream().anyMatch(e -> e.tipo().equals("SIGUIENTE_PELEA")));
    }
}
