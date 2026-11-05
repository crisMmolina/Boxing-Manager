package com.sigebox.central.ui.rounds;

import com.sigebox.central.service.DescuentoService;
import com.sigebox.central.ui.Labels;
import com.sigebox.central.ui.theme.Ui;
import com.sigebox.core.model.Descuento;
import com.sigebox.core.model.Esquina;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Tarjeta de descuentos de En vivo (ADR 0005): quitar un punto a una esquina y anular con motivo. Lista cada
 * descuento por separado, sin sumas: En vivo no muestra totales ni ganador (PROJECT.md principio 3).
 */
final class DescuentosPanel {

    static final String ROJA_ID = "descuento-roja";
    static final String AZUL_ID = "descuento-azul";

    /** Lo que se le pregunta al operador; las pruebas lo sustituyen para no abrir diálogos. */
    interface Ask {

        /** Round elegido con {@code actual} por defecto; vacío si no confirma. */
        Optional<Integer> round(String pregunta, List<Integer> rounds, int actual);

        /** Motivo escrito; vacío si cancela. */
        Optional<String> motivo(String pregunta);
    }

    /** Diálogos reales de JavaFX. */
    static final Ask DIALOGS = new Ask() {
        @Override
        public Optional<Integer> round(String pregunta, List<Integer> rounds, int actual) {
            var dialog = new ChoiceDialog<>(actual, rounds);
            dialog.setTitle("Descuento");
            dialog.setHeaderText(pregunta);
            dialog.setContentText("Round");
            return dialog.showAndWait();
        }

        @Override
        public Optional<String> motivo(String pregunta) {
            var dialog = new TextInputDialog();
            dialog.setTitle("Anular descuento");
            dialog.setHeaderText(pregunta);
            dialog.setContentText("Motivo");
            return dialog.showAndWait();
        }
    };

    private final DescuentoService service;
    private final Supplier<String> fightId;
    private final Ask ask;
    private final Button roja = Ui.secondary("−1 roja");
    private final Button azul = Ui.secondary("−1 azul");
    private final Label status = Ui.support("");
    private final VBox list = new VBox(4);
    private final VBox card;

    private int rounds;
    private int current;
    private boolean open;
    private List<Descuento> shown;

    DescuentosPanel(DescuentoService service, Supplier<String> fightId, Ask ask) {
        this.service = service;
        this.fightId = fightId;
        this.ask = ask;
        roja.setId(ROJA_ID);
        azul.setId(AZUL_ID);
        roja.setOnAction(e -> poner(Esquina.ROJA));
        azul.setOnAction(e -> poner(Esquina.AZUL));
        var buttons = new HBox(8, roja, azul);
        buttons.setAlignment(Pos.CENTER_LEFT);
        card = Ui.card(Ui.cardTitle("Descuentos"), buttons, status, list);
    }

    Node node() {
        return card;
    }

    /** Sin pelea activa: botones apagados y lista vacía. */
    void clear() {
        open = false;
        shown = null;
        list.getChildren().clear();
        setButtons();
    }

    /** {@code current} es el round en curso, o 0 si ya se cerraron todos. Solo repinta la lista si cambió. */
    void refresh(int rounds, int current) {
        var f = fightId.get();
        if (f == null) {
            clear();
            return;
        }
        this.rounds = rounds;
        this.current = current;
        boolean nowOpen = service.abierta(f);
        var now = service.listar(f);
        if (nowOpen != open || !now.equals(shown)) {
            open = nowOpen;
            shown = now;
            fillList(now);
        }
        setButtons();
    }

    private void setButtons() {
        roja.setDisable(!open);
        azul.setDisable(!open);
    }

    private void poner(Esquina esquina) {
        var f = fightId.get();
        if (f == null) {
            return;
        }
        var pregunta = "Quitar 1 punto a la esquina " + Labels.of(esquina).toLowerCase() + ". ¿En qué round?";
        var opciones = IntStream.rangeClosed(1, rounds).boxed().toList();
        ask.round(pregunta, opciones, current == 0 ? rounds : current)
                .ifPresent(round -> run(() -> service.poner(f, round, esquina)));
    }

    private void anular(Descuento d) {
        ask.motivo("Anular: " + texto(d) + ". ¿Por qué?").ifPresent(motivo -> run(() -> service.anular(d.id(), motivo)));
    }

    private void run(Runnable action) {
        try {
            action.run();
            status.setText("");
        } catch (RuntimeException ex) {
            status.setText(ex.getMessage());
        }
        refresh(rounds, current);
    }

    private void fillList(List<Descuento> descuentos) {
        list.getChildren().clear();
        for (var d : descuentos) {
            list.getChildren().add(row(d));
        }
    }

    private Node row(Descuento d) {
        if (d.anulado()) {
            return Ui.support(texto(d) + " · anulado: " + d.motivoAnulacion());
        }
        var line = new HBox(8, Ui.body(texto(d)), Ui.grow());
        line.setAlignment(Pos.CENTER_LEFT);
        if (open) {
            var undo = Ui.secondary("Anular");
            undo.setOnAction(e -> anular(d));
            line.getChildren().add(undo);
        }
        return line;
    }

    private static String texto(Descuento d) {
        return "Round " + d.round() + " · −" + d.puntos() + " " + Labels.of(d.esquina()).toLowerCase();
    }
}
