package com.sigebox.central.ui.inicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.ui.terms.TermsScreen;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.central.ui.theme.StepsBar;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Test;

/**
 * Spec 005 §Inicio y navegación: fuera de un evento la barra solo lleva logo, "?" y tema; dentro de un evento lleva
 * los pasos y los enlaces; en una pantalla fuera del evento que no es Inicio queda el enlace "Inicio" para volver.
 */
class NavModeTest {

    private static List<Node> all(Node node) {
        var out = new ArrayList<Node>();
        out.add(node);
        if (node instanceof Parent p) {
            p.getChildrenUnmodifiable().forEach(c -> out.addAll(all(c)));
        }
        return out;
    }

    /** Textos de los botones que se ven (el nodo y todos sus padres visibles). */
    private static List<String> visibleButtons(StepsBar bar) {
        return all(bar).stream().filter(n -> n instanceof Button && shown(n, bar))
                .map(n -> ((Button) n).getText()).toList();
    }

    private static boolean shown(Node n, Node root) {
        for (var x = n; x != null; x = x.getParent()) {
            if (!x.isVisible() || !x.isManaged()) {
                return false;
            }
            if (x == root) {
                return true;
            }
        }
        return true;
    }

    private static StepsBar bar() {
        return new StepsBar(Nav.STEPS, Nav.LINKS, id -> { });
    }

    @Test
    void inicioNuevoEventoCarteleraYTerminosQuedanFueraDelEvento() {
        for (var id : List.of(InicioScreen.ID, NuevoEventoScreen.ID, CarteleraScreen.ID, TermsScreen.ID)) {
            assertFalse(Nav.inEvent(id), id);
        }
    }

    @Test
    void cadaPasoYCadaEnlaceMenosInicioEstanDentroDelEvento() {
        Nav.STEPS.forEach(i -> assertTrue(Nav.inEvent(i.id()), i.id()));
        Nav.LINKS.stream().filter(i -> !InicioScreen.ID.equals(i.id()))
                .forEach(i -> assertTrue(Nav.inEvent(i.id()), i.id()));
    }

    @Test
    void enInicioLaBarraSoloTieneAyudaYTema() throws Exception {
        var texts = FxTest.onFx(() -> {
            var bar = bar();
            Nav.apply(bar, InicioScreen.ID);
            return visibleButtons(bar);
        });
        assertEquals(2, texts.size(), texts.toString());
        assertTrue(texts.contains("?"), texts.toString());
    }

    @Test
    void fueraDelEventoPeroNoEnInicioQuedaElEnlaceInicio() throws Exception {
        var texts = FxTest.onFx(() -> {
            var bar = bar();
            Nav.apply(bar, NuevoEventoScreen.ID);
            return visibleButtons(bar);
        });
        assertEquals(3, texts.size(), texts.toString());
        assertTrue(texts.contains("Inicio"), texts.toString());
        assertTrue(texts.contains("?"), texts.toString());
    }

    @Test
    void dentroDelEventoSeVenLosPasosLosEnlacesAyudaYTema() throws Exception {
        var texts = FxTest.onFx(() -> {
            var bar = bar();
            Nav.apply(bar, Nav.JUECES);
            return visibleButtons(bar);
        });
        var expected = new ArrayList<String>();
        Nav.STEPS.forEach(i -> expected.add(i.label()));
        Nav.LINKS.forEach(i -> expected.add(i.label()));
        assertTrue(texts.containsAll(expected), texts.toString());
        assertTrue(texts.contains("?"), texts.toString());
        assertEquals(expected.size() + 2, texts.size(), texts.toString());
    }

    @Test
    void volverAInicioQuitaLaBarraDelEvento() throws Exception {
        var texts = FxTest.onFx(() -> {
            var bar = bar();
            Nav.apply(bar, Nav.JUECES);
            Nav.apply(bar, InicioScreen.ID);
            return visibleButtons(bar);
        });
        assertEquals(2, texts.size(), texts.toString());
    }
}
