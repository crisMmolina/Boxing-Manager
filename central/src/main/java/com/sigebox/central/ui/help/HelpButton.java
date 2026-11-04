package com.sigebox.central.ui.help;

import com.sigebox.central.ui.theme.Ui;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;

/** Botones "?" de ayuda de conexión (DESIGN v4 §5.8): el fijo de la barra y el "? Cómo reconectar". */
public final class HelpButton {

    static final String RECONNECT = "? Cómo reconectar";

    private HelpButton() {
    }

    /** "?" de 36 px en la barra; abre o cierra el panel de ayuda. */
    public static Button bar() {
        var b = new Button("?");
        b.getStyleClass().setAll("button", "help-button");
        b.setAccessibleText("Ayuda de conexión");
        b.setTooltip(new Tooltip("Ayuda de conexión"));
        b.setOnAction(e -> HelpPanel.toggleIn(b));
        return b;
    }

    /** Botón secundario pequeño que va junto a todo "Sin conexión"; abre el panel de ayuda. */
    public static Button reconnect() {
        var b = Ui.secondary(RECONNECT);
        b.getStyleClass().add("reconnect");
        b.setOnAction(e -> HelpPanel.openIn(b));
        return b;
    }

    /**
     * Estado de conexión de un juez: punto lleno o anillo, el texto y, sin conexión, "? Cómo reconectar". Si no
     * cabe en la misma línea, el botón baja entero a la siguiente.
     */
    public static FlowPane connection(boolean connected, String who) {
        var row = new FlowPane(8, 6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("connection");
        row.getChildren().addAll(Ui.dot(connected, who), Ui.support(who + (connected ? " conectado" : " sin conexión")));
        if (!connected) {
            row.getChildren().add(reconnect());
        }
        return row;
    }
}
