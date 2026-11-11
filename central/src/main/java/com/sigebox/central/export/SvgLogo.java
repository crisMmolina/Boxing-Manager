package com.sigebox.central.export;

import java.awt.Color;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.openpdf.text.pdf.PdfContentByte;
import org.w3c.dom.Element;

/**
 * Los guantes de SIGEBOX como vector para el PDF, leídos del mismo SVG que usa la interfaz. Solo
 * entiende lo que trae ese archivo: paths con relleno sólido y comandos M C S L H V Z.
 */
final class SvgLogo {

    private static final String RESOURCE = "/com/sigebox/central/ui/theme/guantes-colgando.svg";
    private static final Pattern FILL = Pattern.compile("(?:^|;)\\s*fill:\\s*(#[0-9A-Fa-f]{6})");
    private static final Pattern TOKEN =
            Pattern.compile("[A-Za-z]|[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?");

    /** Un trazo absoluto: 'M' y 'L' con 2 números, 'C' con 6, 'Z' sin números. */
    private record Op(char type, double[] p) { }

    private record Shape(String fill, List<Op> ops) { }

    private final List<Shape> shapes;
    private final double minX;
    private final double minY;
    private final double width;
    private final double height;

    private SvgLogo(List<Shape> shapes) {
        this.shapes = shapes;
        double x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, y1 = -Double.MAX_VALUE;
        for (var s : shapes) {
            for (var op : s.ops()) {
                for (int i = 0; i + 1 < op.p().length; i += 2) {
                    x0 = Math.min(x0, op.p()[i]);
                    x1 = Math.max(x1, op.p()[i]);
                    y0 = Math.min(y0, op.p()[i + 1]);
                    y1 = Math.max(y1, op.p()[i + 1]);
                }
            }
        }
        minX = x0;
        minY = y0;
        width = x1 - x0;
        height = y1 - y0;
    }

    static SvgLogo load() {
        try (InputStream in = SvgLogo.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("falta " + RESOURCE);
            }
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            var doc = factory.newDocumentBuilder().parse(in);
            var nodes = doc.getElementsByTagName("path");
            var shapes = new ArrayList<Shape>();
            for (int i = 0; i < nodes.getLength(); i++) {
                var el = (Element) nodes.item(i);
                Matcher m = FILL.matcher(el.getAttribute("style"));
                var fill = m.find() ? m.group(1).toLowerCase() : el.getAttribute("fill").toLowerCase();
                if (fill.isEmpty() || fill.equals("none")) {
                    continue;
                }
                shapes.add(new Shape(fill, parse(el.getAttribute("d"))));
            }
            return new SvgLogo(shapes);
        } catch (Exception e) {
            throw new IllegalStateException("no se pudo leer el logo", e);
        }
    }

    /** Ancho que ocupa el logo dibujado con esa altura. */
    double widthFor(double h) {
        return width * h / height;
    }

    /**
     * Dibuja el logo con su esquina inferior izquierda en (x, y) y la altura dada. Sobre fondo oscuro
     * el contorno negro pasa al tono claro de la interfaz, como hace el modo oscuro.
     */
    void draw(PdfContentByte cb, double x, double y, double h, boolean onDark) {
        double k = h / height;
        cb.saveState();
        for (var s : shapes) {
            var hex = onDark && s.fill().equals("#000000") ? "#e8e4dc" : s.fill();
            cb.setColorFill(Color.decode(hex));
            for (var op : s.ops()) {
                var p = op.p();
                switch (op.type()) {
                    case 'M' -> cb.moveTo(fx(x, k, p[0]), fy(y, h, k, p[1]));
                    case 'L' -> cb.lineTo(fx(x, k, p[0]), fy(y, h, k, p[1]));
                    case 'C' -> cb.curveTo(fx(x, k, p[0]), fy(y, h, k, p[1]), fx(x, k, p[2]), fy(y, h, k, p[3]),
                            fx(x, k, p[4]), fy(y, h, k, p[5]));
                    default -> cb.closePath();
                }
            }
            cb.fill();
        }
        cb.restoreState();
    }

    private float fx(double x, double k, double sx) {
        return (float) (x + (sx - minX) * k);
    }

    /** El SVG crece hacia abajo y el PDF hacia arriba. */
    private float fy(double y, double h, double k, double sy) {
        return (float) (y + h - (sy - minY) * k);
    }

    static List<Op> parse(String d) {
        var tokens = new ArrayList<String>();
        var m = TOKEN.matcher(d);
        while (m.find()) {
            tokens.add(m.group());
        }
        var ops = new ArrayList<Op>();
        double cx = 0, cy = 0, sx = 0, sy = 0;
        double lastCx = 0, lastCy = 0;
        char prev = ' ';
        char cmd = ' ';
        int i = 0;
        while (i < tokens.size()) {
            var t = tokens.get(i);
            if (Character.isLetter(t.charAt(0))) {
                cmd = t.charAt(0);
                i++;
                if (cmd == 'z' || cmd == 'Z') {
                    ops.add(new Op('Z', new double[0]));
                    cx = sx;
                    cy = sy;
                    prev = 'Z';
                    continue;
                }
            }
            boolean rel = Character.isLowerCase(cmd);
            double ox = rel ? cx : 0;
            double oy = rel ? cy : 0;
            switch (Character.toUpperCase(cmd)) {
                case 'M' -> {
                    cx = ox + num(tokens, i++);
                    cy = oy + num(tokens, i++);
                    sx = cx;
                    sy = cy;
                    ops.add(new Op('M', new double[] {cx, cy}));
                    cmd = rel ? 'l' : 'L';
                    prev = 'M';
                }
                case 'L' -> {
                    cx = ox + num(tokens, i++);
                    cy = oy + num(tokens, i++);
                    ops.add(new Op('L', new double[] {cx, cy}));
                    prev = 'L';
                }
                case 'H' -> {
                    cx = (rel ? cx : 0) + num(tokens, i++);
                    ops.add(new Op('L', new double[] {cx, cy}));
                    prev = 'L';
                }
                case 'V' -> {
                    cy = (rel ? cy : 0) + num(tokens, i++);
                    ops.add(new Op('L', new double[] {cx, cy}));
                    prev = 'L';
                }
                case 'C' -> {
                    double x1 = ox + num(tokens, i++), y1 = oy + num(tokens, i++);
                    double x2 = ox + num(tokens, i++), y2 = oy + num(tokens, i++);
                    cx = ox + num(tokens, i++);
                    cy = oy + num(tokens, i++);
                    ops.add(new Op('C', new double[] {x1, y1, x2, y2, cx, cy}));
                    lastCx = x2;
                    lastCy = y2;
                    prev = 'C';
                }
                case 'S' -> {
                    double x1 = prev == 'C' ? 2 * cx - lastCx : cx;
                    double y1 = prev == 'C' ? 2 * cy - lastCy : cy;
                    double x2 = ox + num(tokens, i++), y2 = oy + num(tokens, i++);
                    cx = ox + num(tokens, i++);
                    cy = oy + num(tokens, i++);
                    ops.add(new Op('C', new double[] {x1, y1, x2, y2, cx, cy}));
                    lastCx = x2;
                    lastCy = y2;
                    prev = 'C';
                }
                default -> throw new IllegalArgumentException("comando SVG no soportado: " + cmd);
            }
        }
        return ops;
    }

    private static double num(List<String> tokens, int i) {
        return Double.parseDouble(tokens.get(i));
    }
}
