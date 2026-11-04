package com.sigebox.central.ui.theme;

import java.util.ArrayList;
import java.util.List;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.text.Font;

/**
 * Fuentes empaquetadas y hojas de estilo de DESIGN v4. Las fuentes se cargan una vez al arrancar, desde los
 * recursos (nunca de internet), antes de crear la primera escena. Cada escena lleva el archivo de colores del
 * tema actual y, después, {@code app.css} con los componentes.
 */
public final class Theme {

    /** Recurso de la hoja de componentes; solo usa variables {@code -sg-}. */
    public static final String STYLESHEET = "/com/sigebox/central/app.css";

    /** TTF empaquetados (licencia OFL junto a ellos). */
    public static final List<String> FONTS = List.of(
            "Inter-Regular.ttf", "Inter-Medium.ttf", "Inter-SemiBold.ttf", "Inter-Bold.ttf",
            "BarlowCondensed-Bold.ttf", "BarlowCondensed-ExtraBold.ttf");

    static final String FONT_DIR = "/com/sigebox/central/fonts/";

    /** Tema claro (blanco) u oscuro (negro), los dos con dorado y barra negra (DESIGN v4 §3.1). */
    public enum Mode {
        LIGHT("/com/sigebox/central/light.css", "Tema oscuro"),
        DARK("/com/sigebox/central/dark.css", "Tema claro");

        private final String stylesheet;
        private final String toggleLabel;

        Mode(String stylesheet, String toggleLabel) {
            this.stylesheet = stylesheet;
            this.toggleLabel = toggleLabel;
        }

        /** Recurso con los tokens de color de este tema. */
        public String stylesheet() {
            return stylesheet;
        }

        /** Texto del botón que cambia al otro tema. */
        public String toggleLabel() {
            return toggleLabel;
        }

        public Mode toggled() {
            return this == LIGHT ? DARK : LIGHT;
        }
    }

    private static volatile Mode mode = Mode.LIGHT;

    /** {@code null} sigue el ajuste "Reducir movimiento" del sistema. */
    private static volatile Boolean reducedMotion;

    private static final List<Scene> SCENES = new ArrayList<>();

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

    /** Toma el tema del sistema (claro u oscuro). Llamar en el hilo de JavaFX, antes de la primera escena. */
    public static void followSystem() {
        try {
            mode = Platform.getPreferences().getColorScheme() == ColorScheme.DARK ? Mode.DARK : Mode.LIGHT;
        } catch (RuntimeException e) {
            mode = Mode.LIGHT; // sin preferencias del sistema, el claro
        }
    }

    public static Mode mode() {
        return mode;
    }

    /** Movimiento reducido (DESIGN v4 §3.7): el ajuste del sistema, salvo que se fije con {@link #setReducedMotion}. */
    public static boolean reducedMotion() {
        var forced = reducedMotion;
        if (forced != null) {
            return forced;
        }
        try {
            return Platform.getPreferences().isReducedMotion();
        } catch (RuntimeException e) {
            return false; // sin preferencias del sistema, con movimiento
        }
    }

    /** Fija el movimiento reducido; {@code null} vuelve a seguir al sistema. */
    public static void setReducedMotion(Boolean value) {
        reducedMotion = value;
    }

    /** Hojas en orden: primero los colores del tema y luego los componentes. */
    public static List<String> stylesheets(Mode m) {
        var out = new ArrayList<String>();
        for (var resource : List.of(m.stylesheet(), STYLESHEET)) {
            var url = Theme.class.getResource(resource);
            if (url != null) {
                out.add(url.toExternalForm());
            }
        }
        return out;
    }

    /** Aplica el tema actual a la escena y la recuerda para {@link #toggle()}. */
    public static void apply(Scene scene) {
        scene.getStylesheets().setAll(stylesheets(mode));
        synchronized (SCENES) {
            SCENES.removeIf(s -> s.getWindow() != null && !s.getWindow().isShowing());
            if (!SCENES.contains(scene)) {
                SCENES.add(scene);
            }
        }
    }

    /** Cambia al otro tema en todas las escenas abiertas; devuelve el nuevo. */
    public static Mode toggle() {
        mode = mode.toggled();
        synchronized (SCENES) {
            SCENES.removeIf(s -> s.getWindow() != null && !s.getWindow().isShowing());
            for (var s : SCENES) {
                s.getStylesheets().setAll(stylesheets(mode));
            }
        }
        return mode;
    }
}
