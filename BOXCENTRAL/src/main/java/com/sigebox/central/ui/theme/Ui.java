package com.sigebox.central.ui.theme;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/** Componentes de DESIGN v2 §5 para JavaFX. Solo aplican clases de {@code app.css}. */
public final class Ui {

    private Ui() {
    }

    /** Tarjeta de cristal con sus hijos en columna. */
    public static VBox glass(Node... children) {
        var box = new VBox(children);
        box.getStyleClass().add("glass");
        return box;
    }

    public static Label screenTitle(String text) {
        return label(text, "screen-title");
    }

    public static Label cardTitle(String text) {
        return label(text, "card-title");
    }

    public static Label body(String text) {
        var l = label(text, "body");
        l.setWrapText(true);
        return l;
    }

    public static Label support(String text) {
        var l = label(text, "support");
        l.setWrapText(true);
        return l;
    }

    /** Campo con su etiqueta arriba (5.8). */
    public static VBox field(String label, Node control) {
        var box = new VBox(6, label(label, "field-label"), control);
        if (control instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }

    public static Button primary(String text) {
        return button(text, "primary");
    }

    public static Button secondary(String text) {
        return button(text, "secondary");
    }

    /** Píldora neutra de estado (5.5). */
    public static Label pill(String text) {
        return label(text, "pill");
    }

    /** Píldora de esquina: "Roja" o "Azul" en su color. */
    public static Label cornerPill(boolean red) {
        var l = label(red ? "Roja" : "Azul", "pill");
        l.getStyleClass().add(red ? "corner-red" : "corner-blue");
        return l;
    }

    /** Punto de conexión de 10 px (5.1), con texto accesible. */
    public static Circle dot(boolean connected, String who) {
        var c = new Circle(5);
        c.getStyleClass().add("dot");
        setConnected(c, connected, who);
        return c;
    }

    public static void setConnected(Circle dot, boolean connected, String who) {
        dot.getStyleClass().remove("connected");
        if (connected) {
            dot.getStyleClass().add("connected");
        }
        dot.setAccessibleText(who + (connected ? ", conectado" : ", sin conexión"));
    }

    /** Círculo de 10 px del color de la esquina, antes de su etiqueta. */
    public static Circle cornerDot(boolean red) {
        var c = new Circle(5);
        c.getStyleClass().add(red ? "corner-dot-red" : "corner-dot-blue");
        return c;
    }

    /** Número en Barlow Condensed cursiva; {@code corner} = "red", "blue" o "neutral". */
    public static Label numeral(String text, String corner) {
        var l = label(text, "numeral");
        l.getStyleClass().add(corner);
        return l;
    }

    /** Espaciador que empuja lo siguiente a la derecha. */
    public static Region grow() {
        var r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Marca: cuadrado azul marino con una campana de trazo blanco (5.11). */
    public static HBox brand() {
        var bell = new SVGPath();
        bell.setContent("M7 13 L7 9 Q7 4 12 4 Q17 4 17 9 L17 13 L19 15 L5 15 Z M10 17 Q12 19 14 17");
        bell.getStyleClass().add("brand-mark-glyph");
        bell.setScaleX(0.8);
        bell.setScaleY(0.8);
        var mark = new StackPane(bell);
        mark.getStyleClass().add("brand-mark");
        var name = label("SIGEBOX", "brand-name");
        var box = new HBox(10, mark, name);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** Aviso temporal arriba del contenedor: aparece 4 s y se va (5.10). */
    public static void toast(Pane host, String message) {
        var l = label(message, "toast");
        l.setMouseTransparent(true);
        if (host instanceof StackPane sp) {
            StackPane.setAlignment(l, Pos.TOP_CENTER);
            l.setTranslateY(56);
            sp.getChildren().add(l);
        } else {
            host.getChildren().add(l);
        }
        var in = new FadeTransition(Duration.millis(150), l);
        in.setFromValue(0);
        in.setToValue(1);
        var out = new FadeTransition(Duration.millis(200), l);
        out.setToValue(0);
        var seq = new SequentialTransition(in, new PauseTransition(Duration.seconds(4)), out);
        seq.setOnFinished(e -> host.getChildren().remove(l));
        seq.play();
    }

    private static Label label(String text, String styleClass) {
        var l = new Label(text);
        l.getStyleClass().add(styleClass);
        return l;
    }

    private static Button button(String text, String kind) {
        var b = new Button(text);
        b.getStyleClass().add(kind);
        return b;
    }
}
