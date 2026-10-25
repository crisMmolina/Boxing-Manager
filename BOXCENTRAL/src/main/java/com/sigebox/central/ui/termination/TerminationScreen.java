package com.sigebox.central.ui.termination;

import com.sigebox.central.service.TerminationService;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.Ui;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.MotivoTerminacion;
import java.util.function.Supplier;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** Formulario de terminación extraordinaria con doble confirmación (aviso + escribir TERMINAR). */
public final class TerminationScreen {

    public static final String ID = "terminacion";

    private TerminationScreen() {
    }

    public static void register(Screens<Node> screens, TerminationService service, Supplier<String> fightId) {
        screens.register(ID, "Terminación", () -> build(service, fightId));
    }

    static Node build(TerminationService service, Supplier<String> fightId) {
        var motivo = new ComboBox<>(FXCollections.observableArrayList(MotivoTerminacion.values()));
        motivo.setCellFactory(l -> new MotivoCell());
        motivo.setButtonCell(new MotivoCell());
        var otro = new TextField();
        otro.setPromptText("Razón (obligatoria con Otro)");
        var esquina = new ComboBox<>(FXCollections.observableArrayList(Esquina.values()));
        var round = new Spinner<Integer>(1, 12, 1);
        var tiempo = new TextField();
        tiempo.setPromptText("m:ss");
        var obs = new TextArea();
        obs.setPrefRowCount(3);
        var status = Ui.support("");

        motivo.valueProperty().addListener((o, a, m) -> {
            otro.setDisable(m != MotivoTerminacion.OTRO);
            esquina.setDisable(m != null && !TerminationService.requiresCorner(m) && m != MotivoTerminacion.OTRO);
        });

        var terminar = Ui.primary("Terminar pelea");
        terminar.setOnAction(e -> {
            var f = fightId.get();
            if (f == null || motivo.getValue() == null) {
                status.setText("Elige el motivo");
                return;
            }
            var input = new TerminationService.Input(motivo.getValue(), otro.getText(), esquina.getValue(),
                    round.getValue(), tiempo.getText(), obs.getText());
            var warn = new Alert(Alert.AlertType.CONFIRMATION, "Terminar la pelea por "
                    + TerminationService.label(input.motivo()) + " en el round " + input.round() + "?",
                    ButtonType.CANCEL, ButtonType.OK);
            if (warn.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
            var word = new TextInputDialog();
            word.setHeaderText("Escribe " + TerminationService.CONFIRM_WORD + " para confirmar");
            word.showAndWait().ifPresent(text -> {
                try {
                    service.terminate(f, input, text);
                    status.setText("Pelea terminada");
                    terminar.setDisable(true);
                } catch (RuntimeException ex) {
                    status.setText(ex.getMessage());
                }
            });
        });

        var form = new GridPane();
        form.getStyleClass().add("form");
        form.setHgap(12);
        form.setVgap(10);
        form.addRow(0, Ui.support("Motivo"), motivo);
        form.addRow(1, Ui.support("Razón"), otro);
        form.addRow(2, Ui.support("Esquina ganadora"), esquina);
        form.addRow(3, Ui.support("Round"), round);
        form.addRow(4, Ui.support("Tiempo"), tiempo);
        form.addRow(5, Ui.support("Observaciones"), obs);
        var card = Ui.glass(Ui.cardTitle("Datos de la terminación"), form, terminar);
        card.setMaxWidth(720);
        var root = new VBox(16, Ui.screenTitle("Terminación extraordinaria"), card, status);
        return root;
    }

    private static final class MotivoCell extends ListCell<MotivoTerminacion> {
        @Override
        protected void updateItem(MotivoTerminacion item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : TerminationService.label(item));
        }
    }
}
