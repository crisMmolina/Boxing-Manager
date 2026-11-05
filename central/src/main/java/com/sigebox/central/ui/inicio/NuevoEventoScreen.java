package com.sigebox.central.ui.inicio;

import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.ui.theme.Ui;
import java.time.LocalDate;
import java.util.Collections;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
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
        var rounds = new Spinner<Integer>(1, 12, 10);
        var judges = new Spinner<Integer>(1, 5, 3);
        var fights = new Spinner<Integer>(1, FuncionService.MAX_PELEAS, 1);
        for (var s : new Spinner<?>[] {rounds, judges, fights}) {
            s.setEditable(true);
        }
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
                Ui.field("Nombre (opcional)", "Noche de box Iztapalapa", name),
                Ui.field("Fecha", date),
                new HBox(12, Ui.field("Rounds por pelea", rounds), Ui.field("Jueces", judges),
                        Ui.field("Número de peleas", fights)),
                Ui.support("Las esquinas se escriben después en la cartelera; cada pelea puede cambiar sus rounds "
                        + "y jueces antes de empezar."),
                create, status);
        var root = new VBox(24, Ui.screenTitle("Nuevo evento"), Ui.next("Escribe los datos y pulsa Crear evento."), card);
        root.setMaxWidth(640);
        return root;
    }
}
