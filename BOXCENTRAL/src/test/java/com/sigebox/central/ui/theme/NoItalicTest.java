package com.sigebox.central.ui.theme;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Pedido de zoe (DSK-013): ningún texto en cursiva y el contenido en una columna centrada. */
class NoItalicTest {

    private static String css() throws IOException {
        try (var in = Theme.class.getResourceAsStream(Theme.STYLESHEET)) {
            assertNotNull(in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void stylesheetHasNoItalic() throws IOException {
        assertFalse(css().contains("italic"), "app.css no debe poner nada en cursiva");
    }

    @Test
    void onlyUprightFontsAreBundled() {
        assertTrue(Theme.FONTS.stream().noneMatch(f -> f.toLowerCase().contains("italic")), Theme.FONTS.toString());
        assertTrue(Theme.FONTS.contains("BarlowCondensed-ExtraBold.ttf"));
        assertNotNull(Theme.class.getResourceAsStream(Theme.FONT_DIR + "BarlowCondensed-ExtraBold.ttf"));
    }

    @Test
    void screensAreCentered() throws IOException {
        assertTrue(css().matches("(?s).*\\.screen \\{\\s*-fx-alignment: top-center;.*"));
    }
}
