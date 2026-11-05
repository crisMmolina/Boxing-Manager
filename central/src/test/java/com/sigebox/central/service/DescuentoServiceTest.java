package com.sigebox.central.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.server.ServerEvents;
import com.sigebox.central.ui.Labels;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EventoBitacora;
import com.sigebox.core.model.Pelea;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Descuentos de la central (ADR 0005): sin límite, anular marca con motivo y todo queda en bitácora. */
class DescuentoServiceTest {

    private static final String F = "pelea-1";

    @TempDir
    Path dir;

    private Database db;
    private DescuentoService service;
    private final List<String> avisos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        pelea(EstadoPelea.EN_CURSO);
        service = new DescuentoService(db, new ServerEvents() {
            @Override
            public void onDescuentoChanged(String fightId) {
                avisos.add(fightId);
            }
        }, () -> 5_000L);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private void pelea(EstadoPelea estado) {
        new PeleaRepository(db).save(new Pelea(F, "Rojo", "Azul", 3, 2, estado, "2026-11-04"));
    }

    private List<EventoBitacora> eventos(String tipo) {
        return new EventLog(db).byPelea(F).stream().filter(e -> e.tipo().equals(tipo)).toList();
    }

    @Test
    void ponerGuardaUnPuntoYAvisa() {
        long id = service.poner(F, 2, Esquina.ROJA);

        var guardados = service.listar(F);
        assertEquals(1, guardados.size());
        var d = guardados.get(0);
        assertEquals(id, d.id());
        assertEquals(2, d.round());
        assertEquals(Esquina.ROJA, d.esquina());
        assertEquals(1, d.puntos());
        assertFalse(d.anulado());
        assertEquals(List.of(F), avisos);
    }

    @Test
    void noHayLimiteDeVecesEnElMismoRound() {
        service.poner(F, 1, Esquina.AZUL);
        service.poner(F, 1, Esquina.AZUL);
        service.poner(F, 1, Esquina.AZUL);

        assertEquals(3, service.listar(F).size());
        assertEquals(3, eventos("DESCUENTO").size());
    }

    @Test
    void ponerQuedaEnBitacoraEnEspanol() {
        service.poner(F, 2, Esquina.ROJA);

        var e = eventos("DESCUENTO").get(0);
        assertEquals("Punto descontado · round 2 · roja", Labels.linea(e.tipo(), e.detalle()));
    }

    @Test
    void anularMarcaSinBorrarYQuedaEnBitacora() {
        long id = service.poner(F, 2, Esquina.AZUL);

        service.anular(id, "error del operador");

        var d = service.listar(F).get(0);
        assertTrue(d.anulado());
        assertEquals("error del operador", d.motivoAnulacion());
        var e = eventos("DESCUENTO_ANULADO").get(0);
        assertEquals("Descuento anulado · round 2 · azul · error del operador", Labels.linea(e.tipo(), e.detalle()));
        assertEquals(List.of(F, F), avisos);
    }

    @Test
    void anularSinMotivoSeRechaza() {
        long id = service.poner(F, 1, Esquina.ROJA);

        assertThrows(IllegalArgumentException.class, () -> service.anular(id, "   "));
        assertThrows(IllegalArgumentException.class, () -> service.anular(id, null));

        assertFalse(service.listar(F).get(0).anulado());
        assertTrue(eventos("DESCUENTO_ANULADO").isEmpty());
    }

    @Test
    void noSeAnulaDosVeces() {
        long id = service.poner(F, 1, Esquina.ROJA);
        service.anular(id, "primero");

        assertThrows(IllegalStateException.class, () -> service.anular(id, "segundo"));

        assertEquals("primero", service.listar(F).get(0).motivoAnulacion());
        assertEquals(1, eventos("DESCUENTO_ANULADO").size());
    }

    @Test
    void anularUnDescuentoDesconocidoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> service.anular(99L, "motivo"));
    }

    @Test
    void conLaPeleaTerminadaNoSePoneNiSeAnula() {
        long id = service.poner(F, 1, Esquina.ROJA);
        pelea(EstadoPelea.TERMINADA);

        assertFalse(service.abierta(F));
        assertThrows(IllegalStateException.class, () -> service.poner(F, 1, Esquina.ROJA));
        assertThrows(IllegalStateException.class, () -> service.anular(id, "tarde"));
        assertEquals(1, service.listar(F).size());
    }

    @Test
    void conLaPeleaCerradaNoSePone() {
        pelea(EstadoPelea.CERRADA);

        assertFalse(service.abierta(F));
        assertThrows(IllegalStateException.class, () -> service.poner(F, 1, Esquina.AZUL));
    }

    @Test
    void roundFueraDeRangoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> service.poner(F, 0, Esquina.ROJA));
        assertThrows(IllegalArgumentException.class, () -> service.poner(F, 4, Esquina.ROJA));

        assertTrue(service.listar(F).isEmpty());
        assertTrue(avisos.isEmpty());
    }

    @Test
    void peleaDesconocidaOSinEsquinaSeRechaza() {
        assertFalse(service.abierta("otra"));
        assertThrows(IllegalArgumentException.class, () -> service.poner("otra", 1, Esquina.ROJA));
        assertThrows(IllegalArgumentException.class, () -> service.poner(F, 1, null));
    }
}
