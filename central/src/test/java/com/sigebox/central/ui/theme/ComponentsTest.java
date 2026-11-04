package com.sigebox.central.ui.theme;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Componentes de DESIGN v4 en {@code ui/theme}: barra con logo, paso activo, skeleton, campo y botones. */
class ComponentsTest {

    private static final List<StepsBar.Item> STEPS = List.of(new StepsBar.Item("evento", "Evento"),
            new StepsBar.Item("jueces", "Jueces"), new StepsBar.Item("vivo", "En vivo"),
            new StepsBar.Item("resultado", "Resultado"));
    private static final List<StepsBar.Item> LINKS = List.of(new StepsBar.Item("terminacion", "Terminación"),
            new StepsBar.Item("bitacora", "Bitácora"));

    @AfterEach
    void followSystemMotionAgain() {
        Theme.setReducedMotion(null);
    }

    private static List<Node> all(Node root) {
        var out = new ArrayList<Node>();
        out.add(root);
        if (root instanceof Parent p) {
            for (var child : p.getChildrenUnmodifiable()) {
                out.addAll(all(child));
            }
        }
        return out;
    }

    private static long currentCount(StepsBar bar) {
        return all(bar).stream().filter(n -> n.getStyleClass().contains("current")).count();
    }

    @Test
    void exactlyOneStepOrLinkIsActiveForEveryScreen() throws Exception {
        FxTest.onFx(() -> {
            var bar = new StepsBar(STEPS, LINKS, id -> { });
            assertEquals(0, currentCount(bar));
            var ids = new ArrayList<String>();
            STEPS.forEach(i -> ids.add(i.id()));
            LINKS.forEach(i -> ids.add(i.id()));
            for (var id : ids) {
                bar.select(id);
                assertEquals(1, currentCount(bar), id);
                assertEquals(id, bar.currentId());
            }
            bar.select("terminos"); // pantalla sin paso ni enlace
            assertEquals(0, currentCount(bar));
            return null;
        });
    }

    @Test
    void theBarCarriesTheLogoAndTheHelpButton() throws Exception {
        FxTest.onFx(() -> {
            var bar = new StepsBar(STEPS, LINKS, id -> { });
            var nodes = all(bar);
            assertTrue(nodes.stream().anyMatch(n -> n.getStyleClass().contains("logo")), "falta el logo");
            assertTrue(nodes.stream().anyMatch(n -> n instanceof Label l && "SIGEBOX".equals(l.getText())));
            var help = nodes.stream().filter(n -> n.getStyleClass().contains("help-button")).toList();
            assertEquals(1, help.size(), "un solo ? en la barra");
            assertEquals("?", ((Button) help.get(0)).getText());
            assertEquals("Ayuda de conexión", help.get(0).getAccessibleText());
            return null;
        });
    }

    @Test
    void theActiveStepHasABackgroundInTheStylesheet() throws IOException {
        var css = ThemeTest.read(Theme.STYLESHEET);
        var rule = Pattern.compile("\\.top-bar \\.current\\s*\\{([^}]*)\\}").matcher(css);
        assertTrue(rule.find(), "falta la regla del paso activo");
        assertTrue(rule.group(1).contains("-fx-background-color: -sg-bar-accent"), rule.group(1));
        assertTrue(rule.group(1).contains("-fx-text-fill: -sg-bar"), rule.group(1));
    }

    @Test
    void logoIsTheBundledCopyOfTheChosenSvg() throws IOException {
        var original = Files.readAllBytes(Path.of("..", "docs", "design", "logo", "guantes-colgando.svg"));
        try (var in = Logo.class.getResourceAsStream(Logo.RESOURCE)) {
            assertNotNull(in, Logo.RESOURCE);
            assertArrayEquals(original, in.readAllBytes(), "el logo empaquetado debe ser idéntico al de docs/design");
        }
    }

    @Test
    void logoShapesKeepTheirOwnColors() throws IOException {
        var shapes = Logo.shapes(Logo.svg());
        assertEquals(11, shapes.size());
        assertEquals("#000000", shapes.get(0).fill());
        assertEquals(8, shapes.stream().filter(s -> s.fill().equalsIgnoreCase("#ff1800")).count());
        assertTrue(shapes.stream().allMatch(s -> s.d().startsWith("m") || s.d().startsWith("M")));
        // Versión chica de la barra (§3.6): sin los brillos.
        var small = Logo.small(shapes);
        assertEquals(9, small.size());
        assertTrue(small.stream().noneMatch(s -> s.fill().equalsIgnoreCase("#fff7f9")));
    }

    @Test
    void logoNodeIsScaledToTheRequestedHeight() throws Exception {
        FxTest.onFx(() -> {
            var logo = Logo.node(28, true);
            assertTrue(logo.getStyleClass().contains("logo"));
            assertEquals(28, logo.getBoundsInParent().getHeight(), 0.5);
            assertEquals(9, all(logo).stream().filter(n -> n instanceof SVGPath).count());
            assertTrue(logo.isMouseTransparent());
            return null;
        });
    }

    @Test
    void skeletonShimmersAndStaysStillWithReducedMotion() throws Exception {
        FxTest.onFx(() -> {
            Theme.setReducedMotion(false);
            var moving = Skeleton.lines(3);
            assertTrue(moving.getStyleClass().contains("skeleton-box"));
            assertEquals("Cargando", moving.getAccessibleText());
            var blocks = moving.getChildren().stream().filter(n -> n.getStyleClass().contains("skeleton")).toList();
            assertEquals(3, blocks.size());
            assertNotNull(moving.shimmer());
            assertEquals(1400, moving.shimmer().getCycleDuration().toMillis(), 1);
            moving.stop();

            Theme.setReducedMotion(true);
            var still = Skeleton.lines(3);
            assertNull(still.shimmer(), "con movimiento reducido el skeleton queda fijo");
            assertEquals(1.0, still.getOpacity());
            return null;
        });
    }

    @Test
    void skeletonLinesFollowTheShapeOfText() throws Exception {
        FxTest.onFx(() -> {
            Theme.setReducedMotion(true);
            var s = Skeleton.lines(3);
            var widths = s.getChildren().stream().filter(n -> n.getStyleClass().contains("skeleton"))
                    .map(n -> ((javafx.scene.layout.Region) n).getMaxWidth()).toList();
            assertEquals(List.of(1.0, 0.8, 0.6), s.fractions());
            assertEquals(3, widths.size());
            var block = Skeleton.block(120, 40);
            assertEquals(1, block.getChildren().size());
            return null;
        });
    }

    @Test
    void fieldHasItsLabelAboveAndTheExampleInside() throws Exception {
        FxTest.onFx(() -> {
            var input = new TextField();
            VBox box = Ui.field("Código", "123456", input);
            assertTrue(box.getStyleClass().contains("field"));
            var label = (Label) box.getChildren().get(0);
            assertEquals("Código", label.getText());
            assertTrue(label.getStyleClass().contains("field-label"));
            assertSame(input, label.getLabelFor());
            assertSame(input, box.getChildren().get(1));
            assertEquals("123456", input.getPromptText());

            var plain = new TextField();
            var old = Ui.field("Rounds", plain);
            assertSame(plain, ((Label) old.getChildren().get(0)).getLabelFor());
            return null;
        });
    }

    @Test
    void fieldStylesFollowDesign() throws IOException {
        var css = ThemeTest.read(Theme.STYLESHEET);
        var label = Pattern.compile("\\.field-label\\s*\\{([^}]*)\\}").matcher(css);
        assertTrue(label.find());
        assertTrue(label.group(1).contains("-fx-text-fill: -sg-ink;"), "etiqueta en ink, no ink_2");
        assertTrue(label.group(1).contains("-fx-font-weight: 600"));
        var input = Pattern.compile("\\.text-field,[^{]*\\{([^}]*)\\}").matcher(css);
        assertTrue(input.find());
        assertTrue(input.group(1).contains("-fx-background-color: -sg-surface;"), "campo sobre surface (§3.2)");
        assertTrue(input.group(1).contains("-fx-border-color: -sg-field-border;"));
        assertTrue(input.group(1).contains("-fx-prompt-text-fill: -sg-ink-2;"));
    }

    @Test
    void primaryAndSecondaryButtons() throws Exception {
        FxTest.onFx(() -> {
            assertTrue(Ui.primary("Crear evento").getStyleClass().contains("primary"));
            assertTrue(Ui.secondary("Cancelar").getStyleClass().contains("secondary"));
            return null;
        });
        var css = ThemeTest.read(Theme.STYLESHEET);
        var secondary = Pattern.compile("\\.button\\.secondary\\s*\\{([^}]*)\\}").matcher(css);
        assertTrue(secondary.find());
        assertTrue(secondary.group(1).contains("-fx-border-color: -sg-field-border;"));
        var button = Pattern.compile("\\n\\.button\\s*\\{([^}]*)\\}").matcher(css);
        assertTrue(button.find());
        assertTrue(button.group(1).contains("-fx-background-radius: 14px;"), "radio 14 (§5.2)");
        assertTrue(button.group(1).contains("-fx-min-height: 44px;"), "alto 44 px (§5.2)");
    }

    @Test
    void reducedMotionCanBeForcedAndReleased() {
        Theme.setReducedMotion(true);
        assertTrue(Theme.reducedMotion());
        Theme.setReducedMotion(false);
        assertFalse(Theme.reducedMotion());
    }
}
