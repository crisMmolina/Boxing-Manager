package com.sigebox.central.ui.rounds;

import com.sigebox.central.service.RoundService;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.Ui;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.Ganador;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
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
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Tablero en vivo del operador (DESIGN v2 §7.2): tarjetas juez × round, fila de totales de los
 * rounds cerrados, "Va ganando" y control del round. Se refresca cada 500 ms, así un envío se ve
 * en ≤ 1 s. Nada de esto llega a los teléfonos.
 */
public final class RoundsScreen {

    public static final String ID = "rounds";

    private static final Duration REFRESH = Duration.millis(500);

    /** Con más rounds futuros que estos, se agrupan en una fila "Pendientes". */
    private static final int FUTURE_ROWS = 3;

    private static final String OPEN_ROW = "-fx-background-color: rgba(20,40,75,0.05);";

    private RoundsScreen() {
    }

    /** Registra la pantalla; {@code fightId} da la pelea activa (o null si no hay). */
    public static void register(Screens<Node> screens, RoundService service, Supplier<String> fightId) {
        screens.register(ID, "En vivo", () -> build(service, fightId));
    }

    static Node build(RoundService service, Supplier<String> fightId) {
        var title = Ui.screenTitle("En vivo");
        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(4);
        var cards = Ui.glass(Ui.cardTitle("Tarjetas"), grid);
        HBox.setHgrow(cards, Priority.ALWAYS);

        var leader = Ui.numeral("—", "neutral");
        leader.getStyleClass().add("big");
        var leaderNote = Ui.support("Sin rounds cerrados");
        var leaderCard = Ui.glass(Ui.cardTitle("Va ganando"), leader, leaderNote);

        var roundTitle = Ui.cardTitle("");
        var pill = Ui.pill("");
        var sent = Ui.support("");
        var action = Ui.primary("");
        action.setMaxWidth(Double.MAX_VALUE);
        var status = Ui.support("");
        var control = Ui.glass(roundTitle, pill, sent, action, status);

        var side = new VBox(16, leaderCard, control);
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
            fillLeader(leader, leaderNote, live.leader());
            fillControl(live, roundTitle, pill, sent, action);
            title.setText("En vivo");
        };

        action.setOnAction(e -> {
            var f = fightId.get();
            if (f == null) {
                return;
            }
            int n = service.currentRound(f);
            if (n == 0) {
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

        grid.add(Ui.cardTitle("Total"), 0, line);
        col = 1;
        for (var t : live.totals()) {
            grid.add(pair(t.red(), t.blue()), col++, line);
        }
    }

    private static Node score(RoundService.Cell c) {
        if (c.hasScore()) {
            var p = pair(c.red(), c.blue());
            if (c.casilla() == RoundService.Casilla.TARDIO || c.casilla() == RoundService.Casilla.CORREGIDO) {
                var box = new VBox(0, p, Ui.support(texto(c.casilla())));
                box.setAlignment(Pos.CENTER);
                return box;
            }
            return p;
        }
        return c.casilla() == null ? Ui.support("—") : Ui.support(texto(c.casilla()));
    }

    private static HBox pair(int red, int blue) {
        var box = new HBox(4, Ui.numeral(String.valueOf(red), "red"), Ui.numeral("–", "neutral"),
                Ui.numeral(String.valueOf(blue), "blue"));
        box.setAlignment(Pos.CENTER);
        return box;
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

    private static void fillLeader(Label leader, Label note, RoundService.Leader l) {
        leader.getStyleClass().removeAll("red", "blue", "neutral");
        if (l == null) {
            leader.setText("—");
            leader.getStyleClass().add("neutral");
            note.setText("Sin rounds cerrados");
            return;
        }
        switch (l.ganador()) {
            case ROJA -> {
                leader.setText("Roja");
                leader.getStyleClass().add("red");
            }
            case AZUL -> {
                leader.setText("Azul");
                leader.getStyleClass().add("blue");
            }
            default -> {
                leader.setText("Empate");
                leader.getStyleClass().add("neutral");
            }
        }
        note.setText(l.ganador() == Ganador.EMPATE
                ? "Con los rounds cerrados"
                : l.aFavor() + " de " + l.jueces() + " jueces a favor");
    }

    private static void fillControl(RoundService.Live live, Label title, Label pill, Label sent, Button action) {
        int n = live.current();
        if (n == 0) {
            title.setText("Rounds terminados");
            pill.setText("Todos cerrados");
            sent.setText("");
            action.setText("Sin round por abrir");
            action.setDisable(true);
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
