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
import com.sigebox.central.server.SeatTokens;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
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

/** Spec 005 §Evento (DSK-040): la pelea que espera en Jueces baja jueces o cambia rounds antes del round 1. */
class FightCambioTest {

    @TempDir
    Path dir;

    private Database db;
    private SeatTokens tokens;
    private FightService fights;
    private LugarJuezRepository lugares;
    private String id;

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        tokens = new SeatTokens();
        fights = new FightService(db, tokens, () -> LocalDate.of(2026, 11, 6));
        lugares = new LugarJuezRepository(db);
        id = fights.create("Ana", "Bea", 10, 3).id();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private void lugar3(EstadoConexion conexion, Long aprobado) {
        lugares.save(new LugarJuez(id, 3, "Carla", "D-3", "abcd1234ef567890", conexion,
                aprobado == null ? null : "central", aprobado));
    }

    private FightService.Invitation invite(int seat) {
        return fights.invite(id, seat, "192.168.0.2", 8765);
    }

    @Test
    void bajarADosConElLugarTresSinJuezQuitaSuCodigo() {
        var dos = invite(2);
        var tres = invite(3);

        var p = fights.cambiar(id, null, 2);

        assertEquals(2, p.jueces());
        assertEquals(10, p.rounds());
        assertEquals(2, new PeleaRepository(db).find(id).orElseThrow().jueces());
        assertTrue(tokens.lookup(tres.code()).isEmpty(), "el código del lugar 3 ya no sirve");
        assertTrue(tokens.lookup(dos.code()).isPresent(), "los demás lugares conservan su código");
        assertEquals(2, fights.seats(id, List.of()).size());
    }

    @Test
    void conUnJuezEsperandoAprobacionEnElLugarQueSobraNoCambiaNadaYDiceQuien() {
        var tres = invite(3);
        lugar3(EstadoConexion.PENDIENTE, null);

        var e = assertThrows(IllegalStateException.class, () -> fights.cambiar(id, 8, 2));

        assertTrue(e.getMessage().contains("lugar 3") && e.getMessage().contains("Carla"), e.getMessage());
        var p = new PeleaRepository(db).find(id).orElseThrow();
        assertEquals(List.of(10, 3), List.of(p.rounds(), p.jueces()));
        assertTrue(lugares.find(id, 3).isPresent());
        assertTrue(tokens.lookup(tres.code()).isPresent(), "sin cambio, el código sigue como estaba");
    }

    @Test
    void unJuezAprobadoSinConexionTampocoSeSaca() {
        lugar3(EstadoConexion.DESCONECTADO, 1_000L);

        var e = assertThrows(IllegalStateException.class, () -> fights.cambiar(id, null, 1));

        assertTrue(e.getMessage().contains("lugar 3") && e.getMessage().contains("Carla"), e.getMessage());
        assertEquals(3, new PeleaRepository(db).find(id).orElseThrow().jueces());
    }

    @Test
    void unLugarRechazadoQueSobraSeQuita() {
        lugar3(EstadoConexion.RECHAZADO, null);

        fights.cambiar(id, null, 2);

        assertTrue(lugares.find(id, 3).isEmpty());
        assertEquals(2, new PeleaRepository(db).find(id).orElseThrow().jueces());
    }

    @Test
    void conElPrimerRoundAbiertoNoCambiaYDicePorQue() {
        new RoundRepository(db).save(new Round(id, 1, EstadoRound.ABIERTO, 1_000L, null, false));

        var e = assertThrows(IllegalStateException.class, () -> fights.cambiar(id, 8, null));

        assertTrue(e.getMessage().contains("round"), e.getMessage());
        assertEquals(10, new PeleaRepository(db).find(id).orElseThrow().rounds());
        assertFalse(fights.preparada(id));
    }

    @Test
    void unaPeleaTerminadaNoCambia() {
        new PeleaRepository(db).updateEstado(id, EstadoPelea.TERMINADA);

        assertThrows(IllegalStateException.class, () -> fights.cambiar(id, 8, null));
        assertFalse(fights.preparada(id));
    }

    @Test
    void susJuecesNoSuben() {
        var e = assertThrows(IllegalStateException.class, () -> fights.cambiar(id, null, 4));

        assertTrue(e.getMessage().contains("bajar"), e.getMessage());
        assertEquals(3, new PeleaRepository(db).find(id).orElseThrow().jueces());
    }

    @Test
    void losRoundsSubenOBajanYQuedaEnLaBitacora() {
        assertTrue(fights.preparada(id));

        assertEquals(12, fights.cambiar(id, 12, null).rounds());
        assertEquals(4, fights.cambiar(id, 4, null).rounds());

        var tipos = new EventLog(db).byPelea(id).stream().map(e -> e.tipo()).toList();
        assertEquals(2, tipos.stream().filter("CARTELERA_CAMBIADA"::equals).count());
    }

    @Test
    void fueraDeRangoNoCambiaNada() {
        assertThrows(IllegalArgumentException.class, () -> fights.cambiar(id, 13, null));
        assertThrows(IllegalArgumentException.class, () -> fights.cambiar(id, null, 0));
        assertEquals(10, new PeleaRepository(db).find(id).orElseThrow().rounds());
    }
}
