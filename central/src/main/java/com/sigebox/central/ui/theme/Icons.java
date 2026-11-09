package com.sigebox.central.ui.theme;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;

/**
 * Íconos de línea de la central (DESIGN v5 §3.9), dibujados con figuras de JavaFX: nada se descarga ni hay
 * dependencias. El color sale de la hoja ({@code .icon-shape}: {@code -sg-ink}; variantes {@code .icon.muted},
 * {@code .accent}, {@code .on-primary}, {@code .success}).
 *
 * <p>Trazos de Lucide (https://lucide.dev), ISC License · Copyright (c) for portions of Lucide are held by Cole
 * Bemis 2013-2022 as part of Feather (MIT). All other copyright (c) for Lucide are held by Lucide Contributors 2022.
 * Cada elemento es "P:" (path), "C:cx,cy,r" (círculo) o "R:x,y,ancho,alto,rx" (rectángulo), en una cuadrícula de 24.
 */
public final class Icons {

    private static final double GRID = 24;

    private static final Map<String, String[]> SHAPES = new LinkedHashMap<>();

    static {
        def("calendar", "P:M8 2v3", "P:M16 2v3", "R:3,3,18,18,2", "P:M3 9h18");
        def("users", "P:M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", "P:M16 3.128a4 4 0 0 1 0 7.744",
                "P:M22 21v-2a4 4 0 0 0-3-3.87", "C:9,7,4");
        def("activity", "P:M22 12h-2.48a2 2 0 0 0-1.93 1.46l-2.35 8.36a.25.25 0 0 1-.48 0L9.24 2.18a.25.25 0 0 0-.48 "
                + "0l-2.35 8.36A2 2 0 0 1 4.49 12H2");
        def("trophy", "P:M10 14.66V17a1 1 0 0 1-1 1 2 2 0 0 0-2 2v2", "P:M14 14.66V17a1 1 0 0 0 1 1 2 2 0 0 1 2 2v2",
                "P:M17.916 10H19.5A2.5 2.5 0 0 0 22 7.5V5a1 1 0 0 0-1-1h-3", "P:M4 22h16",
                "P:M6 9a6 6 0 0 0 12 0V3a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1z",
                "P:M6.084 10H4.5A2.5 2.5 0 0 1 2 7.5V5a1 1 0 0 1 1-1h3");
        def("file-text", "P:M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 "
                + "8v12a2 2 0 0 1-2 2z", "P:M14 2v5a1 1 0 0 0 1 1h5", "P:M10 9H8", "P:M16 13H8", "P:M16 17H8");
        def("house", "P:M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8",
                "P:M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z");
        def("sun", "C:12,12,4", "P:M12 2v2", "P:M12 20v2", "P:m4.93 4.93 1.41 1.41", "P:m17.66 17.66 1.41 1.41",
                "P:M2 12h2", "P:M20 12h2", "P:m6.34 17.66-1.41 1.41", "P:m19.07 4.93-1.41 1.41");
        def("moon", "P:M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215"
                + ".825-.004.803.401");
        def("circle-question-mark", "C:12,12,10", "P:M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3", "P:M12 17h.01");
        def("info", "C:12,12,10", "P:M12 16v-4", "P:M12 8h.01");
        def("circle-alert", "C:12,12,10", "P:M12 8v4", "P:M12 16h.01");
        def("check", "P:M20 6 9 17l-5-5");
        def("x", "P:M18 6 6 18", "P:m6 6 12 12");
        def("plus", "P:M5 12h14", "P:M12 5v14");
        def("minus", "P:M5 12h14");
        def("ellipsis", "C:12,12,1", "C:19,12,1", "C:5,12,1");
        def("play", "P:M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z");
        def("clock", "C:12,12,10", "P:M12 6v6l4 2");
        def("rotate-ccw", "P:M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8", "P:M3 3v5h5");
        def("wifi", "P:M12 20h.01", "P:M2 8.82a15 15 0 0 1 20 0", "P:M5 12.859a10 10 0 0 1 14 0",
                "P:M8.5 16.429a5 5 0 0 1 7 0");
        def("wifi-off", "P:M12 20h.01", "P:M8.5 16.429a5 5 0 0 1 7 0", "P:M5 12.859a10 10 0 0 1 5.17-2.69",
                "P:M19 12.859a10 10 0 0 0-2.007-1.523", "P:M2 8.82a15 15 0 0 1 4.177-2.643",
                "P:M22 8.82a15 15 0 0 0-11.288-3.764", "P:m2 2 20 20");
        def("arrow-right", "P:M5 12h14", "P:m12 5 7 7-7 7");
        def("grip-vertical", "C:9,12,1", "C:9,5,1", "C:9,19,1", "C:15,12,1", "C:15,5,1", "C:15,19,1");
        def("qr-code", "R:3,3,5,5,1", "R:16,3,5,5,1", "R:3,16,5,5,1", "P:M21 16h-3a2 2 0 0 0-2 2v3", "P:M21 21v.01",
                "P:M12 7v3a2 2 0 0 1-2 2H7", "P:M3 12h.01", "P:M12 3h.01", "P:M12 16v.01", "P:M16 12h1",
                "P:M21 12v.01", "P:M12 21v-1");
        def("pencil", "P:M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623"
                + ".622l4.353-1.32a2 2 0 0 0 .83-.497z", "P:m15 5 4 4");
        def("skip-forward", "P:M21 4v16",
                "P:M6.029 4.285A2 2 0 0 0 3 6v12a2 2 0 0 0 3.029 1.715l9.997-5.998a2 2 0 0 0 .003-3.432z");
        def("trash", "P:M10 11v6", "P:M14 11v6", "P:M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", "P:M3 6h18",
                "P:M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2");
        def("arrow-up", "P:m5 12 7-7 7 7", "P:M12 19V5");
        def("arrow-down", "P:M12 5v14", "P:m19 12-7 7-7-7");
    }

    /** Nombres disponibles (los de Lucide). */
    public static final List<String> NAMES = List.copyOf(SHAPES.keySet());

    private Icons() {
    }

    private static void def(String name, String... shapes) {
        SHAPES.put(name, shapes);
    }

    /**
     * El ícono {@code name} de {@code size} × {@code size} px, con trazo de 2 en la cuadrícula de 24.
     *
     * @throws IllegalArgumentException si no existe
     */
    public static Pane icon(String name, double size) {
        var spec = SHAPES.get(name);
        if (spec == null) {
            throw new IllegalArgumentException("ícono desconocido: " + name);
        }
        var group = new Group();
        for (var s : spec) {
            group.getChildren().add(shape(s));
        }
        group.getTransforms().add(new Scale(size / GRID, size / GRID, 0, 0));
        var holder = new Pane(group);
        holder.getStyleClass().add("icon");
        holder.setPrefSize(size, size);
        holder.setMinSize(size, size);
        holder.setMaxSize(size, size);
        holder.setMouseTransparent(true);
        return holder;
    }

    private static Shape shape(String spec) {
        var kind = spec.charAt(0);
        var data = spec.substring(2);
        Shape shape;
        switch (kind) {
            case 'P' -> {
                var p = new SVGPath();
                p.setContent(data);
                shape = p;
            }
            case 'C' -> {
                var v = numbers(data);
                shape = new Circle(v[0], v[1], v[2]);
            }
            case 'R' -> {
                var v = numbers(data);
                var r = new Rectangle(v[0], v[1], v[2], v[3]);
                r.setArcWidth(v[4] * 2);
                r.setArcHeight(v[4] * 2);
                shape = r;
            }
            default -> throw new IllegalStateException("figura desconocida: " + spec);
        }
        shape.setFill(null);
        shape.setStroke(javafx.scene.paint.Color.web("#111110")); // sin hoja; la hoja usa -sg-ink
        shape.setStrokeWidth(2);
        shape.setStrokeLineCap(StrokeLineCap.ROUND);
        shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
        shape.getStyleClass().add("icon-shape");
        return shape;
    }

    private static double[] numbers(String csv) {
        var parts = csv.split(",");
        var out = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Double.parseDouble(parts[i].trim());
        }
        return out;
    }
}
