package com.sigebox.central.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javafx.css.CssParser;
import org.junit.jupiter.api.Test;

/** El sistema visual de DESIGN v3 sin arrancar JavaFX. */
class ThemeTest {

    /** Colores de DESIGN v3 §3.1, tema claro. */
    private static final Map<String, String> LIGHT = Map.ofEntries(
            Map.entry("-sg-bg", "#F7F4EF"), Map.entry("-sg-surface", "#FFFFFF"), Map.entry("-sg-surface-2", "#EFEAE2"),
            Map.entry("-sg-ink", "#1C1A17"), Map.entry("-sg-ink-2", "#55585A"),
            Map.entry("-sg-primary", "#1C1A17"), Map.entry("-sg-on-primary", "#FFFFFF"),
            Map.entry("-sg-accent", "#7A5A2A"),
            Map.entry("-sg-red", "#9F2241"), Map.entry("-sg-on-red", "#FFFFFF"), Map.entry("-sg-red-numeral", "#9F2241"),
            Map.entry("-sg-blue", "#266CB4"), Map.entry("-sg-on-blue", "#FFFFFF"), Map.entry("-sg-blue-numeral", "#266CB4"),
            Map.entry("-sg-connected", "#027A35"), Map.entry("-sg-disconnected", "#55585A"),
            Map.entry("-sg-line", "#D9D2C7"));

    /** Colores de DESIGN v3 §3.1, tema oscuro. */
    private static final Map<String, String> DARK = Map.ofEntries(
            Map.entry("-sg-bg", "#14120F"), Map.entry("-sg-surface", "#1F1C18"), Map.entry("-sg-surface-2", "#2A2621"),
            Map.entry("-sg-ink", "#F3EFE8"), Map.entry("-sg-ink-2", "#B8B1A6"),
            Map.entry("-sg-primary", "#F3EFE8"), Map.entry("-sg-on-primary", "#1C1A17"),
            Map.entry("-sg-accent", "#BC955C"),
            Map.entry("-sg-red", "#E58A9D"), Map.entry("-sg-on-red", "#1C1A17"), Map.entry("-sg-red-numeral", "#E58A9D"),
            Map.entry("-sg-blue", "#8CB8EE"), Map.entry("-sg-on-blue", "#1C1A17"), Map.entry("-sg-blue-numeral", "#8CB8EE"),
            Map.entry("-sg-connected", "#4CC97A"), Map.entry("-sg-disconnected", "#B8B1A6"),
            Map.entry("-sg-line", "#3A352E"));

    static String read(String resource) throws IOException {
        try (var in = Theme.class.getResourceAsStream(resource)) {
            assertNotNull(in, "falta " + resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertTokens(String css, Map<String, String> tokens, String file) {
        tokens.forEach((token, hex) -> assertTrue(
                Pattern.compile(Pattern.quote(token) + ":\\s*" + hex + ";", Pattern.CASE_INSENSITIVE).matcher(css).find(),
                file + ": " + token + " = " + hex));
    }

    private static TreeSet<String> definedTokens(String css) {
        var out = new TreeSet<String>();
        Pattern.compile("(-sg-[a-z0-9-]+):").matcher(css).results().forEach(m -> out.add(m.group(1)));
        return out;
    }

    @Test
    void lightAndDarkTokensMatchDesignV3() throws IOException {
        assertTokens(read(Theme.Mode.LIGHT.stylesheet()), LIGHT, "claro");
        assertTokens(read(Theme.Mode.DARK.stylesheet()), DARK, "oscuro");
    }

    @Test
    void bothThemesDefineTheSameTokens() throws IOException {
        assertEquals(definedTokens(read(Theme.Mode.LIGHT.stylesheet())), definedTokens(read(Theme.Mode.DARK.stylesheet())));
    }

    @Test
    void everyVariableUsedByTheComponentsIsDefinedInBothThemes() throws IOException {
        // CssParser no avisa de una variable sin definir: JavaFX solo lo reporta al aplicar el estilo.
        var used = new TreeSet<String>();
        Pattern.compile("(-sg-[a-z0-9-]+)\\b").matcher(read(Theme.STYLESHEET)).results().forEach(m -> used.add(m.group(1)));
        assertFalse(used.isEmpty());
        for (var mode : Theme.Mode.values()) {
            var defined = definedTokens(read(mode.stylesheet()));
            for (var v : used) {
                assertTrue(defined.contains(v), mode + " no define " + v);
            }
        }
    }

    @Test
    void componentStylesheetHasNoLiteralColors() throws IOException {
        var css = read(Theme.STYLESHEET);
        assertFalse(Pattern.compile("#[0-9A-Fa-f]{3,8}\\b|rgba?\\(").matcher(css).find(),
                "app.css solo usa variables -sg-; los colores viven en los archivos de tema");
        assertTrue(definedTokens(css).isEmpty(), "app.css no define tokens de color");
    }

    @Test
    void greenIsOnlyTheConnectionDot() throws IOException {
        var css = read(Theme.STYLESHEET);
        assertEquals(1, Pattern.compile("-sg-connected\\b").matcher(css).results().count());
        assertTrue(Pattern.compile("\\.dot\\.connected \\{[^}]*-sg-connected").matcher(css).find());
    }

    @Test
    void guindaIsOnlyTheRedCorner() throws IOException {
        var css = read(Theme.STYLESHEET);
        var rule = Pattern.compile("([^{}]+)\\{([^}]*)\\}");
        rule.matcher(css).results().forEach(m -> {
            if (Pattern.compile("-sg-red\\b|-sg-red-numeral\\b|-sg-on-red\\b").matcher(m.group(2)).find()) {
                assertTrue(m.group(1).contains("red"), "guinda fuera de la esquina roja en: " + m.group(1).trim());
            }
        });
    }

    @Test
    void everyStylesheetParsesWithoutErrors() throws IOException {
        for (var resource : List.of(Theme.STYLESHEET, Theme.Mode.LIGHT.stylesheet(), Theme.Mode.DARK.stylesheet())) {
            CssParser.errorsProperty().clear();
            var url = Theme.class.getResource(resource);
            assertNotNull(url, resource);
            new CssParser().parse(url);
            assertTrue(CssParser.errorsProperty().isEmpty(), resource + ": " + CssParser.errorsProperty());
        }
    }

    @Test
    void stylesheetsPutTheThemeBeforeTheComponents() {
        for (var mode : Theme.Mode.values()) {
            var sheets = Theme.stylesheets(mode);
            assertEquals(2, sheets.size());
            assertTrue(sheets.get(0).endsWith(mode.stylesheet().substring(mode.stylesheet().lastIndexOf('/'))), sheets.toString());
            assertTrue(sheets.get(1).endsWith("/app.css"), sheets.toString());
        }
        assertEquals(Theme.Mode.DARK, Theme.Mode.LIGHT.toggled());
        assertEquals(Theme.Mode.LIGHT, Theme.Mode.DARK.toggled());
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
            assertTrue(read(Theme.FONT_DIR + license).contains("SIL Open Font License"), license);
        }
    }
}
