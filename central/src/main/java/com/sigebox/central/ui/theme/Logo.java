package com.sigebox.central.ui.theme;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

/**
 * Logo de guantes rojos colgando (DESIGN v4 §3.6, CC0, créditos en {@code docs/design/CREDITOS.md}). JavaFX no
 * carga SVG, así que se leen las rutas del archivo empaquetado y se dibujan como {@link SVGPath} con sus propios
 * colores: el logo no usa tokens del tema ni se recolorea.
 */
public final class Logo {

    /** Copia idéntica de {@code docs/design/logo/guantes-colgando.svg}; nada se descarga en ejecución. */
    public static final String RESOURCE = "/com/sigebox/central/ui/theme/guantes-colgando.svg";

    /** Brillos de los guantes: se quitan en la versión chica (≤ 32 px). */
    static final String HIGHLIGHT = "#fff7f9";

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

    /** Rutas para un tema (DSK-050: pendiente). */
    public static List<Shape> forMode(List<Shape> shapes, Theme.Mode mode) {
        return shapes;
    }

    /** Logo escalado a {@code height} px de alto; quieto (en la barra no se mueve, §3.7). */
    public static Group node(double height, boolean small) {
        var all = shapes(svg());
        var art = new Group();
        for (var s : small ? small(all) : all) {
            var p = new SVGPath();
            p.setContent(s.d());
            p.setFill(Color.web(s.fill()));
            art.getChildren().add(p);
        }
        var bounds = art.getLayoutBounds();
        double scale = height / bounds.getHeight();
        art.setScaleX(scale);
        art.setScaleY(scale);
        // El escalado de JavaFX es desde el centro; Group externo para que el tamaño en el layout sea el escalado.
        var logo = new Group(art);
        logo.getStyleClass().add("logo");
        logo.setMouseTransparent(true);
        logo.setAccessibleText("Logo de SIGEBOX: guantes de box");
        return logo;
    }
}
