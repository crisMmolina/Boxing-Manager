package com.sigebox.central.ui.result;

import com.sigebox.central.export.ExportService;
import com.sigebox.central.service.FuncionService.Fila;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.funcion.FuncionView;
import com.sigebox.central.ui.theme.Ui;
import java.util.Optional;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Paso "Resultado": terminar la pelea tras el último round, esperar las tarjetas finales, cerrar y
 * abrir el resultado a pantalla completa (DESIGN v3 §7.4).
 */
public final class ResultStepScreen {

    public static final String ID = "resultado";

    private ResultStepScreen() {
    }

    public static void register(Screens<Node> screens, ResultService service, ExportService export,
            Supplier<String> fightId) {
        register(screens, service, export, fightId, Optional::empty, () -> { });
    }

    /**
     * Con una función en curso: tras cerrar la pelea ofrece "Siguiente pelea: Roja vs Azul" ({@code next} la
     * da, {@code startNext} la inicia y lleva a emparejar jueces).
     */
    public static void register(Screens<Node> screens, ResultService service, ExportService export,
            Supplier<String> fightId, Supplier<Optional<Fila>> next, Runnable startNext) {
        screens.register(ID, "Resultado", () -> build(service, export, fightId, next, startNext));
    }

    static Node build(ResultService service, ExportService export, Supplier<String> fightId,
            Supplier<Optional<Fila>> next, Runnable startNext) {
        var status = Ui.support("");
        var card = Ui.card();
        card.setMaxWidth(640);
        var root = new VBox(16, Ui.screenTitle("Resultado"), card, status);

        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            card.getChildren().clear();
            var f = fightId.get();
            if (f == null) {
                var following = next.get();
                if (following.isPresent()) {
                    card.getChildren().addAll(nextFight(following.get(), startNext, status));
                    return;
                }
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

    /** Tarjeta de la siguiente pelea de la función: esquinas, rounds, jueces y el botón que la inicia. */
    static java.util.List<Node> nextFight(Fila fila, Runnable startNext, Label status) {
        var red = new HBox(8, Ui.cornerDot(true), Ui.body(fila.roja()));
        red.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        var blue = new HBox(8, Ui.cornerDot(false), Ui.body(fila.azul()));
        blue.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        var start = Ui.primary(FuncionView.siguienteTexto(fila));
        start.setOnAction(e -> {
            try {
                startNext.run();
            } catch (RuntimeException ex) {
                status.setText(ex.getMessage());
            }
        });
        return java.util.List.of(Ui.cardTitle("Pelea " + fila.orden() + " de la función"), red, blue,
                Ui.support(FuncionView.detalle(fila.rounds(), fila.jueces())
                        + ". Los jueces conectados pasan solos; solo los lugares nuevos usan QR."), start);
    }

    /** "Juez 1, 2 y 3". */
    static String seats(java.util.List<Integer> seats) {
        var names = seats.stream().map(String::valueOf).toList();
        var joined = names.size() == 1 ? names.get(0)
                : String.join(", ", names.subList(0, names.size() - 1)) + " y " + names.get(names.size() - 1);
        return (seats.size() == 1 ? "juez " : "jueces ") + joined;
    }
}
