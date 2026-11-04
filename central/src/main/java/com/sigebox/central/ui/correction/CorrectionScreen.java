package com.sigebox.central.ui.correction;

import com.sigebox.central.service.CorrectionService;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.Ui;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Corrección de una tarjeta o captura desde papel por el organizador, con motivo obligatorio.
 *
 * <p>Ya no está en la app (spec 004 quitó Corrección): {@code App} no la registra ni hay enlace. Queda solo porque
 * {@code ScreenshotHarness}, fuera de DSK-030, la usa; se borra junto con esa captura.
 */
@Deprecated
public final class CorrectionScreen {

    public static final String ID = "correccion";

    private CorrectionScreen() {
    }

    public static void register(Screens<Node> screens, CorrectionService service, Supplier<String> fightId) {
        screens.register(ID, "Corrección", () -> build(service, fightId));
    }

    static Node build(CorrectionService service, Supplier<String> fightId) {
        var juez = new Spinner<Integer>(1, 5, 1);
        var round = new Spinner<Integer>(1, 12, 1);
        var rojo = new Spinner<Integer>(0, 10, 10);
        var azul = new Spinner<Integer>(0, 10, 9);
        var motivo = new TextField();
        motivo.setPromptText("Motivo (obligatorio)");
        var status = Ui.support("");

        var corregir = Ui.primary("Corregir tarjeta");
        corregir.setOnAction(e -> run(status, () -> service.correct(fightId.get(), juez.getValue(), round.getValue(),
                rojo.getValue(), azul.getValue(), motivo.getText()), "Tarjeta corregida"));
        var capturar = Ui.secondary("Capturar desde papel");
        capturar.setOnAction(e -> run(status, () -> service.capture(fightId.get(), juez.getValue(), round.getValue(),
                rojo.getValue(), azul.getValue(), motivo.getText()), "Tarjeta capturada"));

        var form = new GridPane();
        form.getStyleClass().add("form");
        form.setHgap(12);
        form.setVgap(10);
        form.addRow(0, Ui.support("Juez"), juez);
        form.addRow(1, Ui.support("Round"), round);
        form.addRow(2, Ui.support("Roja"), rojo);
        form.addRow(3, Ui.support("Azul"), azul);
        form.addRow(4, Ui.support("Motivo"), motivo);
        var card = Ui.card(Ui.cardTitle("Tarjeta"), form, new HBox(12, corregir, capturar),
                Ui.support("Cada cambio exige motivo y queda en la bitácora con el valor anterior."));
        card.setMaxWidth(640);
        return new VBox(16, Ui.screenTitle("Corrección o captura desde papel"), card, status);
    }

    private static void run(Label status, Runnable action, String ok) {
        try {
            action.run();
            status.setText(ok);
        } catch (RuntimeException ex) {
            status.setText(ex.getMessage());
        }
    }
}
