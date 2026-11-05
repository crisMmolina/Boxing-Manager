package com.sigebox.central.ui.funcion;

import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.service.FuncionService.Fila;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.Ui;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Evento (spec 002): la configuración base y la cartelera ordenada con el estado de cada pelea. Antes
 * de que empiece una pelea se puede agregar, quitar, mover, saltar, renombrar y cambiar sus rounds o
 * jueces. Cada cambio se guarda con {@link FuncionService} y la lista se vuelve a leer de lo guardado.
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
        screens.register(ID, "Evento", screen::build);
    }

    Node build() {
        var status = Ui.support("");
        var body = new VBox(16);
        var next = Ui.next("Crea el evento.");
        var root = new VBox(16, Ui.screenTitle("Evento"), next, body, status);

        String[] signature = {null};
        Runnable[] draw = new Runnable[1];
        // Las acciones redibujan siempre (un spinner rechazado debe volver a su valor guardado).
        Runnable refresh = () -> {
            signature[0] = null;
            draw[0].run();
        };
        // Un nombre guardado ya está en pantalla: se anota para que el temporizador no redibuje por él.
        Runnable synced = () -> flow.funcionId()
                .ifPresent(id -> signature[0] = id + "|" + flow.service().cartelera(id));
        draw[0] = () -> {
            var id = flow.funcionId().orElse(null);
            var rows = id == null ? List.<Fila>of() : flow.service().cartelera(id);
            var sig = id + "|" + rows;
            if (sig.equals(signature[0])) {
                return; // sin cambios: no se redibuja (no se pierde el foco)
            }
            signature[0] = sig;
            Ui.next(next, nextStep(id, rows));
            body.getChildren().setAll(id == null ? create(status, refresh) : show(id, rows, status, refresh, synced));
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
        var name = new TextField();
        name.setPromptText("Ej. Velada de mayo");
        var date = new DatePicker(LocalDate.now());
        date.setPromptText("Ej. 01/05/2026");
        var rounds = new Spinner<Integer>(1, 12, 10);
        var judges = new Spinner<Integer>(1, 5, 3);
        var fights = new Spinner<Integer>(1, FuncionService.MAX_PELEAS, 1);
        var create = Ui.primary("Crear evento");
        create.setOnAction(e -> act(status, refresh, () -> {
            flow.service().crear(name.getText(), date.getValue(), rounds.getValue(), judges.getValue(),
                    Collections.nCopies(fights.getValue(), new Esquinas("", "")));
            return "Evento creado. Escribe las esquinas de cada pelea.";
        }));
        return Ui.card(Ui.cardTitle("Nuevo evento"),
                Ui.support("La configuración base sirve para todas las peleas; cada una puede cambiarla antes de "
                        + "empezar."),
                new HBox(12, Ui.field("Nombre del evento", name), Ui.field("Fecha", date)),
                new HBox(12, Ui.field("Rounds", rounds), Ui.field("Jueces", judges), Ui.field("Peleas", fights)),
                create);
    }

    private Node show(String id, List<Fila> rows, Label status, Runnable refresh, Runnable synced) {
        var f = flow.service().funcion(id);
        // El botón de iniciar muestra los nombres: se actualiza sin rehacerlo, por si lo están presionando.
        var nextText = new SimpleStringProperty("");
        Runnable renamed = () -> {
            synced.run();
            FuncionView.siguiente(flow.service().cartelera(id)).map(FuncionView::siguienteTexto)
                    .ifPresent(nextText::set);
        };
        var base = Ui.card(Ui.cardTitle("Base"), new HBox(40, Ui.field("Evento", Ui.body(FuncionView.nombre(f))),
                Ui.field("Fecha", Ui.body(f.fecha())),
                Ui.field("Rounds", Ui.numeral(String.valueOf(f.baseRounds()), "neutral")),
                Ui.field("Jueces", Ui.numeral(String.valueOf(f.baseJueces()), "neutral"))),
                Ui.support("Las peleas heredan esta base salvo que cambies la suya."));
        base.setSpacing(8);

        var list = new VBox(8);
        for (var r : rows) {
            list.getChildren().add(row(id, r, status, refresh, renamed));
        }
        if (rows.isEmpty()) {
            list.getChildren().add(Ui.support("Todavía no hay peleas. Agrega la primera abajo."));
        }
        var cartelera = Ui.card(new HBox(12, Ui.cardTitle("Cartelera"), Ui.grow(), Ui.support(FuncionView.resumen(rows))),
                list, add(id, status, refresh), footer(rows, status, refresh, nextText));
        cartelera.setSpacing(12);
        return new VBox(16, base, cartelera);
    }

    private Node row(String id, Fila r, Label status, Runnable refresh, Runnable renamed) {
        var service = flow.service();
        var number = Ui.numeral(String.valueOf(r.orden()), "neutral");
        number.setMinWidth(36);
        var names = FuncionView.editable(r) ? editableNames(id, r, status, refresh, renamed) : fixedNames(r);
        names.setMinWidth(200);
        names.setPrefWidth(200);

        var pill = Ui.pill(FuncionView.estado(r.estado()));
        pill.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        var row = new HBox(12, number, names);
        row.setAlignment(Pos.CENTER_LEFT);
        if (FuncionView.editable(r)) {
            var rounds = new Spinner<Integer>(1, 12, r.rounds());
            var judges = new Spinner<Integer>(1, 5, r.jueces());
            rounds.setPrefWidth(110);
            judges.setPrefWidth(110);
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
            up.setVisible(r.orden() > 1); // la primera no puede subir; el hueco mantiene las columnas
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
                b.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE); // sin "…" en el texto
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

    private static VBox fixedNames(Fila r) {
        var red = new HBox(6, Ui.cornerDot(true), Ui.body(r.roja()));
        red.setAlignment(Pos.CENTER_LEFT);
        var blue = new HBox(6, Ui.cornerDot(false), Ui.body(r.azul()));
        blue.setAlignment(Pos.CENTER_LEFT);
        return new VBox(2, red, blue);
    }

    /** Los nombres de una pelea que no ha empezado: se guardan con Enter o al salir del campo. */
    private VBox editableNames(String id, Fila r, Label status, Runnable refresh, Runnable renamed) {
        var red = new TextField(r.roja());
        var blue = new TextField(r.azul());
        red.setPromptText("Ej. Ana Pérez");
        blue.setPromptText("Ej. Bea Ruiz");
        red.setAccessibleText("Esquina roja de la pelea " + r.orden());
        blue.setAccessibleText("Esquina azul de la pelea " + r.orden());
        // Lo último enviado tal como se escribió: un nombre recortado al guardar no se reenvía en cada salida.
        String[] saved = {r.roja(), r.azul()};
        Runnable save = () -> {
            var roja = red.getText();
            var azul = blue.getText();
            if (roja.equals(saved[0]) && azul.equals(saved[1])) {
                return;
            }
            try {
                flow.service().renombrar(id, r.orden(), roja, azul);
            } catch (RuntimeException ex) {
                status.setText(ex.getMessage());
                refresh.run(); // vuelve a lo guardado
                return;
            }
            // Sin redibujar: el clic que quitó el foco debe llegar al botón que lo recibió.
            saved[0] = roja;
            saved[1] = azul;
            status.setText("");
            renamed.run();
        };
        for (var field : List.of(red, blue)) {
            field.setOnAction(e -> save.run());
            field.focusedProperty().addListener((o, was, now) -> {
                if (!now && field.getScene() != null) { // al redibujar, el campo ya salió de la pantalla
                    save.run();
                }
            });
        }
        return new VBox(4, Ui.field("Esquina roja", red), Ui.field("Esquina azul", blue));
    }

    private Node add(String id, Label status, Runnable refresh) {
        var red = new TextField();
        var blue = new TextField();
        var add = Ui.secondary("Agregar pelea");
        Runnable go = () -> act(status, refresh, () -> {
            flow.service().agregar(id, new Esquinas(red.getText(), blue.getText()));
            return "Pelea agregada al final de la cartelera.";
        });
        add.setOnAction(e -> go.run());
        blue.setOnAction(e -> go.run());
        var redField = cornerField(true, "Esquina roja", "Ej. Ana Pérez", red);
        var blueField = cornerField(false, "Esquina azul", "Ej. Bea Ruiz", blue);
        HBox.setHgrow(redField, Priority.ALWAYS);
        HBox.setHgrow(blueField, Priority.ALWAYS);
        var box = new HBox(12, redField, blueField, add);
        box.setAlignment(Pos.BOTTOM_LEFT);
        return box;
    }

    /** Campo de una esquina con el punto de su color a la izquierda; la etiqueta queda en el campo mismo. */
    private static HBox cornerField(boolean isRed, String label, String example, TextField input) {
        var field = Ui.field(label, example, input);
        HBox.setHgrow(field, Priority.ALWAYS);
        var box = new HBox(8, Ui.cornerDot(isRed), field);
        box.setAlignment(Pos.BOTTOM_LEFT);
        return box;
    }

    private static String nextStep(String id, List<Fila> rows) {
        if (id == null) {
            return "Crea el evento.";
        }
        if (rows.isEmpty()) {
            return "Agrega la primera pelea.";
        }
        return FuncionView.siguiente(rows).isPresent() ? "Revisa las esquinas e inicia la siguiente pelea."
                : "No quedan peleas por iniciar.";
    }

    private Node footer(List<Fila> rows, Label status, Runnable refresh, StringProperty nextText) {
        var next = FuncionView.siguiente(rows);
        var box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        if (next.isEmpty()) {
            box.getChildren().add(Ui.support(rows.isEmpty() ? "" : "No quedan peleas por iniciar."));
            return box;
        }
        nextText.set(FuncionView.siguienteTexto(next.get()));
        var start = Ui.primary(nextText.get());
        start.textProperty().bind(nextText);
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
