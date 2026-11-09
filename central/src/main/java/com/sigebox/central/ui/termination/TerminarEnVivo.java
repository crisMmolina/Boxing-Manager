package com.sigebox.central.ui.termination;

import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.TerminationService;
import com.sigebox.central.ui.Labels;
import com.sigebox.central.ui.theme.Ui;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.MotivoTerminacion;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * "Terminar pelea" en En vivo (spec 005, DSK-036): pide solo el motivo (y la esquina o la razón si el motivo las
 * pide); el round y el tiempo los pone la central. Al confirmar, la pelea queda terminada y cerrada con su resultado,
 * y {@code alCerrar} lleva a Resultado.
 */
public final class TerminarEnVivo {

    /** Lo que eligió el organizador. */
    public record Eleccion(MotivoTerminacion motivo, String textoOtro, Esquina ganadora) {}

    /** Pide el motivo y confirma; vacío si se canceló. Las pruebas pasan una respuesta fija. */
    @FunctionalInterface
    public interface Ask {
        Optional<Eleccion> ask(Window owner, TerminationService.Punto punto);
    }

    private final TerminationService terminaciones;
    private final ResultService resultados;
    private final Ask ask;
    private final Consumer<String> alCerrar;

    public TerminarEnVivo(TerminationService terminaciones, ResultService resultados, Ask ask,
            Consumer<String> alCerrar) {
        this.terminaciones = terminaciones;
        this.resultados = resultados;
        this.ask = ask;
        this.alCerrar = alCerrar;
    }

    /** Si "Terminar pelea" se puede usar con la pelea {@code fightId}. */
    public boolean disponible(String fightId) {
        return fightId != null && terminaciones.sePuedeTerminar(fightId);
    }

    /** Pide el motivo; si se confirma, termina, cierra y avisa con {@code alCerrar}. Un error sale como excepción. */
    public void terminar(Window owner, String fightId) {
        var eleccion = ask.ask(owner, terminaciones.punto(fightId)).orElse(null);
        if (eleccion == null) {
            return;
        }
        terminaciones.terminarYCerrar(fightId, eleccion.motivo(), eleccion.textoOtro(), eleccion.ganadora(),
                resultados);
        alCerrar.accept(fightId);
    }

    /** El diálogo de la app: motivo, esquina y razón, y después una confirmación. */
    public static final Ask DIALOGS = (owner, punto) -> {
        var motivo = new ComboBox<>(FXCollections.observableArrayList(MotivoTerminacion.values()));
        motivo.setCellFactory(l -> new NameCell<>(TerminationService::label));
        motivo.setButtonCell(new NameCell<>(TerminationService::label));
        var esquina = new ComboBox<>(FXCollections.observableArrayList(Esquina.values()));
        esquina.setCellFactory(l -> new NameCell<>(Labels::of));
        esquina.setButtonCell(new NameCell<>(Labels::of));
        var otro = new TextField();

        var dialog = new Dialog<ButtonType>();
        dialog.initOwner(owner);
        dialog.setTitle("Terminar pelea");
        dialog.setHeaderText("Se termina en el round " + punto.round() + " (" + punto.tiempo()
                + ") y se cierra con su resultado.");
        var terminar = new ButtonType("Terminar pelea", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, terminar);
        dialog.getDialogPane().setContent(new VBox(12, Ui.field("Motivo", motivo),
                Ui.field("Esquina ganadora", esquina), Ui.field("Razón (obligatoria con Otro)", "Ej. Corte de luz", otro)));
        var ok = dialog.getDialogPane().lookupButton(terminar);
        Runnable check = () -> {
            var m = motivo.getValue();
            esquina.setDisable(m == null || !TerminationService.requiresCorner(m) && m != MotivoTerminacion.OTRO);
            otro.setDisable(m != MotivoTerminacion.OTRO);
            ok.setDisable(m == null || TerminationService.requiresCorner(m) && esquina.getValue() == null
                    || m == MotivoTerminacion.OTRO && otro.getText().isBlank());
        };
        motivo.valueProperty().addListener((o, a, b) -> check.run());
        esquina.valueProperty().addListener((o, a, b) -> check.run());
        otro.textProperty().addListener((o, a, b) -> check.run());
        check.run();
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != terminar) {
            return Optional.empty();
        }
        var m = motivo.getValue();
        var g = esquina.isDisabled() ? null : esquina.getValue();
        var confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Terminar la pelea por " + TerminationService.label(m)
                + " en el round " + punto.round() + "? Se cierra con su resultado y no se puede deshacer.",
                ButtonType.CANCEL, ButtonType.OK);
        confirm.initOwner(owner);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return Optional.empty();
        }
        return Optional.of(new Eleccion(m, otro.isDisabled() ? null : otro.getText(), g));
    };

    /** Muestra el nombre visible de un valor, nunca su código. */
    private static final class NameCell<T> extends ListCell<T> {
        private final Function<T, String> name;

        NameCell(Function<T, String> name) {
            this.name = name;
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : name.apply(item));
        }
    }
}
