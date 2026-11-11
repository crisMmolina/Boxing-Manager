package com.sigebox.central.ui.inicio;

import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.ui.theme.PageHeader;
import com.sigebox.central.ui.theme.Stepper;
import com.sigebox.central.ui.theme.Ui;
import java.time.LocalDate;
import java.util.Collections;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * "Nuevo evento" desde Inicio (DESIGN v4 §7.1): campos con etiqueta arriba y "Crear evento". Las esquinas
 * quedan vacías ("Esquina roja N"); se escriben después en la cartelera.
 */
public final class NuevoEventoScreen {

    public static final String ID = "nuevo-evento";

    private final FuncionService funciones;
    private final Consumer<String> onCreated;

    /** @param onCreated recibe el id del evento creado */
    public NuevoEventoScreen(FuncionService funciones, Consumer<String> onCreated) {
        this.funciones = funciones;
        this.onCreated = onCreated;
    }

    public Node build() {
        var name = new TextField();
        var date = new DatePicker(LocalDate.now());
        // DESIGN v5 §5.14: steppers en lugar de Spinner.
        var rounds = stepper(1, 12, 10, "Rounds por pelea");
        var judges = stepper(1, 5, 3, "Jueces");
        var fights = stepper(1, FuncionService.MAX_PELEAS, 1, "Número de peleas");
        var status = Ui.support("");
        var create = Ui.primary("Crear evento");
        create.setOnAction(e -> {
            try {
                var f = funciones.crear(name.getText(), date.getValue(), rounds.getValue(), judges.getValue(),
                        Collections.nCopies(fights.getValue(), new Esquinas("", "")));
                onCreated.accept(f.id());
            } catch (IllegalArgumentException | IllegalStateException ex) {
                status.setText("No se pudo crear el evento. Revisa la fecha y los números.");
            }
        });
        var card = Ui.card(Ui.cardTitle("Datos del evento"),
                Ui.field("Nombre (opcional)", name),
                Ui.field("Fecha", date),
                new HBox(12, limited("Rounds por pelea", rounds, 1, 12), limited("Jueces", judges, 1, 5),
                        limited("Número de peleas", fights, 1, FuncionService.MAX_PELEAS)),
                Ui.support("Las esquinas se escriben después en la cartelera; cada pelea puede cambiar sus rounds "
                        + "y jueces antes de empezar."),
                create, status);
        var root = new VBox(24, new PageHeader("Nuevo evento").context("Inicio"),
                Ui.next("Escribe los datos y pulsa Crear evento."), card);
        root.setMaxWidth(640);
        return root;
    }

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
}
