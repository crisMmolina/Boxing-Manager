package com.sigebox.central.ui.theme;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Movimiento de la central v5 (DESIGN v5 §3.7): corto, con la curva de la guía; con "reducir movimiento", solo
 * fundidos de 150 ms como máximo, sin latidos ni desplazamientos.
 */
public final class Motion {

    public static final Duration FAST = Duration.millis(120);
    public static final Duration BASE = Duration.millis(180);
    public static final Duration ENTER = Duration.millis(220);
    public static final Duration REDUCED = Duration.millis(150);
    public static final Duration PULSE = Duration.millis(1800);
    /** {@code cubic-bezier(0.2, 0, 0, 1)}. */
    public static final Interpolator EASE = Interpolator.SPLINE(0.2, 0, 0, 1);

    private Motion() {
    }

    /** Entrada de una pantalla: fundido y 8 px hacia arriba en 220 ms (solo fundido con movimiento reducido). */
    public static Animation enter(Node node) {
        boolean reduced = Theme.reducedMotion();
        node.setOpacity(0);
        var fade = new FadeTransition(reduced ? REDUCED : ENTER, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(EASE);
        if (reduced) {
            node.setTranslateY(0);
            fade.play();
            return fade;
        }
        node.setTranslateY(8);
        var slide = new TranslateTransition(ENTER, node);
        slide.setFromY(8);
        slide.setToY(0);
        slide.setInterpolator(EASE);
        var both = new ParallelTransition(fade, slide);
        both.play();
        return both;
    }

    /** Hover que eleva {@code node} 2 px; nada con movimiento reducido. */
    public static void lift(Node node) {
        node.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED, e -> to(node, -2));
        node.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_EXITED, e -> to(node, 0));
    }

    private static void to(Node node, double y) {
        if (Theme.reducedMotion()) {
            return;
        }
        var t = new TranslateTransition(FAST, node);
        t.setToY(y);
        t.setInterpolator(EASE);
        t.play();
    }

    /** Latido de un anillo: cada 1.8 s crece y se desvanece. {@code null} con movimiento reducido. */
    public static Animation pulse(Node node) {
        if (Theme.reducedMotion()) {
            return null;
        }
        var t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(node.scaleXProperty(), 1),
                        new KeyValue(node.scaleYProperty(), 1), new KeyValue(node.opacityProperty(), 0.6)),
                new KeyFrame(PULSE.multiply(0.7), new KeyValue(node.scaleXProperty(), 2.4, EASE),
                        new KeyValue(node.scaleYProperty(), 2.4, EASE), new KeyValue(node.opacityProperty(), 0, EASE)),
                new KeyFrame(PULSE, new KeyValue(node.scaleXProperty(), 1), new KeyValue(node.scaleYProperty(), 1),
                        new KeyValue(node.opacityProperty(), 0)));
        t.setCycleCount(Animation.INDEFINITE);
        t.play();
        return t;
    }
}
