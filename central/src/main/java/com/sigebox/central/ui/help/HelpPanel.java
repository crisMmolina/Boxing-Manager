package com.sigebox.central.ui.help;

import com.sigebox.central.ui.theme.Ui;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Panel lateral de 420 px con "Recuperar la conexión" (DESIGN v4 §5.8): pasos numerados en Barlow Condensed
 * dorado, la parte de la central primero y un botón principal "Listo" que lo cierra. Se abre a la derecha de la
 * ventana, sea cual sea el bloque desde donde se pidió.
 */
public final class HelpPanel extends VBox {

    static final double WIDTH = 420;

    private final Button close;

    HelpPanel(ConnectionGuide guide) {
        super(16);
        getStyleClass().add("help-panel");
        setPrefWidth(WIDTH);
        setMinWidth(WIDTH);
        setMaxWidth(WIDTH);
        var steps = new VBox(20);
        for (var section : guide.sections()) {
            var list = new VBox(12);
            list.getChildren().add(Ui.cardTitle(section.name()));
            int n = 1;
            for (var step : section.steps()) {
                var number = new Label(String.valueOf(n++));
                number.getStyleClass().add("help-number");
                number.setMinWidth(24);
                var text = new VBox(2);
                if (!step.lead().isEmpty()) {
                    var lead = Ui.body(step.lead());
                    lead.getStyleClass().add("help-lead");
                    text.getChildren().add(lead);
                }
                text.getChildren().add(Ui.body(step.text()));
                list.getChildren().add(new HBox(12, number, text));
            }
            steps.getChildren().add(list);
        }
        var scroll = new ScrollPane(steps);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);
        close = Ui.primary("Listo");
        close.setMaxWidth(Double.MAX_VALUE);
        close.setOnAction(e -> close());
        getChildren().addAll(Ui.screenTitle(guide.title()), scroll, close);
        setAccessibleText(guide.title());
    }

    /** Botón principal que cierra el panel. */
    public Button closeButton() {
        return close;
    }

    /** Quita el panel de la ventana. */
    public void close() {
        if (getParent() instanceof BorderPane host && host.getRight() == this) {
            host.setRight(null);
        }
    }

    /** Abre el panel en la ventana de {@code from} (o devuelve el que ya está abierto). */
    public static HelpPanel openIn(Node from) {
        var host = host(from);
        if (host == null) {
            return null;
        }
        if (host.getRight() instanceof HelpPanel open) {
            return open;
        }
        var panel = new HelpPanel(ConnectionGuide.load());
        host.setRight(panel);
        panel.close.requestFocus();
        return panel;
    }

    /** Abre el panel o, si ya está abierto, lo cierra (el "?" de la barra). */
    public static void toggleIn(Node from) {
        var host = host(from);
        if (host != null && host.getRight() instanceof HelpPanel open) {
            open.close();
        } else {
            openIn(from);
        }
    }

    /** El {@link BorderPane} más externo que contiene a {@code from}: la ventana de la central. */
    static BorderPane host(Node from) {
        BorderPane found = null;
        for (Node n = from; n != null; n = n.getParent()) {
            if (n instanceof BorderPane b) {
                found = b;
            }
        }
        return found;
    }

    /** Textos visibles del panel en orden (para pruebas y lectores de pantalla). */
    static List<String> texts(Node root) {
        var out = new ArrayList<String>();
        if (root instanceof Label l) {
            out.add(l.getText());
        }
        if (root instanceof ScrollPane s && s.getContent() != null) {
            out.addAll(texts(s.getContent()));
        }
        if (root instanceof Parent p) {
            for (var child : p.getChildrenUnmodifiable()) {
                out.addAll(texts(child));
            }
        }
        return out;
    }
}
