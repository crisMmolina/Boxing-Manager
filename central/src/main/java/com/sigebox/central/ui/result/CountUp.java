package com.sigebox.central.ui.result;

import com.sigebox.central.ui.theme.Motion;
import com.sigebox.central.ui.theme.Theme;
import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * Los totales de la ventana de resultado entran contando de 0 a su valor en 400 ms (DESIGN v5 §7), solo con la
 * pelea cerrada; con "reducir movimiento" aparecen de una vez.
 */
final class CountUp {

    static final Duration DURATION = Duration.millis(400);

    private CountUp() {
    }

    /** Cuenta {@code label} de 0 a {@code target}; {@code null} (y el valor de una vez) con movimiento reducido. */
    static Animation play(Label label, int target) {
        if (Theme.reducedMotion()) {
            label.setText(String.valueOf(target));
            return null;
        }
        label.setText("0");
        var t = new Transition() {
            {
                setCycleDuration(DURATION);
                setInterpolator(Motion.EASE);
            }

            @Override
            protected void interpolate(double frac) {
                label.setText(String.valueOf((int) Math.round(target * frac)));
            }
        };
        t.play();
        return t;
    }

    /** Cuenta cada total marcado con {@code .judge-total} dentro de {@code root}. */
    static void playAll(Node root) {
        if (root instanceof Label l && l.getStyleClass().contains("judge-total")) {
            try {
                play(l, Integer.parseInt(l.getText().trim()));
            } catch (NumberFormatException e) {
                // No es un número: se deja como está.
            }
        }
        if (root instanceof Parent p) {
            for (var c : p.getChildrenUnmodifiable()) {
                playAll(c);
            }
        }
    }
}
