package com.sigebox.central.ui.theme;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.text.Font;

/**
 * Fuentes empaquetadas y hoja de estilos de DESIGN v2. Las fuentes se cargan una vez al arrancar,
 * desde los recursos (nunca de internet), antes de crear la primera escena.
 */
public final class Theme {

    /** Recurso de la hoja de estilos con los tokens. */
    public static final String STYLESHEET = "/com/sigebox/central/app.css";

    /** TTF empaquetados (licencia OFL junto a ellos). */
    public static final List<String> FONTS = List.of(
            "Inter-Regular.ttf", "Inter-Medium.ttf", "Inter-SemiBold.ttf", "Inter-Bold.ttf",
            "BarlowCondensed-Bold.ttf", "BarlowCondensed-ExtraBold.ttf");

    static final String FONT_DIR = "/com/sigebox/central/fonts/";

    private Theme() {
    }

    /** Carga las fuentes; devuelve las familias que quedaron disponibles. */
    public static List<String> loadFonts() {
        var families = new ArrayList<String>();
        for (var file : FONTS) {
            try (var in = Theme.class.getResourceAsStream(FONT_DIR + file)) {
                if (in == null) {
                    continue;
                }
                var font = Font.loadFont(in, 12);
                if (font != null && !families.contains(font.getFamily())) {
                    families.add(font.getFamily());
                }
            } catch (java.io.IOException e) {
                // Sin la fuente, JavaFX usa la del sistema; la UI sigue funcionando.
            }
        }
        return families;
    }

    /** Agrega la hoja de estilos a la escena. */
    public static void apply(Scene scene) {
        var css = Theme.class.getResource(STYLESHEET);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }
}
