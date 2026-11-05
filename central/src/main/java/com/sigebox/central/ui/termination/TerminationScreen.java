package com.sigebox.central.ui.termination;

import com.sigebox.central.service.TerminationService;
import com.sigebox.central.ui.Labels;
import com.sigebox.central.ui.LiveRefresh;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.Skeleton;
import com.sigebox.central.ui.theme.Ui;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.MotivoTerminacion;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Formulario de terminación extraordinaria con doble confirmación (aviso + escribir TERMINAR). */
public final class TerminationScreen {

    public static final String ID = "terminacion";

    static final String NO_FIGHT = "No hay pelea en curso.";
    private static final String START_FIRST = "Inicia una pelea para poder terminarla.";

    private TerminationScreen() {
    }

    public static void register(Screens<Node> screens, TerminationService service, Supplier<String> fightId) {
        register(screens, service, fightId, LiveRefresh.BACKGROUND);
    }

    /** Como {@link #register(Screens, TerminationService, Supplier)}, con las lecturas en {@code reads}. */
    public static void register(Screens<Node> screens, TerminationService service, Supplier<String> fightId,
            Executor reads) {
        screens.register(ID, "Terminación", () -> build(service, fightId, reads));
    }

    static Node build(TerminationService service, Supplier<String> fightId) {
        return build(service, fightId, LiveRefresh.DIRECT);
    }

    static Node build(TerminationService service, Supplier<String> fightId, Executor reads) {
        var motivo = new ComboBox<>(FXCollections.observableArrayList(MotivoTerminacion.values()));
        motivo.setCellFactory(l -> new NameCell<>(TerminationService::label));
        motivo.setButtonCell(new NameCell<>(TerminationService::label));
        var otro = new TextField();
        var esquina = new ComboBox<>(FXCollections.observableArrayList(Esquina.values()));
        esquina.setCellFactory(l -> new NameCell<>(Labels::of));
        esquina.setButtonCell(new NameCell<>(Labels::of));
        var round = new Spinner<Integer>(1, 12, 1);
        var tiempo = new TextField();
        var obs = new TextArea();
        obs.setPrefRowCount(3);
        var status = Ui.support("");

        motivo.valueProperty().addListener((o, a, m) -> {
            otro.setDisable(m != MotivoTerminacion.OTRO);
            esquina.setDisable(m != null && !TerminationService.requiresCorner(m) && m != MotivoTerminacion.OTRO);
        });

        var terminar = Ui.primary("Terminar pelea");
        terminar.setDisable(true); // hasta saber si hay pelea
        var why = Ui.why(START_FIRST, terminar);
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
                    why.setText("La pelea ya terminó.");
                } catch (RuntimeException ex) {
                    status.setText(ex.getMessage());
                }
            });
        });

        var card = Ui.card(Ui.cardTitle("Datos de la terminación"), Ui.field("Motivo", motivo),
                Ui.field("Razón (obligatoria con Otro)", "Ej. Decisión del médico", otro),
                Ui.field("Esquina ganadora", esquina),
                new HBox(12, Ui.field("Round", round), Ui.field("Tiempo", "Ej. 1:30", tiempo)),
                Ui.field("Observaciones", "Ej. Corte en la ceja", obs), terminar, why);
        card.setMaxWidth(720);
        var fightState = new VBox(Skeleton.lines(1));
        var next = Ui.next("Inicia una pelea desde Evento.");
        var root = new VBox(16, Ui.screenTitle("Terminación extraordinaria"), next, fightState, card, status);
        // Solo se refresca el estado de la pelea: lo que se lleva escrito en el formulario no se toca.
        LiveRefresh.watch(root, reads, () -> Optional.ofNullable(fightId.get()), fight -> {
            terminar.setDisable(fight.isEmpty());
            why.setText(START_FIRST);
            Ui.next(next, fight.isPresent() ? "Usa esta pantalla solo si la pelea no puede seguir."
                    : "Inicia una pelea desde Evento.");
            if (fight.isPresent()) {
                status.setText("");
            }
            LiveRefresh.swap(fightState, fight.isPresent() ? List.of() : List.of(Ui.support(NO_FIGHT)));
        });
        return root;
    }

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
