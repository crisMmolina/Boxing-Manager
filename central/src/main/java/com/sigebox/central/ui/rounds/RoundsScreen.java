package com.sigebox.central.ui.rounds;

import com.sigebox.central.service.DescuentoService;
import com.sigebox.central.service.RoundService;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.help.HelpButton;
import com.sigebox.central.ui.termination.TerminarEnVivo;
import com.sigebox.central.ui.theme.Ui;
import com.sigebox.core.model.EstadoRound;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.util.Duration;

/**
 * Tablero en vivo del operador (DESIGN v4 §7.3), en rejilla bento: el round actual en grande, las
 * acciones y el estado de cada juez por round. Sin puntos, totales ni ganador, sin excepción (PROJECT.md
 * principio 3; spec 004 quitó "Ver resumen parcial" y Corrección). Se refresca cada 500 ms, así un envío se ve
 * en ≤ 1 s.
 */
public final class RoundsScreen {

    public static final String ID = "rounds";

    /** Id del botón principal (abrir, cerrar o "Cerrar pelea"), para {@link #firePrimary}. */
    public static final String PRIMARY_ID = "round-primary";

    /** Id de "Terminar pelea" (spec 005: la terminación se hace en En vivo). */
    public static final String END_ID = "fight-end";
    private static final String NO_FIGHT = "Inicia una pelea desde Evento.";

    private static final Duration REFRESH = Duration.millis(500);


    private RoundsScreen() {
    }

    /**
     * Registra la pantalla; {@code fightId} da la pelea activa (o null si no hay) y {@code onFinish} lleva a
     * cerrar la pelea tras el último round.
     */
    public static void register(Screens<Node> screens, RoundService service, Supplier<String> fightId,
            Runnable onFinish) {
        screens.register(ID, "En vivo", () -> build(service, fightId, onFinish));
    }

    /** Igual, con la tarjeta de descuentos. */
    public static void register(Screens<Node> screens, RoundService service, DescuentoService descuentos,
            Supplier<String> fightId, Runnable onFinish) {
        screens.register(ID, "En vivo", () -> build(service, descuentos, DescuentosPanel.DIALOGS, fightId, onFinish));
    }

    /** Con "Terminar pelea" (spec 005): termina y cierra desde aquí; {@code onFinish} sigue llevando a Resultado. */
    public static void register(Screens<Node> screens, RoundService service, Supplier<String> fightId,
            Runnable onFinish, TerminarEnVivo terminar) {
        screens.register(ID, "En vivo", () -> build(service, null, null, fightId, onFinish, terminar));
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
        return build(service, fightId, () -> { });
    }

    static Node build(RoundService service, Supplier<String> fightId, Runnable onFinish) {
        return build(service, null, null, fightId, onFinish);
    }

    static Node build(RoundService service, DescuentoService descuentos, DescuentosPanel.Ask ask,
            Supplier<String> fightId, Runnable onFinish) {
        return build(service, descuentos, ask, fightId, onFinish, null);
    }

    static Node build(RoundService service, DescuentoService descuentos, DescuentosPanel.Ask ask,
            Supplier<String> fightId, Runnable onFinish, TerminarEnVivo terminar) {
        var title = Ui.screenTitle("En vivo");
        // Fila que reparte el ancho: las tarjetas crecen y no cortan nombre ni estado.
        var judges = new HBox(12);
        judges.setFillHeight(false); // cada tarjeta mide lo que trae, sin hueco abajo
        // Sección, no tarjeta: las tarjetas de juez no van dentro de otra tarjeta (v5).
        var cards = new VBox(12, Ui.cardTitle("Jueces"), judges);
        var track = new Arc(88, 88, 82, 82, 90, 360);
        track.setType(ArcType.OPEN);
        track.getStyleClass().add("ring-track");
        var ringArc = new Arc(88, 88, 82, 82, 90, 0);
        ringArc.setType(ArcType.OPEN);
        ringArc.getStyleClass().add("ring-arc");
        var ringCount = new Label();
        ringCount.getStyleClass().add("ring-count");
        var ringLabel = new Label("tarjetas");
        ringLabel.getStyleClass().add("ring-label");
        var ringText = new VBox(0, ringCount, ringLabel);
        ringText.setAlignment(Pos.CENTER);
        var ring = new StackPane(new Group(track, ringArc), ringText);
        ring.getStyleClass().add("cards-ring");
        ring.setPrefSize(176, 176);
        ring.setMinSize(176, 176);
        ring.setMaxSize(176, 176);
        var segments = new HBox(6);
        segments.getStyleClass().add("round-segments");


        var roundTitle = Ui.statBig("");
        var pill = Ui.pill("");
        var sent = Ui.body("");
        var action = Ui.primary("");
        action.setId(PRIMARY_ID);
        action.setMaxWidth(Double.MAX_VALUE);
        var status = Ui.support("");
        var heroText = new VBox(8, roundTitle, pill, sent, segments);
        var heroRow = new HBox(24, ring, heroText);
        heroRow.setAlignment(Pos.CENTER_LEFT);
        var hero = Ui.card(heroRow);
        hero.getStyleClass().add("hero");
        var next = Ui.next(NO_FIGHT);
        var why = Ui.why("Inicia una pelea desde Evento para abrir rounds.", action);
        var actions = Ui.card(Ui.cardTitle("Acciones"), action, why, status);
        var end = Ui.secondary("Terminar pelea");
        end.setId(END_ID);
        end.setMaxWidth(Double.MAX_VALUE);
        end.setDisable(true);
        if (terminar != null) {
            actions.getChildren().add(2, end); // bajo el aviso del botón principal
        }
        var panel = descuentos == null ? null : new DescuentosPanel(descuentos, fightId, ask);
        // Solo se redibuja con otro dato: el refresco no debe quitar el botón de ayuda bajo el cursor.
        var drawn = new RoundService.Live[1];

        Runnable refresh = () -> {
            var f = fightId.get();
            if (f == null) {
                title.setText("En vivo"); // spec 005: el título es siempre el de su sección
                drawn[0] = null;
                Ui.next(next, NO_FIGHT);
                action.setText("Abrir round");
                judges.getChildren().clear();
                segments.getChildren().clear();
                ring.setVisible(false);
                action.setDisable(true);
                why.setText("Inicia una pelea desde Evento para abrir rounds.");
                end.setDisable(true);
                if (panel != null) {
                    panel.clear();
                }
                return;
            }
            var live = service.live(f);
            if (!live.equals(drawn[0])) {
                drawn[0] = live;
                fillLive(live, judges, ring, ringArc, ringCount, segments);
                fillControl(live, roundTitle, pill, sent, action, next, why);
            }
            end.setDisable(terminar == null || !terminar.disponible(f));
            if (panel != null) {
                panel.refresh(live.rounds(), live.current());
            }
            title.setText("En vivo");
        };

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
                    "Faltan los jueces " + missing + ". ¿Cerrar el round igual?", ButtonType.CANCEL, ButtonType.OK);
            if (warn.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
            var word = new TextInputDialog();
            word.setHeaderText("Escribe " + RoundService.CONFIRM_WORD + " para cerrar el round " + n);
            word.showAndWait().ifPresent(text -> run(status, () -> service.closeRound(f, n, text), refresh));
        });

        end.setOnAction(e -> {
            var f = fightId.get();
            if (f != null && terminar != null) {
                var window = end.getScene() == null ? null : end.getScene().getWindow();
                run(status, () -> terminar.terminar(window, f), refresh);
            }
        });

        var timer = new Timeline(new KeyFrame(REFRESH, e -> refresh.run()));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
        refresh.run();

        var body = panel == null ? bento(hero, actions, cards) : bento(hero, actions, cards, panel.node());
        var root = new VBox(20, title, next, body);
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

    /** Rejilla bento: arriba el round actual (ancho) y las acciones (280 px); abajo las tarjetas a todo lo ancho. */
    static GridPane bento(Node hero, Node actions, Node cards) {
        var bento = new GridPane();
        bento.getStyleClass().add("bento");
        var wide = new ColumnConstraints();
        wide.setHgrow(Priority.ALWAYS);
        var side = new ColumnConstraints(260, 280, 320);
        bento.getColumnConstraints().addAll(wide, side);
        bento.add(hero, 0, 0);
        bento.add(actions, 1, 0);
        bento.add(cards, 0, 1, 2, 1);
        for (var n : new Node[] {hero, actions, cards}) {
            GridPane.setFillWidth(n, true);
            GridPane.setFillHeight(n, true);
        }
        bento.setMaxWidth(Double.MAX_VALUE);
        return bento;
    }

    /** La misma rejilla con los descuentos debajo de las tarjetas, a todo lo ancho. */
    static GridPane bento(Node hero, Node actions, Node cards, Node descuentos) {
        var bento = bento(hero, actions, cards);
        bento.add(descuentos, 0, 2, 2, 1);
        GridPane.setFillWidth(descuentos, true);
        GridPane.setFillHeight(descuentos, true);
        return bento;
    }

    private static void fillLive(RoundService.Live live, HBox judges, StackPane ring, Arc ringArc, Label ringCount,
            HBox segments) {
        ring.setVisible(true);
        int n = live.judges().size();
        var current = live.rows().stream().filter(r -> r.round() == live.current()).findFirst().orElse(null);
        int sent = 0;
        if (current != null) {
            for (var c : current.cells()) {
                if (isSent(c)) {
                    sent++;
                }
            }
        }
        ringCount.setText(sent + "/" + n);
        ringArc.setLength(-360.0 * sent / Math.max(n, 1));

        segments.getChildren().clear();
        for (var row : live.rows()) {
            var seg = new Region();
            seg.getStyleClass().addAll("round-segment", row.round() == live.current() ? "current"
                    : row.estado() == EstadoRound.PENDIENTE ? "future" : "done");
            segments.getChildren().add(seg);
        }

        judges.getChildren().clear();
        for (int i = 0; i < n; i++) {
            var j = live.judges().get(i);
            var who = "Juez " + j.seat();
            var name = new Label(j.name() == null ? who : j.name());
            name.getStyleClass().add("judge-name");
            boolean cellSent = current != null && i < current.cells().size() && isSent(current.cells().get(i));
            Node chip;
            if (!j.connected()) {
                chip = Ui.chip("Sin conexión", "neutral", "wifi-off");
            } else if (cellSent) {
                chip = Ui.chip("Enviada", "success", "check");
            } else if (current != null && current.estado() == EstadoRound.ABIERTO) {
                chip = Ui.chip("Calificando…", "pending", "clock");
            } else {
                chip = Ui.chip("En espera", "neutral", null);
            }
            // Avatar y nombre a la izquierda, el estado a la derecha (DESIGN v5 En vivo).
            if (chip instanceof Region r) {
                r.setMinWidth(Region.USE_PREF_SIZE); // el estado nunca se corta
            }
            var seatRow = new HBox(6, Ui.dot(j.connected(), who), Ui.support(who));
            seatRow.setAlignment(Pos.CENTER_LEFT);
            var head = new HBox(10, Ui.avatar(name.getText()), new VBox(2, name, seatRow), Ui.grow(), chip);
            head.setAlignment(Pos.CENTER_LEFT);
            var card = new VBox(8, head);
            card.getStyleClass().add("judge-card");
            card.setMaxWidth(Double.MAX_VALUE);
            card.setMaxHeight(Region.USE_PREF_SIZE);
            HBox.setHgrow(card, Priority.ALWAYS);
            if (!j.connected()) {
                card.getChildren().add(HelpButton.connection(false, who));
            }
            judges.getChildren().add(card);
        }
    }

    private static boolean isSent(RoundService.Cell c) {
        return c.casilla() == RoundService.Casilla.ENVIADO || c.casilla() == RoundService.Casilla.TARDIO
                || c.casilla() == RoundService.Casilla.CORREGIDO;
    }

    /**
     * Spec 005: con un juez sin conectar no se abre el round. Un lugar vacío (sin juez) se resuelve en Jueces o
     * bajando los jueces en Evento; un juez aprobado que se cayó se reconecta solo al abrir SIGEBOX.
     */
    static String missingJudge(RoundService.Live live) {
        for (var j : live.judges()) {
            if (j.connected()) {
                continue;
            }
            return j.name() == null
                    ? "Falta conectar al Juez " + j.seat() + ". Conéctalo en Jueces o baja los jueces en Evento si no"
                            + " va a llegar."
                    : "Falta conectar al Juez " + j.seat() + " · " + j.name() + ". Pídele que abra SIGEBOX en su"
                            + " teléfono: se reconecta solo.";
        }
        return null;
    }

    private static void fillControl(RoundService.Live live, Label title, Label pill, Label sent, Button action,
            Label next, Label why) {
        int n = live.current();
        if (n == 0) {
            title.setText("Rounds terminados");
            pill.setText("Todos cerrados");
            sent.setText("");
            action.setText("Cerrar pelea");
            Ui.next(next, "Cierra la pelea para ver el resultado.");
            action.setDisable(false);
            return;
        }
        title.setText("Round " + n + " de " + live.rounds());
        var row = live.row(n);
        if (row.estado() == EstadoRound.ABIERTO) {
            pill.setText("Round abierto");
            sent.setText("Enviaron " + live.sent(n) + " de " + live.judges().size() + " jueces");
            action.setText("Cerrar round " + n);
            Ui.next(next, "Espera las tarjetas y cierra el round " + n + ".");
        } else {
            pill.setText("Por abrir");
            sent.setText("");
            action.setText("Abrir round " + n);
            var missing = missingJudge(live);
            if (missing != null) {
                why.setText(missing);
                Ui.next(next, "Conecta a todos los jueces para abrir el round " + n + ".");
                action.setDisable(true);
                return;
            }
            Ui.next(next, "Abre el round " + n + ".");
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
