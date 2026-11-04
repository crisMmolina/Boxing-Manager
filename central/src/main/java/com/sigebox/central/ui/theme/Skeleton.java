package com.sigebox.central.ui.theme;

import java.util.List;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Bloques {@code skeleton} con la forma del contenido que viene (DESIGN v4 §5.6). Reemplaza a "Cargando…" en
 * toda la sección que espera datos. Brilla cada 1.4 s (opacidad 0.6 ↔ 1.0); con movimiento reducido queda fijo.
 * Si tarda más de 3 s, aparece una línea de apoyo debajo.
 */
public final class Skeleton extends VBox {

    /** Ancho de las líneas de texto: 100 %, 80 % y 60 %, y se repite. */
    static final List<Double> LINE_FRACTIONS = List.of(1.0, 0.8, 0.6);

    static final Duration SHIMMER = Duration.millis(1400);
    static final Duration SLOW = Duration.seconds(3);
    static final String SLOW_TEXT = "Esto está tardando más de lo normal.";

    private final Timeline shimmer;
    private final PauseTransition slow;
    private final List<Double> fractions;

    private Skeleton(List<Region> blocks, List<Double> fractions) {
        super(8);
        this.fractions = fractions;
        getStyleClass().add("skeleton-box");
        setAccessibleRole(AccessibleRole.TEXT);
        setAccessibleText("Cargando");
        getChildren().addAll(blocks);
        var notice = new Label(SLOW_TEXT);
        notice.getStyleClass().add("support");
        slow = new PauseTransition(SLOW);
        slow.setOnFinished(e -> getChildren().add(notice));
        slow.play();
        if (Theme.reducedMotion()) {
            shimmer = null;
        } else {
            shimmer = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(opacityProperty(), 1.0, Interpolator.EASE_BOTH)),
                    new KeyFrame(SHIMMER.divide(2), new KeyValue(opacityProperty(), 0.6, Interpolator.EASE_BOTH)),
                    new KeyFrame(SHIMMER, new KeyValue(opacityProperty(), 1.0, Interpolator.EASE_BOTH)));
            shimmer.setCycleCount(Animation.INDEFINITE);
            shimmer.play();
        }
        // Al salir de la escena (los datos llegaron) se detiene solo.
        sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                stop();
            }
        });
    }

    /** {@code count} líneas de texto de 12 px a 100 %, 80 % y 60 % del ancho. */
    public static Skeleton lines(int count) {
        var blocks = new java.util.ArrayList<Region>();
        var fractions = new java.util.ArrayList<Double>();
        for (int i = 0; i < count; i++) {
            double f = LINE_FRACTIONS.get(i % LINE_FRACTIONS.size());
            var line = block("skeleton-line");
            fractions.add(f);
            blocks.add(line);
        }
        var s = new Skeleton(blocks, List.copyOf(fractions));
        for (int i = 0; i < blocks.size(); i++) {
            var line = blocks.get(i);
            double f = fractions.get(i);
            line.maxWidthProperty().bind(s.widthProperty().multiply(f));
            line.prefWidthProperty().bind(s.widthProperty().multiply(f));
        }
        return s;
    }

    /** Un bloque del tamaño de una tarjeta o fila que todavía no llega. */
    public static Skeleton block(double width, double height) {
        var b = block("skeleton-card");
        b.setPrefSize(width, height);
        b.setMinHeight(height);
        return new Skeleton(List.of(b), List.of());
    }

    private static Region block(String kind) {
        var r = new Region();
        r.getStyleClass().addAll("skeleton", kind);
        return r;
    }

    /** Animación del brillo, o {@code null} con movimiento reducido. */
    public Timeline shimmer() {
        return shimmer;
    }

    /** Ancho relativo de cada línea. */
    public List<Double> fractions() {
        return fractions;
    }

    /** Detiene el brillo y el aviso de demora. */
    public void stop() {
        if (shimmer != null) {
            shimmer.stop();
            setOpacity(1.0);
        }
        slow.stop();
    }
}
