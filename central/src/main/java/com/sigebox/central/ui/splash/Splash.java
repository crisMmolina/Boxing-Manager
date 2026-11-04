package com.sigebox.central.ui.splash;

import com.sigebox.central.ui.theme.Logo;
import com.sigebox.central.ui.theme.Theme;
import com.sigebox.central.ui.theme.Ui;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * Pantalla de carga (DESIGN v4 §5.1): logo de 200 px, SIGEBOX, progreso indeterminado y "Abriendo la base…".
 * El trabajo (abrir y migrar la base, reconstruir la pelea activa) corre fuera del hilo de JavaFX; la pantalla
 * dura al menos 600 ms. Si falla, el logo queda quieto, el error se lee en español y "Reintentar" vuelve a
 * intentarlo. Si pasa de 3 s, aparece una línea de apoyo.
 *
 * @param <T> lo que deja listo la carga
 */
public final class Splash<T> extends StackPane {

    public static final Duration MIN = Duration.millis(600);
    public static final Duration MAX = Duration.seconds(3);
    public static final String LOADING = "Abriendo la base…";
    public static final String SLOW = "Esto está tardando más de lo normal.";
    public static final String ERROR = "No se pudo abrir la base de datos de SIGEBOX. Cierra otras ventanas de la "
            + "central y pulsa Reintentar; si sigue, avisa a quien instaló la laptop.";

    private final Callable<T> work;
    private final Consumer<T> onReady;
    private final Group art;
    private final ProgressBar progress = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
    private final Label message = Ui.support(LOADING);
    private final Label error = Ui.body(ERROR);
    private final Button retry = Ui.primary("Reintentar");
    private final PauseTransition slow = new PauseTransition(MAX);
    private final Rotate tilt = new Rotate(0);
    private Timeline swing;
    private long startedNanos;

    public Splash(Callable<T> work, Consumer<T> onReady) {
        this.work = work;
        this.onReady = onReady;
        getStyleClass().addAll("splash", "app-root");
        art = Logo.node(200, false);
        // El eje está donde cuelgan los cordones: arriba al centro del dibujo.
        var b = art.getLayoutBounds();
        tilt.setPivotX(b.getMinX() + b.getWidth() / 2);
        tilt.setPivotY(b.getMinY());
        art.getTransforms().add(tilt);
        var name = new Label("SIGEBOX");
        name.getStyleClass().add("brand-name");
        name.setStyle("-fx-font-size: 32px;");
        progress.setPrefWidth(280);
        progress.setPrefHeight(4);
        error.setWrapText(true);
        error.setMaxWidth(420);
        error.setVisible(false);
        error.setManaged(false);
        retry.setVisible(false);
        retry.setManaged(false);
        retry.setOnAction(e -> start());
        slow.setOnFinished(e -> message.setText(SLOW));
        var column = new VBox(16, art, name, progress, message, error, retry);
        column.setAlignment(Pos.CENTER);
        getChildren().add(column);
        setAccessibleText("Cargando SIGEBOX");
        if (Theme.reducedMotion()) {
            var fade = new FadeTransition(Duration.millis(150), art);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        } else {
            // Entra con un giro de −8° a 0° en 420 ms y luego un vaivén de ±2° cada 1.6 s (§3.7).
            swing = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(tilt.angleProperty(), -8)),
                    new KeyFrame(Duration.millis(420), new KeyValue(tilt.angleProperty(), 0, Interpolator.EASE_OUT)),
                    new KeyFrame(Duration.millis(820), new KeyValue(tilt.angleProperty(), 2, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.millis(1620), new KeyValue(tilt.angleProperty(), -2, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.millis(2020), new KeyValue(tilt.angleProperty(), 0, Interpolator.EASE_BOTH)));
            swing.setCycleCount(Animation.INDEFINITE);
            swing.play();
        }
    }

    /** Empieza (o reintenta) la carga en un hilo aparte. */
    public void start() {
        showError(false);
        message.setText(LOADING);
        startedNanos = System.nanoTime();
        slow.playFromStart();
        var thread = new Thread(() -> {
            try {
                var value = work.call();
                Platform.runLater(() -> finish(value));
            } catch (Exception | Error e) {
                Platform.runLater(() -> fail());
            }
        }, "sigebox-carga");
        thread.setDaemon(true);
        thread.start();
    }

    private void finish(T value) {
        long elapsedMs = (System.nanoTime() - startedNanos) / 1_000_000;
        var wait = new PauseTransition(Duration.millis(Math.max(0, MIN.toMillis() - elapsedMs)));
        wait.setOnFinished(e -> {
            slow.stop();
            stopMotion();
            onReady.accept(value);
        });
        wait.play();
    }

    private void fail() {
        slow.stop();
        stopMotion();
        tilt.setAngle(0);
        showError(true);
    }

    private void showError(boolean on) {
        error.setVisible(on);
        error.setManaged(on);
        retry.setVisible(on);
        retry.setManaged(on);
        progress.setVisible(!on);
        message.setVisible(!on);
        if (on) {
            retry.requestFocus();
        }
    }

    /** Texto del error a la vista, o {@code null} si no hay error. */
    public String errorText() {
        return error.isVisible() ? error.getText() : null;
    }

    public Button retryButton() {
        return retry;
    }

    /** Animación del balanceo, o {@code null} con movimiento reducido. */
    public Timeline swing() {
        return swing;
    }

    /** Detiene el balanceo (al terminar, al fallar o al salir). */
    public void stopMotion() {
        if (swing != null) {
            swing.stop();
        }
    }
}
