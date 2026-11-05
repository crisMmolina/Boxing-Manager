package com.sigebox.central.ui.result;

import com.sigebox.central.export.ExportService;
import com.sigebox.central.export.ResultSheet;
import com.sigebox.central.service.FuncionService.Fila;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.funcion.FuncionView;
import com.sigebox.central.ui.theme.Ui;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Paso "Resultado": los puntos de cada juez con sus descuentos, sin ganador ni decisión, y "Cerrar
 * pelea" en cuanto llegan las tarjetas finales; el ganador se ve al cerrar, en PDF y CSV (DESIGN v3
 * §7.4). Se actualiza sola mientras está en pantalla.
 */
public final class ResultStepScreen {

    public static final String ID = "resultado";

    /** Cada cuánto se relee: un {@code ACK} o un descuento se ve en menos de 1 s. */
    private static final Duration REFRESH = Duration.millis(500);

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
        return build(service, fightId, next, startNext,
                (node, sheet) -> ResultScreen.show(node.getScene().getWindow(), sheet, export));
    }

    /** {@code show} recibe la hoja de la pelea recién cerrada. */
    static Node build(ResultService service, Supplier<String> fightId, Supplier<Optional<Fila>> next,
            Runnable startNext, BiConsumer<Node, ResultSheet> show) {
        var status = Ui.support("");
        var card = Ui.card();
        card.setMaxWidth(860);
        var root = new VBox(16, Ui.screenTitle("Resultado"), card, status);

        // Solo se reconstruye si cambió lo que se ve: así el botón sigue pulsable entre lecturas.
        String[] shown = new String[1];
        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            var f = fightId.get();
            if (f == null) {
                var following = next.get();
                if (!changed(shown, "sin pelea " + following)) {
                    return;
                }
                card.getChildren().clear();
                if (following.isPresent()) {
                    card.getChildren().addAll(nextFight(following.get(), startNext, status));
                    return;
                }
                card.getChildren().addAll(Ui.cardTitle("Sin pelea activa"),
                        Ui.support("Cuando cierres una pelea, su resultado aparece aquí."));
                return;
            }
            boolean ended = service.ended(f);
            boolean closed = service.stored(f).isPresent();
            boolean canClose = service.canClose(f);
            var missing = service.missingFinalCards(f);
            var sheet = service.scoreSheet(f);
            if (!changed(shown, String.join("|", f, String.valueOf(ended), String.valueOf(closed),
                    String.valueOf(canClose), missing.toString(), sheet.map(ResultStepScreen::scores).orElse("")))) {
                return;
            }
            card.getChildren().clear();
            if (!ended) {
                card.getChildren().addAll(Ui.cardTitle("Pelea en curso"),
                        Ui.support("Los puntos aparecen cuando termine la pelea."));
                return;
            }
            card.getChildren().add(Ui.cardTitle(closed ? "Pelea cerrada"
                    : canClose ? "Listo para cerrar" : "Esperando tarjetas finales"));
            if (!closed && !canClose) {
                card.getChildren().add(Ui.support("Aún sin tarjeta final: " + seats(missing) + "."));
            }
            card.getChildren().add(sheet.<Node>map(ResultTable::of)
                    .orElseGet(() -> Ui.support("Falta la tarjeta de algún round: los puntos aparecen al llegar.")));
            if (closed) {
                return;
            }
            var close = Ui.primary("Cerrar pelea");
            close.setId("cerrar-pelea");
            close.setDisable(!canClose);
            close.setOnAction(e -> {
                try {
                    service.close(f);
                    show.accept(root, service.sheet(f));
                    status.setText("Pelea cerrada.");
                } catch (RuntimeException ex) {
                    status.setText(ex.getMessage());
                }
                refresh[0].run();
            });
            card.getChildren().add(close);
        };
        refresh[0].run();

        var timer = new Timeline(new KeyFrame(REFRESH, e -> {
            try {
                refresh[0].run();
            } catch (RuntimeException ex) {
                status.setText(ex.getMessage());
            }
        }));
        timer.setCycleCount(Animation.INDEFINITE);
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                timer.stop();
            } else {
                timer.play();
            }
        });
        return root;
    }

    private static boolean changed(String[] shown, String key) {
        if (key.equals(shown[0])) {
            return false;
        }
        shown[0] = key;
        return true;
    }

    private static String scores(ResultSheet sheet) {
        return sheet.rows() + " " + sheet.descuentos();
    }

    /** Tarjeta de la siguiente pelea de la función: esquinas, rounds, jueces y el botón que la inicia. */
    static List<Node> nextFight(Fila fila, Runnable startNext, Label status) {
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
        return List.of(Ui.cardTitle("Pelea " + fila.orden() + " de la función"), red, blue,
                Ui.support(FuncionView.detalle(fila.rounds(), fila.jueces())
                        + ". Los jueces conectados pasan solos; solo los lugares nuevos usan QR."), start);
    }

    /** "Juez 1, 2 y 3". */
    static String seats(List<Integer> seats) {
        var names = seats.stream().map(String::valueOf).toList();
        var joined = names.size() == 1 ? names.get(0)
                : String.join(", ", names.subList(0, names.size() - 1)) + " y " + names.get(names.size() - 1);
        return (seats.size() == 1 ? "juez " : "jueces ") + joined;
    }
}
