package com.sigebox.central.ui.result;

import com.sigebox.central.export.ExportService;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.Ui;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Paso "Resultado": terminar la pelea tras el último round, esperar las tarjetas finales, cerrar y
 * abrir el resultado a pantalla completa (DESIGN v2 §7.4).
 */
public final class ResultStepScreen {

    public static final String ID = "resultado";

    private ResultStepScreen() {
    }

    public static void register(Screens<Node> screens, ResultService service, ExportService export,
            Supplier<String> fightId) {
        screens.register(ID, "Resultado", () -> build(service, export, fightId));
    }

    static Node build(ResultService service, ExportService export, Supplier<String> fightId) {
        var status = Ui.support("");
        var card = Ui.glass();
        card.setMaxWidth(640);
        var root = new VBox(16, Ui.screenTitle("Resultado"), card, status);

        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            card.getChildren().clear();
            var f = fightId.get();
            if (f == null) {
                card.getChildren().addAll(Ui.cardTitle("Sin pelea activa"),
                        Ui.support("Cuando cierres una pelea, su resultado aparece aquí."));
                return;
            }
            var missing = service.missingFinalCards(f);
            if (service.canClose(f)) {
                var close = Ui.primary("Cerrar pelea y ver resultado");
                close.setOnAction(e -> {
                    try {
                        service.close(f);
                        ResultScreen.show(root.getScene().getWindow(), service.sheet(f), export);
                        status.setText("Pelea cerrada.");
                    } catch (RuntimeException ex) {
                        status.setText(ex.getMessage());
                    }
                    refresh[0].run();
                });
                card.getChildren().addAll(Ui.cardTitle("Listo para cerrar"),
                        Ui.support("Están todas las tarjetas finales o hay una terminación."), close);
                return;
            }
            var end = Ui.secondary("Terminar pelea");
            end.setOnAction(e -> {
                try {
                    service.endFight(f);
                    status.setText("Se pidió la tarjeta final a cada juez.");
                } catch (RuntimeException ex) {
                    status.setText(ex.getMessage());
                }
                refresh[0].run();
            });
            var update = Ui.secondary("Actualizar");
            update.setOnAction(e -> refresh[0].run());
            Label detail = Ui.support("Con todos los rounds cerrados, \"Terminar pelea\" pide la tarjeta final a "
                    + "cada juez. Después se puede cerrar." + (missing.size() < 5 && !missing.isEmpty()
                            ? " Aún sin tarjeta final: " + seats(missing) + "." : ""));
            card.getChildren().addAll(Ui.cardTitle("Pelea en curso"), detail, new HBox(12, end, update));
        };
        refresh[0].run();
        return root;
    }

    /** "Juez 1, 2 y 3". */
    static String seats(java.util.List<Integer> seats) {
        var names = seats.stream().map(String::valueOf).toList();
        var joined = names.size() == 1 ? names.get(0)
                : String.join(", ", names.subList(0, names.size() - 1)) + " y " + names.get(names.size() - 1);
        return (seats.size() == 1 ? "juez " : "jueces ") + joined;
    }
}
