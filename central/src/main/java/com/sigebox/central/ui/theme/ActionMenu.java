package com.sigebox.central.ui.theme;

import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/** Menú ⋯ de una fila (DESIGN v5 §5.15): reúne sus acciones en lugar de varios botones sueltos. */
public final class ActionMenu extends MenuButton {

    /** {@code accessibleText}: qué fila es ("Más acciones de la pelea 3"). */
    public ActionMenu(String accessibleText) {
        getStyleClass().add("action-menu");
        setGraphic(Icons.icon("ellipsis", 20));
        setAccessibleText(accessibleText);
    }

    /** Agrega una acción al final. */
    public MenuItem add(String text, Runnable action) {
        var item = new MenuItem(text);
        item.setOnAction(e -> action.run());
        getItems().add(item);
        return item;
    }

    /** Línea que separa las acciones que borran de las demás. */
    public void separator() {
        getItems().add(new SeparatorMenuItem());
    }
}
