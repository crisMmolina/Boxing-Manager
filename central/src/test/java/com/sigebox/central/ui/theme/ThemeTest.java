package com.sigebox.central.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javafx.css.CssParser;
import org.junit.jupiter.api.Test;

/** El sistema visual de DESIGN v4 sin arrancar JavaFX. */
class ThemeTest {

    /** Guía visual vigente; las pruebas leen sus tablas para que el CSS no se separe de ella. */
    static final Path DESIGN = Path.of("..", "docs", "DESIGN.md");

    /** Par extra que no está en §3.2: el paso o enlace activo de la barra (texto {@code bar} sobre fondo dorado). */
    private static final List<String[]> EXTRA_PAIRS = List.<String[]>of(new String[] {"bar", "bar_accent", "texto"});

    private static final Pattern CODE = Pattern.compile("`([a-z0-9_]+)`");
    private static final Pattern HEX = Pattern.compile("#[0-9A-Fa-f]{6}\\b");

    /** Filas de la tabla que sigue al encabezado {@code heading} de DESIGN.md, ya partidas en celdas. */
    static List<String[]> table(String heading) throws IOException {
        var rows = new ArrayList<String[]>();
        var inside = false;
        for (var line : Files.readAllLines(DESIGN, StandardCharsets.UTF_8)) {
            if (line.startsWith("#")) {
                inside = line.startsWith(heading);
            } else if (inside && line.startsWith("|") && !line.startsWith("|---")) {
                rows.add(line.substring(1).split("\\|"));
            }
        }
        rows.remove(0); // encabezado de la tabla
        return rows;
    }

    /** Tokens de color de DESIGN §3.1 en un tema ({@code column} 1 = claro, 2 = oscuro), con nombre CSS. */
    static Map<String, String> designTokens(int column) throws IOException {
        var out = new LinkedHashMap<String, String>();
        for (var row : table("### 3.1 Color")) {
            var names = CODE.matcher(row[0]).results().map(m -> m.group(1)).toList();
            var hexes = HEX.matcher(row[column]).results().map(m -> m.group()).toList();
            assertEquals(names.size(), hexes.size(), String.join("|", row));
            for (int i = 0; i < names.size(); i++) {
                out.put(cssName(names.get(i)), hexes.get(i));
            }
        }
        assertTrue(out.size() >= 24, out.toString());
        return out;
    }

    static String cssName(String token) {
        return "-sg-" + token.replace('_', '-');
    }

    /** Valor hexadecimal de cada token definido en una hoja de tema. */
    static Map<String, String> cssColors(String css) {
        var out = new LinkedHashMap<String, String>();
        Pattern.compile("(-sg-[a-z0-9-]+):\\s*(#[0-9A-Fa-f]{6})\\s*;").matcher(css).results()
                .forEach(m -> out.put(m.group(1), m.group(2)));
        return out;
    }

    /** Luminancia relativa de WCAG 2.x. */
    static double luminance(String hex) {
        double[] c = new double[3];
        for (int i = 0; i < 3; i++) {
            var v = Integer.parseInt(hex.substring(1 + 2 * i, 3 + 2 * i), 16) / 255.0;
            c[i] = v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
    }

    static double contrast(String a, String b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

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
    void lightAndDarkTokensMatchDesignV4() throws IOException {
        assertTokens(read(Theme.Mode.LIGHT.stylesheet()), designTokens(1), "claro");
        assertTokens(read(Theme.Mode.DARK.stylesheet()), designTokens(2), "oscuro");
    }

    @Test
    void everyPairOfDesignPassesAaInBothThemes() throws IOException {
        var pairs = new ArrayList<String[]>();
        for (var row : table("### 3.2 Contraste AA medido")) {
            var names = CODE.matcher(row[0]).results().map(m -> m.group(1)).toList();
            pairs.add(new String[] {names.get(0), names.get(1), row[1].trim()});
        }
        assertEquals(28, pairs.size(), "DESIGN §3.2 mide 28 pares por tema");
        pairs.addAll(EXTRA_PAIRS);
        var failures = new ArrayList<String>();
        for (var mode : Theme.Mode.values()) {
            var colors = cssColors(read(mode.stylesheet()));
            for (var p : pairs) {
                var fg = colors.get(cssName(p[0]));
                var bg = colors.get(cssName(p[1]));
                assertNotNull(fg, mode + " no define " + p[0]);
                assertNotNull(bg, mode + " no define " + p[1]);
                double min = p[2].equals("texto") ? 4.5 : 3.0;
                double ratio = contrast(fg, bg);
                if (ratio < min) {
                    failures.add(mode + " " + p[0] + "/" + p[1] + " = " + String.format("%.2f", ratio) + " < " + min);
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.toString());
    }

    @Test
    void contrastFormulaMatchesTheMeasurementInDesign() {
        // Valores de DESIGN §3.2: ink/bg claro 17.95 y field_border/surface oscuro 3.96.
        assertEquals(17.95, contrast("#111110", "#FAF9F6"), 0.01);
        assertEquals(3.96, contrast("#7D776D", "#1A1917"), 0.01);
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
