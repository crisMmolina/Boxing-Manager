package com.sigebox.central.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** DSK-050: en el tema oscuro el texto secundario se lee (blanco, no gris) y el logo se ve sobre negro. */
class DarkContrastTest {

    private static final List<String> TEXT = List.of("-sg-ink-2", "-sg-ink-3", "-sg-nav-ink", "-sg-nav-ink-dim", "-sg-on-bar-2");
    private static final List<String> V5 = List.of(
            "/com/sigebox/central/v5-envivo.css", "/com/sigebox/central/v5-evento.css",
            "/com/sigebox/central/v5-resultado.css", "/com/sigebox/central/v5-shell.css");
    private static final Pattern LITERAL = Pattern.compile("#[0-9A-Fa-f]{3,8}\\b|rgba?\\(");

    @Test
    void textoSecundarioOscuroLlegaASieteAUno() throws Exception {
        var colors = ThemeTest.cssColors(ThemeTest.read(Theme.Mode.DARK.stylesheet()));
        for (var fg : TEXT) {
            for (var bg : List.of("-sg-bg", "-sg-surface")) {
                assertNotNull(colors.get(fg), fg);
                double c = ThemeTest.contrast(colors.get(fg), colors.get(bg));
                assertTrue(c >= 7.0, fg + " sobre " + bg + " = " + c);
            }
        }
    }

    @Test
    void hojasV5NoFijanColores() throws Exception {
        for (var r : V5) {
            var m = LITERAL.matcher(ThemeTest.read(r));
            boolean found = m.find();
            assertFalse(found, r + " fija un color: " + (found ? m.group() : ""));
        }
    }

    @Test
    void logoOscuroSeVeSobreNegro() {
        for (var s : Logo.forMode(Logo.shapes(Logo.svg()), Theme.Mode.DARK)) {
            for (var bg : List.of("#0A0A09", "#0E0E0C")) {
                double c = ThemeTest.contrast(s.fill(), bg);
                assertTrue(c >= 3.0, s.fill() + " sobre " + bg + " = " + c);
            }
        }
    }

    @Test
    void logoClaroNoCambia() {
        var all = Logo.shapes(Logo.svg());
        assertEquals(all, Logo.forMode(all, Theme.Mode.LIGHT));
    }
}
