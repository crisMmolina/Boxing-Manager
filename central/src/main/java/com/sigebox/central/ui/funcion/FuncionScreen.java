package com.sigebox.central.ui.funcion;

import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.service.FuncionService.Fila;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.ActionMenu;
import com.sigebox.central.ui.theme.Icons;
import com.sigebox.central.ui.theme.Motion;
import com.sigebox.central.ui.theme.PageHeader;
import com.sigebox.central.ui.theme.Stepper;
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
    private final Ask ask;

    /** @param onStarted se llama al iniciar la siguiente pelea (lleva a emparejar jueces) */
    public FuncionScreen(FuncionFlow flow, Runnable onStarted) {
        this(flow, onStarted, DIALOGS);
    }

    /** Con {@code ask} para "Cambiar rounds y jueces" (las pruebas pasan una respuesta fija). */
    public FuncionScreen(FuncionFlow flow, Runnable onStarted, Ask ask) {
        this.flow = flow;
        this.onStarted = onStarted;
        this.ask = ask;
    }

    public static void register(Screens<Node> screens, FuncionFlow flow, Runnable onStarted) {
        var screen = new FuncionScreen(flow, onStarted);
        screens.register(ID, "Evento", screen::build);
    }

    Node build() {
        var status = Ui.support("");
        var body = new VBox(16);
        var next = Ui.next("Crea el evento.");
        // DESIGN v5 §5.12: encabezado con el evento como contexto y la siguiente pelea como acción principal.
        var header = new PageHeader("Evento");
        var root = new VBox(16, header, next, body, status);
        root.getStyleClass().add("evento-v5");

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
            if (id == null) {
                header.context(null).action(null);
            }
            body.getChildren().setAll(id == null ? create(status, refresh)
                    : show(id, rows, status, refresh, synced, header));
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
        var rounds = stepper(1, 12, 10, "Rounds");
        var judges = stepper(1, 5, 3, "Jueces");
        var fights = stepper(1, FuncionService.MAX_PELEAS, 1, "Peleas");
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
                new HBox(12, limited("Rounds", rounds, 1, 12), limited("Jueces", judges, 1, 5), limited("Peleas", fights, 1, FuncionService.MAX_PELEAS)),
                create);
    }

    private Node show(String id, List<Fila> rows, Label status, Runnable refresh, Runnable synced, PageHeader header) {
        var f = flow.service().funcion(id);
        var nombre = FuncionView.nombre(f); // sin nombre ya es "Evento <fecha>": no se repite la fecha
        header.context(nombre.contains(f.fecha()) ? nombre : nombre + " · " + f.fecha());
        // El botón de iniciar muestra los nombres: se actualiza sin rehacerlo, por si lo están presionando.
        var nextText = new SimpleStringProperty("");
        Runnable renamed = () -> {
            synced.run();
            FuncionView.siguiente(flow.service().cartelera(id)).map(FuncionView::siguienteTexto)
                    .ifPresent(nextText::set);
        };
        // Spec 005: rounds y jueces del evento se pueden cambiar después de crearlo; aplica a las peleas sin empezar.
        var baseRounds = stepper(1, 12, f.baseRounds(), "Rounds del evento");
        var baseJudges = stepper(1, 5, f.baseJueces(), "Jueces del evento");
        baseRounds.valueProperty().addListener((o, a, b) -> act(status, refresh, () -> {
            flow.service().cambiarEvento(id, b.intValue(), null);
            return "Rounds del evento: " + b + ". Cambian las peleas que no han empezado.";
        }));
        baseJudges.valueProperty().addListener((o, a, b) -> act(status, refresh, () -> {
            flow.service().cambiarEvento(id, null, b.intValue());
            return "Jueces del evento: " + b + ". Cambian las peleas que no han empezado.";
        }));
        var base = Ui.card(Ui.cardTitle("Base del evento"),
                new HBox(24, limited("Rounds por pelea", baseRounds, 1, 12), limited("Jueces por pelea", baseJudges, 1, 5)),
                Ui.support("Cambiar la base cambia todas las peleas que no han empezado; las demás quedan igual."));
        base.setSpacing(8);
        var startNext = FuncionView.siguiente(rows).map(n -> startButton(n, status, refresh, nextText)).orElse(null);
        header.action(startNext);

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
        HBox names = FuncionView.editable(r) ? editableNames(id, r, status, refresh, renamed) : fixedNames(r);
        names.setMinWidth(380);
        names.setPrefWidth(380);

        // DESIGN v5 §5.15: una línea por pelea; el estado como chip y las acciones en el menú ⋯.
        var chip = Ui.chip(FuncionView.estado(r.estado()), chipVariant(r), null);
        chip.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        var meta = Ui.support(FuncionView.detalle(r.rounds(), r.jueces())
                + (r.preparada() ? " · en Jueces: sigue la base hasta el round 1 (sus jueces solo bajan)"
                        : r.cambiada() ? " · propia" : ""));
        meta.setMinWidth(150);
        meta.setWrapText(true);

        var row = new HBox(16, number, names, meta, Ui.grow(), chip);
        row.getStyleClass().add("cartelera-row");
        row.setAlignment(Pos.CENTER_LEFT);
        if (FuncionView.editable(r)) {
            var menu = new ActionMenu("Más acciones de la pelea " + r.orden());
            menu.add("Cambiar rounds y jueces", () -> ask.ask(r).ifPresent(v -> act(status, refresh, () -> {
                service.cambiar(id, r.orden(), v[0], v[1]);
                return "Pelea " + r.orden() + ": " + FuncionView.detalle(v[0], v[1]) + ".";
            })));
            var up = menu.add("Subir", () -> act(status, refresh, () -> {
                service.mover(id, r.orden(), r.orden() - 1);
                return "";
            }));
            up.setDisable(r.orden() == 1); // la primera no puede subir
            menu.add("Bajar", () -> act(status, refresh, () -> {
                service.mover(id, r.orden(), r.orden() + 1);
                return "";
            }));
            menu.add("Saltar pelea", () -> act(status, refresh, () -> {
                service.saltar(id, r.orden());
                return "Pelea " + r.orden() + " saltada. Queda en la cartelera.";
            }));
            menu.separator();
            menu.add("Quitar de la cartelera", () -> act(status, refresh, () -> {
                service.quitar(id, r.orden());
                return "Pelea " + r.orden() + " quitada.";
            }));
            row.getChildren().add(menu);
        }
        Motion.lift(row);
        return row;
    }

    /** Chip del estado de una fila: cerrada en verde, en curso en dorado, las demás neutras. */
    private static String chipVariant(Fila r) {
        return switch (r.estado()) {
            case CERRADA -> "success";
            case EN_CURSO -> "pending";
            default -> "neutral";
        };
    }

    private static HBox fixedNames(Fila r) {
        var box = new HBox(8, Ui.cornerDot(true), Ui.body(r.roja()), Ui.support("vs"), Ui.cornerDot(false),
                Ui.body(r.azul()));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** Los nombres de una pelea que no ha empezado: se guardan con Enter o al salir del campo. */
    private HBox editableNames(String id, Fila r, Label status, Runnable refresh, Runnable renamed) {
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
        // Una línea: "● roja  vs  ● azul", cada campo con su etiqueta arriba (DESIGN v4 §5.4).
        var redField = Ui.field("Esquina roja", red);
        var blueField = Ui.field("Esquina azul", blue);
        HBox.setHgrow(redField, Priority.ALWAYS);
        HBox.setHgrow(blueField, Priority.ALWAYS);
        var vs = Ui.support("vs");
        vs.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE); // "vs" en una línea
        var box = new HBox(8, Ui.cornerDot(true), redField, vs, Ui.cornerDot(false), blueField);
        box.setAlignment(Pos.BOTTOM_LEFT);
        HBox.setMargin(vs, new javafx.geometry.Insets(0, 0, 12, 0));
        return box;
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
        // Spec 005: el botón va primero, para que se lea "Agregar pelea: roja, azul".
        add.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        var box = new HBox(12, add, redField, blueField);
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
        // v5: el botón de iniciar vive en el encabezado; aquí queda lo que trae la siguiente pelea.
        box.getChildren().addAll(Ui.grow(), Ui.support("Siguiente: pelea " + next.get().orden() + " · "
                + FuncionView.detalle(next.get().rounds(), next.get().jueces())));
        return box;
    }

    /** "Siguiente pelea: Roja vs Azul", la acción principal del encabezado; sigue los nombres sin rehacerse. */
    private Button startButton(Fila next, Label status, Runnable refresh, StringProperty nextText) {
        nextText.set(FuncionView.siguienteTexto(next));
        var start = Ui.primary(nextText.get());
        start.textProperty().bind(nextText);
        var play = Icons.icon("play", 16);
        play.getStyleClass().add("on-primary");
        start.setGraphic(play);
        start.setOnAction(e -> act(status, refresh, () -> {
            flow.start();
            onStarted.run();
            return "";
        }));
        return start;
    }

    /** Stepper v5 (DESIGN v5 §5.14) con el nombre que dice el lector de pantalla. */
    /** El stepper con su etiqueta y, en cada límite, por qué su botón está apagado (spec 003). */
    private static javafx.scene.layout.VBox limited(String label, Stepper s, int min, int max) {
        var low = Ui.why("Mínimo " + min + ".", s.minusButton());
        var high = Ui.why("Máximo " + max + ".", s.plusButton());
        return new javafx.scene.layout.VBox(4, Ui.field(label, s), low, high);
    }

    private static Stepper stepper(int min, int max, int value, String name) {
        var s = new Stepper(min, max, value);
        s.setName(name);
        return s;
    }

    /** Pide rounds y jueces de una pelea; vacío si se cancela. Las pruebas pasan una respuesta fija. */
    @FunctionalInterface
    public interface Ask {
        java.util.Optional<int[]> ask(Fila fila);
    }

    /** El diálogo de la app: dos steppers con los valores de la fila. */
    public static final Ask DIALOGS = fila -> {
        var rounds = stepper(1, 12, fila.rounds(), "Rounds de la pelea " + fila.orden());
        var judges = stepper(1, 5, fila.jueces(), "Jueces de la pelea " + fila.orden());
        var dialog = new javafx.scene.control.Dialog<javafx.scene.control.ButtonType>();
        dialog.setTitle("Cambiar rounds y jueces");
        dialog.setHeaderText("Pelea " + fila.orden() + ": " + fila.roja() + " vs " + fila.azul());
        var save = new javafx.scene.control.ButtonType("Guardar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.CANCEL, save);
        dialog.getDialogPane().setContent(new HBox(24, Ui.field("Rounds", rounds), Ui.field("Jueces", judges)));
        return dialog.showAndWait().filter(save::equals)
                .map(b -> new int[] {rounds.getValue(), judges.getValue()});
    };

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
