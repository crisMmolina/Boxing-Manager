package com.sigebox.central.ui.rounds;

import com.sigebox.central.service.RoundService;
import com.sigebox.central.ui.Screens;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Pantalla de control de rounds: abrir/cerrar, casilla por juez (se refresca cada 500 ms, así un
 * envío se ve en ≤ 1 s) y "Ver resumen parcial" discreto. Nunca muestra totales en el tablero.
 */
public final class RoundsScreen {

    public static final String ID = "rounds";

    private static final Duration REFRESH = Duration.millis(500);

    private RoundsScreen() {
    }

    /** Registra la pantalla; {@code fightId} da la pelea activa (o null si no hay). */
    public static void register(Screens<Node> screens, RoundService service, Supplier<String> fightId) {
        screens.register(ID, "Rounds", () -> build(service, fightId));
    }

    static Node build(RoundService service, Supplier<String> fightId) {
        var title = new Label();
        title.getStyleClass().add("screen-title");
        var status = new Label();
        var slots = new FlowPane(12, 12);
        var open = new Button("Abrir round");
        var close = new Button("Cerrar round");
        var summary = new Button("Ver resumen parcial");
        summary.getStyleClass().add("discreet");
        var summaryBox = new Label();
        summaryBox.setVisible(false);

        Runnable refresh = () -> {
            var f = fightId.get();
            if (f == null) {
                title.setText("Sin pelea activa");
                slots.getChildren().clear();
                open.setDisable(true);
                close.setDisable(true);
                return;
            }
            int n = service.currentRound(f);
            if (n == 0) {
                title.setText("Todos los rounds cerrados");
                slots.getChildren().clear();
                open.setDisable(true);
                close.setDisable(true);
                return;
            }
            var board = service.board(f, n);
            title.setText("Round " + n + " · " + board.estado());
            open.setDisable(board.estado() != com.sigebox.core.model.EstadoRound.PENDIENTE);
            close.setDisable(board.estado() != com.sigebox.core.model.EstadoRound.ABIERTO);
            slots.getChildren().setAll(board.slots().stream().map(RoundsScreen::slot).toList());
        };

        open.setOnAction(e -> run(status, () -> service.openRound(fightId.get(), service.currentRound(fightId.get())), refresh));
        close.setOnAction(e -> {
            var f = fightId.get();
            int n = service.currentRound(f);
            var missing = service.board(f, n).missing();
            if (missing.isEmpty()) {
                run(status, () -> service.closeRound(f, n, null), refresh);
                return;
            }
            // Doble confirmación: aviso + escribir la palabra.
            var warn = new Alert(Alert.AlertType.CONFIRMATION,
                    "Faltan los jueces " + missing + ". Cerrar el round igual?", ButtonType.CANCEL, ButtonType.OK);
            if (warn.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
            var word = new TextInputDialog();
            word.setHeaderText("Escribe " + RoundService.CONFIRM_WORD + " para cerrar el round " + n);
            word.showAndWait().ifPresent(text -> run(status, () -> service.closeRound(f, n, text), refresh));
        });
        summary.setOnAction(e -> {
            var f = fightId.get();
            if (f == null) {
                return;
            }
            var ask = new Alert(Alert.AlertType.CONFIRMATION,
                    "El resumen parcial queda registrado en la bitácora. Mostrar 15 s?", ButtonType.CANCEL, ButtonType.OK);
            if (ask.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
            var lines = new StringBuilder();
            for (var t : service.partialSummary(f, true)) {
                lines.append("Juez ").append(t.seat())
                        .append(t.judgeName() == null ? "" : " · " + t.judgeName())
                        .append(": ").append(t.red()).append(" – ").append(t.blue()).append('\n');
            }
            summaryBox.setText(lines.toString().strip());
            summaryBox.setVisible(true);
            var hide = new PauseTransition(Duration.seconds(RoundService.SUMMARY_VISIBLE_SECONDS));
            hide.setOnFinished(x -> {
                summaryBox.setVisible(false);
                summaryBox.setText("");
            });
            hide.play();
        });

        var timer = new Timeline(new KeyFrame(REFRESH, e -> refresh.run()));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
        refresh.run();

        var root = new VBox(16, title, slots, new HBox(12, open, close), status, summary, summaryBox);
        root.setPadding(new Insets(24));
        // Detener el refresco cuando la pantalla deja de estar en la escena.
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                timer.stop();
            }
        });
        return root;
    }

    private static Node slot(RoundService.Slot s) {
        var name = "Juez " + s.seat() + (s.judgeName() == null ? "" : " · " + s.judgeName());
        var box = new VBox(4, new Label(name), new Label(texto(s.casilla())));
        box.getStyleClass().addAll("slot", "slot-" + s.casilla().name().toLowerCase());
        box.setPadding(new Insets(12));
        return box;
    }

    static String texto(RoundService.Casilla c) {
        return switch (c) {
            case PENDIENTE -> "Pendiente";
            case ENVIADO -> "Enviado";
            case TARDIO -> "Tardío";
            case CORREGIDO -> "Corregido";
        };
    }

    private static void run(Label status, Runnable action, Runnable refresh) {
        try {
            action.run();
            status.setText("");
        } catch (RuntimeException ex) {
            status.setText(ex.getMessage());
        }
        refresh.run();
    }
}
