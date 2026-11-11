package com.sigebox.central.ui.theme;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

/**
 * Logo de guantes rojos colgando (DESIGN v4 §3.6, CC0, créditos en {@code docs/design/CREDITOS.md}). JavaFX no
 * carga SVG, así que se leen las rutas del archivo empaquetado y se dibujan como {@link SVGPath} con sus propios
 * colores. En el tema oscuro el trazo negro pasa a marfil para que se vea sobre el fondo (DSK-050).
 */
public final class Logo {

    /** Copia idéntica de {@code docs/design/logo/guantes-colgando.svg}; nada se descarga en ejecución. */
    public static final String RESOURCE = "/com/sigebox/central/ui/theme/guantes-colgando.svg";

    /** Brillos de los guantes: se quitan en la versión chica (≤ 32 px). */
    static final String HIGHLIGHT = "#fff7f9";

    /** Contorno negro del SVG y su color en el tema oscuro (≥ 3:1 sobre el fondo, DSK-050). */
    static final String OUTLINE = "#000000";
    static final String OUTLINE_DARK = "#E8E4DC";

    private static final Pattern FILL = Pattern.compile("(?:^|;)\\s*fill:\\s*(#[0-9A-Fa-f]{6})");

    /** Una ruta del SVG con su relleno. */
    public record Shape(String d, String fill) {}

    private Logo() {
    }

    /** Texto del SVG empaquetado. */
    public static String svg() {
        try (var in = Logo.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("falta " + RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Rutas en orden de dibujo, con el relleno de su {@code style}. */
    public static List<Shape> shapes(String svg) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
            var paths = doc.getElementsByTagName("path");
            var out = new ArrayList<Shape>();
            for (int i = 0; i < paths.getLength(); i++) {
                var el = (Element) paths.item(i);
                var m = FILL.matcher(el.getAttribute("style"));
                out.add(new Shape(el.getAttribute("d").trim(), m.find() ? m.group(1) : "#000000"));
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("logo SVG inválido", e);
        }
    }

    /** Versión chica para la barra: la silueta del par sin brillos ni trazos finos. */
    public static List<Shape> small(List<Shape> shapes) {
        return shapes.stream().filter(s -> !s.fill().equalsIgnoreCase(HIGHLIGHT)).toList();
    }

    /** Rutas para un tema: en oscuro el contorno negro pasa a marfil; en claro no cambia nada. */
    public static List<Shape> forMode(List<Shape> shapes, Theme.Mode mode) {
        if (mode != Theme.Mode.DARK) {
            return shapes;
        }
        return shapes.stream().map(s -> new Shape(s.d(), fill(s.fill(), mode))).toList();
    }

    private static String fill(String original, Theme.Mode mode) {
        return mode == Theme.Mode.DARK && original.equalsIgnoreCase(OUTLINE) ? OUTLINE_DARK : original;
    }

    /** Clase del logo que va sobre la barra negra (igual en los dos temas): su contorno siempre es marfil. */
    static final String ON_DARK = "logo-on-dark";

    /** Recolorea los logos ya dibujados bajo {@code root} al cambiar de tema (cada ruta guarda su relleno original). */
    public static void recolor(Node root, Theme.Mode mode) {
        for (var logo : root.lookupAll(".logo")) {
            paint(logo, logo.getStyleClass().contains(ON_DARK) ? Theme.Mode.DARK : mode);
        }
    }

    private static void paint(Node n, Theme.Mode mode) {
        if (n instanceof SVGPath p && p.getUserData() instanceof String original) {
            p.setFill(Color.web(fill(original, mode)));
        } else if (n instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(c -> paint(c, mode));
        }
    }

    /** Logo escalado a {@code height} px de alto; quieto (en la barra no se mueve, §3.7). Sigue al tema. */
    public static Group node(double height, boolean small) {
        return node(height, small, false);
    }

    /** Como {@link #node(double, boolean)}; con {@code onDark} el contorno es marfil en los dos temas (barra negra). */
    public static Group node(double height, boolean small, boolean onDark) {
        var mode = onDark ? Theme.Mode.DARK : Theme.mode();
        var all = shapes(svg());
        var art = new Group();
        for (var s : small ? small(all) : all) {
            var p = new SVGPath();
            p.setContent(s.d());
            p.setUserData(s.fill());
            p.setFill(Color.web(fill(s.fill(), mode)));
            art.getChildren().add(p);
        }
        var bounds = art.getLayoutBounds();
        double scale = height / bounds.getHeight();
        art.setScaleX(scale);
        art.setScaleY(scale);
        // El escalado de JavaFX es desde el centro; Group externo para que el tamaño en el layout sea el escalado.
        var logo = new Group(art);
        logo.getStyleClass().add("logo");
        if (onDark) {
            logo.getStyleClass().add(ON_DARK);
        }
        logo.setMouseTransparent(true);
        logo.setAccessibleText("Logo de SIGEBOX: guantes de box");
        return logo;
    }
}
