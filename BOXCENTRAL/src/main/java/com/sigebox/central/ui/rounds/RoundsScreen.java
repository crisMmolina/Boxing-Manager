package com.sigebox.central.ui.rounds;

import com.sigebox.central.service.RoundService;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.Ui;
import com.sigebox.core.model.EstadoRound;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Tablero en vivo del operador (DESIGN v2 §7.2): estado de cada juez por round, control del round y
 * "Ver resumen parcial" discreto. Sin puntos, totales ni ganador (PROJECT.md principio 3). Se
 * refresca cada 500 ms, así un envío se ve en ≤ 1 s.
 */
public final class RoundsScreen {

    public static final String ID = "rounds";

    /** Id del botón principal (abrir, cerrar o "Cerrar pelea"), para {@link #firePrimary}. */
    public static final String PRIMARY_ID = "round-primary";

    private static final Duration REFRESH = Duration.millis(500);

    /** Con más rounds futuros que estos, se agrupan en una fila "Pendientes". */
    private static final int FUTURE_ROWS = 3;

    private static final String OPEN_ROW = "-fx-background-color: rgba(20,40,75,0.05);";

    private RoundsScreen() {
    }

    /**
     * Registra la pantalla; {@code fightId} da la pelea activa (o null si no hay). {@code onCorrect}
     * lleva a corregir un round y {@code onFinish} a cerrar la pelea tras el último round.
     */
    public static void register(Screens<Node> screens, RoundService service, Supplier<String> fightId,
            Runnable onCorrect, Runnable onFinish) {
        screens.register(ID, "En vivo", () -> build(service, fightId, onCorrect, onFinish));
    }

    /** {@code Ctrl+Enter}: pulsa el botón principal del tablero dentro de {@code scope}, si está. */
    public static boolean firePrimary(Node scope) {
        if (scope.lookup("#" + PRIMARY_ID) instanceof Button b && !b.isDisabled()) {
            b.fire();
            return true;
        }
        return false;
    }

    static Node build(RoundService service, Supplier<String> fightId) {
        return build(service, fightId, () -> { }, () -> { });
    }

    static Node build(RoundService service, Supplier<String> fightId, Runnable onCorrect, Runnable onFinish) {
        var title = Ui.screenTitle("En vivo");
        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(4);
        var cards = Ui.glass(Ui.cardTitle("Tarjetas"), grid);
        HBox.setHgrow(cards, Priority.ALWAYS);

        var summary = Ui.secondary("Ver resumen parcial");
        summary.setMaxWidth(Double.MAX_VALUE);
        var summaryBox = new VBox(6);
        var summaryNote = Ui.support("Totales de los rounds cerrados por " + RoundService.SUMMARY_VISIBLE_SECONDS
                + " s. Cada vista queda en la bitácora.");
        summaryNote.setMinHeight(Region.USE_PREF_SIZE);
        var summaryCard = Ui.glass(Ui.cardTitle("Resumen parcial"), summaryNote, summary, summaryBox);

        var roundTitle = Ui.cardTitle("");
        var pill = Ui.pill("");
        var sent = Ui.support("");
        var action = Ui.primary("");
        action.setId(PRIMARY_ID);
        action.setMaxWidth(Double.MAX_VALUE);
        var correct = Ui.secondary("Corregir un round");
        correct.setMaxWidth(Double.MAX_VALUE);
        correct.setOnAction(e -> onCorrect.run());
        var status = Ui.support("");
        var control = Ui.glass(roundTitle, pill, sent, action, correct, status);

        var side = new VBox(16, control, summaryCard);
        side.setMinWidth(260);
        side.setPrefWidth(280);

        Runnable refresh = () -> {
            var f = fightId.get();
            if (f == null) {
                title.setText("Sin pelea activa");
                grid.getChildren().clear();
                action.setDisable(true);
                return;
            }
            var live = service.live(f);
            fillGrid(grid, live);
            fillControl(live, roundTitle, pill, sent, action);
            title.setText("En vivo");
        };

        summary.setOnAction(e -> {
            var f = fightId.get();
            if (f == null) {
                return;
            }
            var ask = new Alert(Alert.AlertType.CONFIRMATION, "El resumen parcial queda registrado en la bitácora. "
                    + "Mostrar " + RoundService.SUMMARY_VISIBLE_SECONDS + " s?", ButtonType.CANCEL, ButtonType.OK);
            if (ask.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
            showSummary(summaryBox, summary, service.partialSummary(f, true));
        });

        action.setOnAction(e -> {
            var f = fightId.get();
            if (f == null) {
                return;
            }
            int n = service.currentRound(f);
            if (n == 0) {
                onFinish.run(); // "Cerrar pelea": el resultado se cierra en su pantalla
                return;
            }
            var board = service.board(f, n);
            if (board.estado() == EstadoRound.PENDIENTE) {
                run(status, () -> service.openRound(f, n), refresh);
                return;
            }
            var missing = board.missing();
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

        var timer = new Timeline(new KeyFrame(REFRESH, e -> refresh.run()));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
        refresh.run();

        var body = new HBox(16, cards, side);
        body.setAlignment(Pos.TOP_CENTER);
        var root = new VBox(20, title, body);
        root.getStyleClass().add("screen");
        root.setPadding(new Insets(24));
        // Detener el refresco cuando la pantalla deja de estar en la escena.
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                timer.stop();
            }
        });
        return root;
    }

    private static void fillGrid(GridPane grid, RoundService.Live live) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getColumnConstraints().add(new ColumnConstraints(72));
        for (int i = 0; i < live.judges().size(); i++) {
            var c = new ColumnConstraints();
            c.setHgrow(Priority.ALWAYS);
            c.setHalignment(HPos.CENTER);
            grid.getColumnConstraints().add(c);
        }

        grid.add(Ui.support("Round"), 0, 0);
        int col = 1;
        for (var j : live.judges()) {
            var who = "Juez " + j.seat();
            var head = new HBox(6, Ui.dot(j.connected(), who), Ui.body(j.name() == null ? who : who + " · " + j.name()));
            head.setAlignment(Pos.CENTER);
            grid.add(head, col++, 0);
        }

        int line = 1;
        int lastShown = live.rows().size();
        long future = live.rows().stream().filter(r -> r.estado() == EstadoRound.PENDIENTE).count();
        if (future > FUTURE_ROWS) {
            lastShown -= (int) future;
        }
        for (var row : live.rows().subList(0, lastShown)) {
            boolean open = row.estado() == EstadoRound.ABIERTO;
            grid.add(cell(Ui.body(String.valueOf(row.round())), open), 0, line);
            col = 1;
            for (var c : row.cells()) {
                grid.add(cell(score(c), open), col++, line);
            }
            line++;
        }
        if (lastShown < live.rows().size()) {
            int from = lastShown + 1;
            grid.add(Ui.support("Pendientes: rounds " + from + "–" + live.rows().size()), 0, line++,
                    live.judges().size() + 1, 1);
        }
    }

    private static Node score(RoundService.Cell c) {
        return Ui.support(c.casilla() == null ? "—" : texto(c.casilla()));
    }

    /** Muestra los totales parciales y los oculta pasados {@link RoundService#SUMMARY_VISIBLE_SECONDS}. */
    static void showSummary(VBox box, Button button, java.util.List<RoundService.PartialTotal> totals) {
        box.getChildren().setAll(totals.stream().map(t -> {
            var who = "Juez " + t.seat() + (t.judgeName() == null ? "" : " · " + t.judgeName());
            var numbers = new HBox(4, Ui.numeral(String.valueOf(t.red()), "red"), Ui.numeral("–", "neutral"),
                    Ui.numeral(String.valueOf(t.blue()), "blue"));
            numbers.setMinWidth(Region.USE_PREF_SIZE); // el nombre se recorta, los totales nunca
            var name = Ui.body(who);
            name.setWrapText(false);
            name.setMinWidth(0);
            var row = new HBox(6, name, Ui.grow(), numbers);
            row.setAlignment(Pos.CENTER_LEFT);
            return (Node) row;
        }).toList());
        button.setDisable(true);
        var hide = new PauseTransition(Duration.seconds(RoundService.SUMMARY_VISIBLE_SECONDS));
        hide.setOnFinished(x -> {
            box.getChildren().clear();
            button.setDisable(false);
        });
        hide.play();
    }

    private static Node cell(Node content, boolean open) {
        var box = new HBox(content);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(6, 8, 6, 8));
        box.setMaxWidth(Double.MAX_VALUE);
        if (open) {
            box.setStyle(OPEN_ROW);
        }
        return box;
    }

    private static void fillControl(RoundService.Live live, Label title, Label pill, Label sent, Button action) {
        int n = live.current();
        if (n == 0) {
            title.setText("Rounds terminados");
            pill.setText("Todos cerrados");
            sent.setText("");
            action.setText("Cerrar pelea");
            action.setDisable(false);
            return;
        }
        title.setText("Round " + n + " de " + live.rounds());
        var row = live.row(n);
        if (row.estado() == EstadoRound.ABIERTO) {
            pill.setText("Round abierto");
            sent.setText("Enviaron " + live.sent(n) + " de " + live.judges().size() + " jueces");
            action.setText("Cerrar round " + n);
        } else {
            pill.setText("Por abrir");
            sent.setText("");
            action.setText("Abrir round " + n);
        }
        action.setDisable(false);
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
