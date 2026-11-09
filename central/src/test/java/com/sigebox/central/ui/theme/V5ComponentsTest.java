package com.sigebox.central.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Shape;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Spec 006 · DSK-041: íconos, stepper, menú ⋯, aviso, chip, avatar y movimiento de la central v5 (DESIGN v5). */
class V5ComponentsTest {

    @BeforeAll
    static void fx() throws Exception {
        FxTest.start();
    }

    @AfterEach
    void motionFollowsTheSystemAgain() {
        Theme.setReducedMotion(null);
    }

    private static List<Node> all(Node node) {
        var out = new ArrayList<Node>();
        out.add(node);
        if (node instanceof Parent p) {
            p.getChildrenUnmodifiable().forEach(c -> out.addAll(all(c)));
        }
        return out;
    }

    // ---- Íconos ----

    @Test
    void cadaIconoDeLaGuiaSeDibujaConTrazoDeDosYSinRelleno() throws Exception {
        for (var name : Icons.NAMES) {
            var icon = FxTest.onFx(() -> Icons.icon(name, 20));
            assertEquals(20, icon.getPrefWidth(), name);
            assertEquals(20, icon.getPrefHeight(), name);
            assertTrue(icon.getStyleClass().contains("icon"), name);
            var shapes = all(icon).stream().filter(n -> n instanceof Shape).map(Shape.class::cast).toList();
            assertFalse(shapes.isEmpty(), name);
            for (var s : shapes) {
                assertEquals(2.0, s.getStrokeWidth(), name);
                assertNull(s.getFill(), name);
                assertTrue(s.getStyleClass().contains("icon-shape"), name);
            }
        }
    }

    @Test
    void losIconosDeLaGuiaEstanTodos() {
        for (var name : List.of("calendar", "users", "activity", "trophy", "file-text", "house", "sun", "moon",
                "circle-question-mark", "info", "check", "x", "plus", "minus", "ellipsis", "play", "clock",
                "rotate-ccw", "circle-alert", "grip-vertical", "pencil", "skip-forward", "trash", "arrow-up",
                "arrow-down", "qr-code", "wifi", "wifi-off", "arrow-right")) {
            assertTrue(Icons.NAMES.contains(name), name);
        }
    }

    @Test
    void unIconoQueNoExisteSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> Icons.icon("no-existe", 16));
    }

    // ---- Stepper ----

    @Test
    void elStepperQuedaDentroDeSuRangoYApagaElBotonDelLimite() throws Exception {
        var s = FxTest.onFx(() -> new Stepper(1, 5, 3));
        FxTest.onFx(() -> {
            s.increment();
            s.increment();
            s.increment();
            return null;
        });
        assertEquals(5, s.getValue());
        assertTrue(s.plusButton().isDisabled());
        assertFalse(s.minusButton().isDisabled());
        FxTest.onFx(() -> {
            s.setValue(-4);
            return null;
        });
        assertEquals(1, s.getValue());
        assertTrue(s.minusButton().isDisabled());
        assertEquals("1", s.valueLabel().getText());
    }

    @Test
    void elStepperSeManejaConTeclado() throws Exception {
        var s = FxTest.onFx(() -> new Stepper(1, 12, 4));
        FxTest.onFx(() -> {
            press(s, KeyCode.UP);
            press(s, KeyCode.ADD);
            press(s, KeyCode.DOWN);
            return null;
        });
        assertEquals(5, s.getValue());
        assertTrue(s.isFocusTraversable(), "se llega con Tab");
    }

    @Test
    void elStepperDiceSuNombreYSuValor() throws Exception {
        var s = FxTest.onFx(() -> {
            var st = new Stepper(1, 12, 10);
            st.setName("Rounds del evento");
            return st;
        });
        assertEquals("Rounds del evento: 10", s.getAccessibleText());
        FxTest.onFx(() -> {
            s.decrement();
            return null;
        });
        assertEquals("Rounds del evento: 9", s.getAccessibleText());
    }

    private static void press(Node target, KeyCode code) {
        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
    }

    // ---- Menú ⋯ ----

    @Test
    void elMenuReuneLasAccionesYCorreLaElegida() throws Exception {
        var hits = new ArrayList<String>();
        var menu = FxTest.onFx(() -> {
            var m = new ActionMenu("Más acciones de la pelea 1");
            m.add("Editar esquinas", () -> hits.add("editar"));
            m.separator();
            m.add("Quitar de la cartelera", () -> hits.add("quitar"));
            return m;
        });
        assertEquals("Más acciones de la pelea 1", menu.getAccessibleText());
        assertNotNull(menu.getGraphic(), "ícono ⋯");
        assertEquals(3, menu.getItems().size());
        assertInstanceOf(SeparatorMenuItem.class, menu.getItems().get(1));
        FxTest.onFx(() -> {
            ((MenuItem) menu.getItems().get(2)).fire();
            return null;
        });
        assertEquals(List.of("quitar"), hits);
    }

    // ---- Aviso, chip y avatar ----

    @Test
    void queSigueEsUnAvisoConIconoYElMismoTexto() throws Exception {
        var l = FxTest.onFx(() -> Ui.next("Abre el round 1."));
        assertEquals("Qué sigue: Abre el round 1.", l.getText());
        assertTrue(l.getStyleClass().contains("callout"));
        assertNotNull(l.getGraphic(), "ícono de información");
    }

    @Test
    void elChipLlevaSuVarianteYSuIcono() throws Exception {
        var chip = FxTest.onFx(() -> Ui.chip("Enviada", "success", "check"));
        assertEquals("Enviada", chip.getText());
        assertTrue(chip.getStyleClass().containsAll(List.of("chip", "success")));
        assertNotNull(chip.getGraphic());
        var plain = FxTest.onFx(() -> Ui.chip("Programada", "neutral", null));
        assertNull(plain.getGraphic());
    }

    @Test
    void elAvatarMuestraLasInicialesDelNombre() throws Exception {
        assertEquals("AL", Ui.initials("Ana López"));
        assertEquals("M", Ui.initials("María"));
        assertEquals("JR", Ui.initials("  jorge   ruiz díaz "));
        assertEquals("?", Ui.initials(null));
        var avatar = FxTest.onFx(() -> Ui.avatar("Ana López"));
        assertTrue(avatar.getStyleClass().contains("avatar"));
        assertTrue(all(avatar).stream().anyMatch(n -> n instanceof Label l && "AL".equals(l.getText())));
    }

    // ---- Movimiento ----

    @Test
    void entrarAUnaPantallaEsFundidoMasOchoPixelesEn220ms() throws Exception {
        Theme.setReducedMotion(false);
        var node = FxTest.onFx(StackPane::new);
        var anim = FxTest.onFx(() -> Motion.enter(node));
        assertEquals(220, anim.getTotalDuration().toMillis(), 1);
        assertEquals(8, FxTest.onFx(node::getTranslateY), 0.01);
        FxTest.onFx(() -> {
            anim.stop();
            return null;
        });
    }

    @Test
    void conMovimientoReducidoSoloHayUnFundidoCortoYNadaSeMueve() throws Exception {
        Theme.setReducedMotion(true);
        var node = FxTest.onFx(StackPane::new);
        var anim = FxTest.onFx(() -> Motion.enter(node));
        assertInstanceOf(FadeTransition.class, anim);
        assertTrue(anim.getTotalDuration().toMillis() <= 150);
        assertEquals(0, FxTest.onFx(node::getTranslateY), 0.01);
        assertNull(FxTest.onFx(() -> Motion.pulse(new Region())), "sin latido");
        FxTest.onFx(() -> {
            anim.stop();
            return null;
        });
    }

    @Test
    void elLatidoSeRepiteCadaUnoPuntoOchoSegundos() throws Exception {
        Theme.setReducedMotion(false);
        var anim = FxTest.onFx(() -> Motion.pulse(new Region()));
        assertNotNull(anim);
        assertEquals(Animation.INDEFINITE, anim.getCycleCount());
        assertEquals(1800, anim.getCycleDuration().toMillis(), 1);
        FxTest.onFx(() -> {
            anim.stop();
            return null;
        });
    }

    // ---- Hojas ----

    @Test
    void appCssImportaLaHojaDeCadaAreaYLosTemasTienenLosTokensV5() throws IOException {
        var app = read("/com/sigebox/central/app.css");
        for (var sheet : List.of("v5-shell.css", "v5-evento.css", "v5-envivo.css", "v5-resultado.css")) {
            assertTrue(app.contains("@import \"" + sheet + "\";"), sheet);
            assertNotNull(Theme.class.getResource("/com/sigebox/central/" + sheet), sheet);
        }
        for (var theme : List.of("/com/sigebox/central/light.css", "/com/sigebox/central/dark.css")) {
            var css = read(theme);
            for (var token : List.of("-sg-nav:", "-sg-nav-ink:", "-sg-nav-ink-dim:", "-sg-nav-active:",
                    "-sg-success-soft:", "-sg-success-ink:", "-sg-pending-ink:", "-sg-ink-3:", "-sg-ring-track:")) {
                assertTrue(css.contains(token), theme + " " + token);
            }
        }
    }

    private static String read(String resource) throws IOException {
        try (var in = Theme.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
