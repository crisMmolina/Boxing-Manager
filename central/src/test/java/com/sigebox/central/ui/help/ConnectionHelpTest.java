package com.sigebox.central.ui.help;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.ui.theme.FxTest;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

/** El "?" abre la guía de reconexión que viaja dentro de la central (DESIGN v4 §5.8). */
class ConnectionHelpTest {

    @Test
    void theGuideIsTheBundledCopyOfTheDesignDoc() throws Exception {
        var original = Files.readAllBytes(Path.of("..", "docs", "design", "guia-conexion.md"));
        try (var in = ConnectionGuide.class.getResourceAsStream(ConnectionGuide.RESOURCE)) {
            assertNotNull(in, ConnectionGuide.RESOURCE);
            assertArrayEquals(original, in.readAllBytes(), "la guía empaquetada debe ser idéntica a docs/design");
        }
    }

    @Test
    void centralStepsComeFirstThenThePhone() {
        var guide = ConnectionGuide.load();
        assertEquals("Recuperar la conexión", guide.title());
        assertEquals(2, guide.sections().size());
        assertEquals("En la central (laptop)", guide.sections().get(0).name());
        assertEquals("En el teléfono del juez", guide.sections().get(1).name());
        assertEquals(6, guide.sections().get(0).steps().size());
        assertEquals(6, guide.sections().get(1).steps().size());
        var first = guide.sections().get(0).steps().get(0);
        assertEquals("El hotspot está encendido.", first.lead());
        assertTrue(first.text().startsWith("Revisa que el teléfono"), first.text());
        for (var section : guide.sections()) {
            for (var step : section.steps()) {
                assertFalse((step.lead() + step.text()).contains("**"), step.toString());
                assertFalse((step.lead() + step.text()).contains("http"), step.toString());
            }
        }
    }

    @Test
    void parseIgnoresThePreambleAndTheShortTexts() {
        var md = """
                # Recuperar la conexión

                Texto previo.

                ## En el teléfono del juez

                1. **Espera.** Unos segundos.

                ## En la central (laptop)

                1. **Revisa el hotspot.** Que esté prendido.
                2. Sin negritas.

                ## Texto corto para avisos

                - Central: "Juez 2 sin conexión".
                """;
        var guide = ConnectionGuide.parse(md);
        assertEquals(2, guide.sections().size());
        assertEquals("En la central (laptop)", guide.sections().get(0).name());
        assertEquals("Revisa el hotspot.", guide.sections().get(0).steps().get(0).lead());
        assertEquals("Que esté prendido.", guide.sections().get(0).steps().get(0).text());
        assertEquals("", guide.sections().get(0).steps().get(1).lead());
        assertEquals("Sin negritas.", guide.sections().get(0).steps().get(1).text());
    }

    @Test
    void theBarButtonOpensAndClosesTheSidePanel() throws Exception {
        FxTest.onFx(() -> {
            var root = new BorderPane();
            var bar = HelpButton.bar();
            root.setTop(bar);
            assertEquals("Ayuda de conexión", bar.getAccessibleText());
            bar.fire();
            var panel = root.getRight();
            assertNotNull(panel, "el ? abre el panel lateral");
            assertTrue(panel.getStyleClass().contains("help-panel"));
            assertEquals(420, ((HelpPanel) panel).getPrefWidth());
            var texts = HelpPanel.texts(panel);
            assertTrue(texts.contains("Recuperar la conexión"));
            assertTrue(texts.contains("El hotspot está encendido."));
            assertTrue(texts.indexOf("En la central (laptop)") < texts.indexOf("En el teléfono del juez"));
            var listo = ((HelpPanel) panel).closeButton();
            assertEquals("Listo", listo.getText());
            assertTrue(listo.getStyleClass().contains("primary"));
            listo.fire();
            assertNull(root.getRight(), "Listo cierra el panel");
            bar.fire();
            bar.fire();
            assertNull(root.getRight(), "el ? también cierra el panel abierto");
            return null;
        });
    }

    @Test
    void reconnectButtonSitsNextToADisconnectedJudge() throws Exception {
        FxTest.onFx(() -> {
            Button b = HelpButton.reconnect();
            assertEquals("? Cómo reconectar", b.getText());
            assertTrue(b.getStyleClass().contains("secondary"));
            assertTrue(b.getStyleClass().contains("reconnect"));
            var app = new BorderPane(new StackPane(b)); // el botón vive dentro del contenido de la ventana
            b.fire();
            var panel = app.getRight();
            assertTrue(panel instanceof HelpPanel, "abre el panel en la ventana, no dentro del bloque");
            b.fire();
            assertSame(panel, app.getRight(), "un solo panel a la vez; volver a pedirlo no lo cierra");
            return null;
        });
    }

    @Test
    void connectionRowShowsTheReconnectButtonOnlyWhenDisconnected() throws Exception {
        FxTest.onFx(() -> {
            var row = HelpButton.connection(false, "Juez 2");
            assertTrue(row.getChildren().stream().anyMatch(n -> n.getStyleClass().contains("reconnect")));
            assertTrue(row.getChildren().stream().anyMatch(n -> n instanceof Label l
                    && l.getText().equals("Juez 2 sin conexión")));
            var ok = HelpButton.connection(true, "Juez 2");
            assertTrue(ok.getChildren().stream().noneMatch(n -> n.getStyleClass().contains("reconnect")));
            return null;
        });
    }
}
