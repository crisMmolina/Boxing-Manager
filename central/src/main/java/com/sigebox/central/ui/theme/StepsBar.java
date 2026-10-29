package com.sigebox.central.ui.theme;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/**
 * Barra superior de 48 px: marca a la izquierda, los pasos al centro (el actual con una línea dorada) y,
 * a la derecha, enlaces secundarios y el botón de tema claro u oscuro (DESIGN v3 §7).
 */
public final class StepsBar extends HBox {

    /** Un paso o enlace: id de pantalla y texto. */
    public record Item(String id, String label) {}

    private final Map<String, Button> steps = new LinkedHashMap<>();
    private final Map<String, Button> links = new LinkedHashMap<>();

    public StepsBar(List<Item> stepItems, List<Item> linkItems, Consumer<String> onSelect) {
        getStyleClass().add("top-bar");
        var stepsBox = new HBox();
        stepsBox.getStyleClass().add("steps");
        for (var item : stepItems) {
            var b = new Button(item.label());
            b.getStyleClass().setAll("step");
            b.setOnAction(e -> onSelect.accept(item.id()));
            steps.put(item.id(), b);
            stepsBox.getChildren().add(b);
        }
        var linksBox = new HBox(4);
        linksBox.setAlignment(Pos.CENTER_RIGHT);
        for (var item : linkItems) {
            var b = new Button(item.label());
            b.getStyleClass().setAll("link-button");
            b.setOnAction(e -> onSelect.accept(item.id()));
            links.put(item.id(), b);
            linksBox.getChildren().add(b);
        }
        var theme = new Button(Theme.mode().toggleLabel());
        theme.getStyleClass().setAll("link-button", "theme-toggle");
        theme.setOnAction(e -> theme.setText(Theme.toggle().toggleLabel()));
        linksBox.getChildren().add(theme);
        getChildren().addAll(Ui.brand(), Ui.grow(), stepsBox, Ui.grow(), linksBox);
    }

    /** Marca el paso actual; un id de enlace no marca ningún paso. */
    public void select(String id) {
        steps.forEach((k, b) -> {
            b.getStyleClass().remove("current");
            if (k.equals(id)) {
                b.getStyleClass().add("current");
            }
        });
    }

    /** Ids de los pasos en orden (para pruebas y atajos). */
    public List<String> stepIds() {
        return List.copyOf(steps.keySet());
    }
}
