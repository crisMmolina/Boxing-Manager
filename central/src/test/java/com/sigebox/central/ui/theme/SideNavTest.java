package com.sigebox.central.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.service.FuncionService.Estado;
import com.sigebox.central.service.FuncionService.Fila;
import com.sigebox.central.ui.inicio.Nav;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SideNavTest {

    @AfterEach
    void reset() {
        Theme.setReducedMotion(null);
    }

    private static SideNav nav() {
        return new SideNav(Nav.STEPS, Nav.LINKS, "rounds", id -> { });
    }

    @Test
    void cadaPasoLlevaIcono() throws Exception {
        FxTest.onFx(() -> {
            var n = nav();
            for (var id : n.stepIds()) {
                assertTrue(n.hasIcon(id), id);
            }
            return null;
        });
    }

    @Test
    void avanceDelEventoSeMuestraComoNDeM() throws Exception {
        FxTest.onFx(() -> {
            var n = nav();
            n.showEvent(true, null);
            n.showProgress("Copa Norte", 2, 5);
            assertEquals("2 de 5", n.progressText());
            n.showProgress(null, 0, 0);
            assertEquals("", n.progressText());
            return null;
        });
    }

    @Test
    void pasosTerminadosLlevanCheck() throws Exception {
        FxTest.onFx(() -> {
            var n = nav();
            n.markDone(List.of("funcion", "jueces"));
            assertTrue(n.isDone("funcion"));
            assertTrue(n.isDone("jueces"));
            assertFalse(n.isDone("rounds"));
            n.markDone(List.of());
            assertFalse(n.isDone("funcion"));
            return null;
        });
    }

    @Test
    void enVivoSinLatidoConMovimientoReducido() throws Exception {
        Theme.setReducedMotion(true);
        FxTest.onFx(() -> {
            var n = nav();
            n.showEvent(true, null);
            n.setLive(true);
            assertTrue(n.isLive());
            assertFalse(n.isPulsing());
            n.setLive(false);
            assertFalse(n.isLive());
            return null;
        });
    }

    @Test
    void enVivoLateConMovimiento() throws Exception {
        Theme.setReducedMotion(false);
        FxTest.onFx(() -> {
            var n = nav();
            n.showEvent(true, null);
            n.setLive(true);
            assertTrue(n.isPulsing());
            n.setLive(false);
            assertFalse(n.isPulsing());
            return null;
        });
    }

    @Test
    void fueraDelEventoOcultaPasosYEnVivo() throws Exception {
        Theme.setReducedMotion(true);
        FxTest.onFx(() -> {
            var n = nav();
            n.showEvent(true, null);
            n.setLive(true);
            n.showEvent(false, "bitacora");
            assertFalse(n.isLive());
            n.select("rounds");
            assertEquals("rounds", n.currentId());
            n.select("nuevo-evento");
            assertNull(n.currentId());
            return null;
        });
    }

    @Test
    void navApplyFuncionaConLaBarraLateral() throws Exception {
        FxTest.onFx(() -> {
            var n = nav();
            Nav.apply(n, "jueces");
            assertEquals("jueces", n.currentId());
            return null;
        });
    }

    @Test
    void progresoTomaLaPeleaEnCursoOLaSiguiente() {
        var f = List.of(fila(1, Estado.CERRADA), fila(2, Estado.EN_CURSO), fila(3, Estado.PROGRAMADA));
        assertEquals(List.of(2, 3), List.of(Nav.progress(f)[0], Nav.progress(f)[1]));
        var g = List.of(fila(1, Estado.CERRADA), fila(2, Estado.SALTADA), fila(3, Estado.PROGRAMADA));
        assertEquals(3, Nav.progress(g)[0]);
        var h = List.of(fila(1, Estado.CERRADA), fila(2, Estado.CERRADA));
        assertEquals(2, Nav.progress(h)[0]);
        assertEquals(0, Nav.progress(List.of())[1]);
    }

    @Test
    void pasosAnterioresAlActualQuedanHechos() {
        assertEquals(List.of("funcion", "jueces"), Nav.doneBefore("rounds"));
        assertEquals(List.of(), Nav.doneBefore("inicio"));
    }

    @Test
    void swapCambiaLaPantallaConEntrada() throws Exception {
        Theme.setReducedMotion(true);
        FxTest.onFx(() -> {
            var content = new StackPane(new Button("a"));
            var next = new Button("b");
            var anim = Nav.swap(content, next);
            assertEquals(List.of(next), content.getChildren());
            assertTrue(anim != null);
            return null;
        });
    }

    @Test
    void encabezadoConContextoTituloYAccion() throws Exception {
        FxTest.onFx(() -> {
            var h = new PageHeader("Tablero");
            assertEquals("", h.contextText());
            h.fight("Pérez", "López");
            assertEquals("Pérez vs López", h.contextText());
            assertEquals("Tablero", h.titleText());
            var b = Ui.primary("Abrir round");
            h.action(b);
            assertEquals(b, h.actionNode());
            h.action(null);
            assertNull(h.actionNode());
            return null;
        });
    }

    private static Fila fila(int orden, Estado e) {
        return new Fila(orden, "R" + orden, "A" + orden, 3, 3, false, e, null);
    }
}
