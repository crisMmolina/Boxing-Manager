package com.sigebox.central.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** El sistema visual de DESIGN v2 sin arrancar JavaFX. */
class ThemeTest {

    /** Colores de DESIGN v2 §3.1. */
    private static final Map<String, String> COLORS = Map.ofEntries(
            Map.entry("-sg-bg", "#FFFFFF"),
            Map.entry("-sg-ink", "#0F172A"),
            Map.entry("-sg-ink-secondary", "#5B6472"),
            Map.entry("-sg-navy", "#14284B"),
            Map.entry("-sg-red-corner", "#C62839"),
            Map.entry("-sg-red-numeral", "#B5222F"),
            Map.entry("-sg-blue-corner", "#1F4E9C"),
            Map.entry("-sg-blue-numeral", "#1B438A"),
            Map.entry("-sg-connected", "#1E8E5A"),
            Map.entry("-sg-disconnected", "#9AA3B2"),
            Map.entry("-sg-pending", "#E3E7EE"));

    private static String css() throws IOException {
        try (var in = Theme.class.getResourceAsStream(Theme.STYLESHEET)) {
            assertNotNull(in, "falta app.css");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyDesignColorIsATokenInTheStylesheet() throws IOException {
        var css = css();
        COLORS.forEach((token, hex) -> assertTrue(
                Pattern.compile(Pattern.quote(token) + ":\\s*" + hex + ";", Pattern.CASE_INSENSITIVE)
                        .matcher(css).find(), token + " = " + hex));
    }

    @Test
    void hexColorsOnlyAppearInTheTokenBlock() throws IOException {
        var css = css();
        int end = css.indexOf('}', css.indexOf(".root {"));
        var rest = css.substring(end);
        assertFalse(Pattern.compile("#[0-9A-Fa-f]{6}").matcher(rest).find(),
                "un color suelto fuera de .root: usa una variable -sg-");
    }

    @Test
    void greenIsOnlyTheConnectionDot() throws IOException {
        var css = css();
        long uses = Pattern.compile("-sg-connected\\b").matcher(css).results().count();
        assertEquals(2, uses, "la definición y .dot.connected");
        assertTrue(Pattern.compile("\\.dot\\.connected \\{\\s*-fx-fill: -sg-connected;").matcher(css).find());
    }

    @Test
    void fontsAndTheirLicensesAreBundled() throws IOException {
        for (var font : Theme.FONTS) {
            try (var in = Theme.class.getResourceAsStream(Theme.FONT_DIR + font)) {
                assertNotNull(in, font);
                assertTrue(in.readAllBytes().length > 50_000, font);
            }
        }
        for (var license : new String[] {"Inter-OFL.txt", "BarlowCondensed-OFL.txt"}) {
            try (var in = Theme.class.getResourceAsStream(Theme.FONT_DIR + license)) {
                assertNotNull(in, license);
                assertTrue(new String(in.readAllBytes(), StandardCharsets.UTF_8).contains("SIL Open Font License"),
                        license);
            }
        }
    }
}
