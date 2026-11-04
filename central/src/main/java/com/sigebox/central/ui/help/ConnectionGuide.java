package com.sigebox.central.ui.help;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Guía "Recuperar la conexión" (DESIGN v4 §5.8). El texto es una copia de {@code docs/design/guia-conexion.md}
 * dentro de los recursos, así que se abre sin internet. Solo se toman los pasos numerados de las secciones
 * {@code ## En …}; la de la central va primero y después la del teléfono.
 */
public record ConnectionGuide(String title, List<Section> sections) {

    /** Copia idéntica de {@code docs/design/guia-conexion.md}. */
    public static final String RESOURCE = "/com/sigebox/central/ui/help/guia-conexion.md";

    static final String CENTRAL = "En la central";

    /** Un paso: lo que se ve en negritas ({@code lead}, puede estar vacío) y el resto del texto. */
    public record Step(String lead, String text) {}

    /** Pasos de una app. */
    public record Section(String name, List<Step> steps) {}

    private static final Pattern STEP = Pattern.compile("^\\d+\\.\\s+(?:\\*\\*(.+?)\\*\\*)?\\s*(.*)$");

    public ConnectionGuide {
        sections = List.copyOf(sections);
    }

    /** La guía empaquetada. */
    public static ConnectionGuide load() {
        try (var in = ConnectionGuide.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("falta " + RESOURCE);
            }
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Lee el título y los pasos de cada sección {@code ## En …}; la de la central queda primero. */
    public static ConnectionGuide parse(String markdown) {
        String title = "Recuperar la conexión";
        var sections = new ArrayList<Section>();
        String name = null;
        var steps = new ArrayList<Step>();
        for (var raw : markdown.split("\\R")) {
            var line = raw.strip();
            if (line.startsWith("# ")) {
                title = line.substring(2).strip();
            } else if (line.startsWith("## ")) {
                if (name != null) {
                    sections.add(new Section(name, steps));
                }
                var heading = line.substring(3).strip();
                name = heading.startsWith("En ") ? heading : null;
                steps = new ArrayList<>();
            } else if (name != null) {
                var m = STEP.matcher(line);
                if (m.matches()) {
                    steps.add(new Step(m.group(1) == null ? "" : clean(m.group(1)), clean(m.group(2))));
                }
            }
        }
        if (name != null) {
            sections.add(new Section(name, steps));
        }
        sections.sort((a, b) -> Boolean.compare(!a.name().startsWith(CENTRAL), !b.name().startsWith(CENTRAL)));
        return new ConnectionGuide(title, sections);
    }

    private static String clean(String text) {
        return text.replace("**", "").replace("`", "").strip();
    }
}
