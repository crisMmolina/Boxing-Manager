package com.sigebox.central.ui.funcion;

import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.service.FuncionService.Fila;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.Ui;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Función (spec 002): la configuración base y la cartelera ordenada con el estado de cada pelea. Antes
 * de que empiece una pelea se puede agregar, quitar, mover, saltar y cambiar sus rounds o jueces. Cada
 * cambio se guarda con {@link FuncionService} y la lista se vuelve a leer de lo guardado.
 */
public final class FuncionScreen {

    public static final String ID = "funcion";

    private final FuncionFlow flow;
    private final Runnable onStarted;

    /** @param onStarted se llama al iniciar la siguiente pelea (lleva a emparejar jueces) */
    public FuncionScreen(FuncionFlow flow, Runnable onStarted) {
        this.flow = flow;
        this.onStarted = onStarted;
    }

    public static void register(Screens<Node> screens, FuncionFlow flow, Runnable onStarted) {
        var screen = new FuncionScreen(flow, onStarted);
        screens.register(ID, "Función", screen::build);
    }

    Node build() {
        var status = Ui.support("");
        var body = new VBox(16);
        var root = new VBox(16, Ui.screenTitle("Función"), body, status);

        String[] signature = {null};
        Runnable[] draw = new Runnable[1];
        // Las acciones redibujan siempre (un spinner rechazado debe volver a su valor guardado).
        Runnable refresh = () -> {
            signature[0] = null;
            draw[0].run();
        };
        draw[0] = () -> {
            var id = flow.funcionId().orElse(null);
            var rows = id == null ? List.<Fila>of() : flow.service().cartelera(id);
            var sig = id + "|" + rows;
            if (sig.equals(signature[0])) {
                return; // sin cambios: no se redibuja (no se pierde el foco)
            }
            signature[0] = sig;
            body.getChildren().setAll(id == null ? create(status, refresh) : show(id, rows, status, refresh));
        };

        var timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> draw[0].run()));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                timer.stop();
            }
        });
        draw[0].run();
        return root;
    }

    private Node create(Label status, Runnable refresh) {
        var rounds = new Spinner<Integer>(1, 12, 10);
        var judges = new Spinner<Integer>(1, 5, 3);
        var create = Ui.primary("Crear función");
        create.setOnAction(e -> act(status, refresh, () -> {
            flow.service().crear(rounds.getValue(), judges.getValue(), List.of());
            return "Función creada. Agrega las peleas en el orden en que van.";
        }));
        return Ui.card(Ui.cardTitle("Nueva función"),
                Ui.support("La configuración base sirve para todas las peleas; cada una puede cambiarla antes de "
                        + "empezar."),
                new HBox(12, Ui.field("Rounds", rounds), Ui.field("Jueces", judges)), create);
    }

    private Node show(String id, List<Fila> rows, Label status, Runnable refresh) {
        var f = flow.service().funcion(id);
        var base = Ui.card(Ui.cardTitle("Base"), Ui.field("Fecha", Ui.body(f.fecha())),
                Ui.field("Rounds", Ui.numeral(String.valueOf(f.baseRounds()), "neutral")),
                Ui.field("Jueces", Ui.numeral(String.valueOf(f.baseJueces()), "neutral")),
                Ui.support("Las peleas heredan esta base salvo que cambies la suya."));
        base.setPrefWidth(240);
        base.setMinWidth(220);

        var list = new VBox(8);
        for (var r : rows) {
            list.getChildren().add(row(id, r, status, refresh));
        }
        if (rows.isEmpty()) {
            list.getChildren().add(Ui.support("Todavía no hay peleas. Agrega la primera abajo."));
        }
        var cartelera = Ui.card(new HBox(12, Ui.cardTitle("Cartelera"), Ui.grow(), Ui.support(FuncionView.resumen(rows))),
                list, add(id, status, refresh), footer(rows, status, refresh));
        cartelera.setSpacing(12);
        HBox.setHgrow(cartelera, Priority.ALWAYS);
        var both = new HBox(24, base, cartelera);
        both.setAlignment(Pos.TOP_LEFT);
        return both;
    }

    private Node row(String id, Fila r, Label status, Runnable refresh) {
        var service = flow.service();
        var number = Ui.numeral(String.valueOf(r.orden()), "neutral");
        number.setMinWidth(36);
        var red = new HBox(6, Ui.cornerDot(true), Ui.body(r.roja()));
        red.setAlignment(Pos.CENTER_LEFT);
        var blue = new HBox(6, Ui.cornerDot(false), Ui.body(r.azul()));
        blue.setAlignment(Pos.CENTER_LEFT);
        var names = new VBox(2, red, blue);
        names.setMinWidth(200);
        names.setPrefWidth(240);

        var pill = Ui.pill(FuncionView.estado(r.estado()));
        pill.setMinWidth(90);

        var row = new HBox(12, number, names);
        row.setAlignment(Pos.CENTER_LEFT);
        if (FuncionView.editable(r)) {
            var rounds = new Spinner<Integer>(1, 12, r.rounds());
            var judges = new Spinner<Integer>(1, 5, r.jueces());
            rounds.setPrefWidth(84);
            judges.setPrefWidth(84);
            rounds.setAccessibleText("Rounds de la pelea " + r.orden());
            judges.setAccessibleText("Jueces de la pelea " + r.orden());
            rounds.valueProperty().addListener((o, a, b) -> act(status, refresh, () -> {
                service.cambiar(id, r.orden(), b, null);
                return "";
            }));
            judges.valueProperty().addListener((o, a, b) -> act(status, refresh, () -> {
                service.cambiar(id, r.orden(), null, b);
                return "";
            }));
            var up = Ui.secondary("Subir");
            up.setDisable(r.orden() == 1);
            up.setOnAction(e -> act(status, refresh, () -> {
                service.mover(id, r.orden(), r.orden() - 1);
                return "";
            }));
            var down = Ui.secondary("Bajar");
            down.setOnAction(e -> act(status, refresh, () -> {
                service.mover(id, r.orden(), r.orden() + 1);
                return "";
            }));
            var skip = Ui.secondary("Saltar");
            skip.setOnAction(e -> act(status, refresh, () -> {
                service.saltar(id, r.orden());
                return "Pelea " + r.orden() + " saltada. Queda en la cartelera.";
            }));
            var remove = Ui.secondary("Quitar");
            remove.setOnAction(e -> act(status, refresh, () -> {
                service.quitar(id, r.orden());
                return "Pelea " + r.orden() + " quitada.";
            }));
            for (var b : new Button[] {up, down, skip, remove}) {
                b.setAccessibleText(b.getText() + " la pelea " + r.orden());
            }
            row.getChildren().addAll(Ui.field("Rounds", rounds), Ui.field("Jueces", judges), Ui.grow(), pill, up, down,
                    skip, remove);
        } else {
            var detail = Ui.support(FuncionView.detalle(r.rounds(), r.jueces()) + (r.cambiada() ? " · cambiada" : ""));
            row.getChildren().addAll(detail, Ui.grow(), pill);
        }
        row.setPadding(new javafx.geometry.Insets(6, 0, 6, 0));
        return row;
    }

    private Node add(String id, Label status, Runnable refresh) {
        var red = new TextField();
        red.setPromptText("Esquina roja");
        var blue = new TextField();
        blue.setPromptText("Esquina azul");
        var add = Ui.secondary("Agregar pelea");
        Runnable go = () -> act(status, refresh, () -> {
            flow.service().agregar(id, new Esquinas(red.getText(), blue.getText()));
            return "Pelea agregada al final de la cartelera.";
        });
        add.setOnAction(e -> go.run());
        blue.setOnAction(e -> go.run());
        HBox.setHgrow(red, Priority.ALWAYS);
        HBox.setHgrow(blue, Priority.ALWAYS);
        var box = new HBox(12, new HBox(8, Ui.cornerDot(true), red), new HBox(8, Ui.cornerDot(false), blue), add);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Node footer(List<Fila> rows, Label status, Runnable refresh) {
        var next = FuncionView.siguiente(rows);
        var box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        if (next.isEmpty()) {
            box.getChildren().add(Ui.support(rows.isEmpty() ? "" : "No quedan peleas por iniciar."));
            return box;
        }
        var start = Ui.primary(FuncionView.siguienteTexto(next.get()));
        start.setOnAction(e -> act(status, refresh, () -> {
            flow.start();
            onStarted.run();
            return "";
        }));
        box.getChildren().addAll(Ui.grow(), Ui.support(FuncionView.detalle(next.get().rounds(), next.get().jueces())),
                start);
        return box;
    }

    /** Ejecuta un cambio; muestra el mensaje o el error y vuelve a leer la cartelera guardada. */
    private static void act(Label status, Runnable refresh, java.util.function.Supplier<String> change) {
        try {
            status.setText(change.get());
        } catch (RuntimeException ex) {
            status.setText(ex.getMessage());
        }
        refresh.run();
    }
}
