package com.sigebox.central.ui.theme;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import com.sigebox.central.ui.help.HelpButton;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/**
 * Barra superior negra de 52 px en los dos temas (DESIGN v4 §5.7): logo + SIGEBOX a la izquierda, los pasos al
 * centro y, a la derecha, enlaces secundarios, el "?" de ayuda de conexión y el botón de tema. El paso o enlace
 * de la pantalla visible lleva fondo dorado; nunca hay más de uno activo.
 */
public final class StepsBar extends HBox {

    /** Un paso o enlace: id de pantalla y texto. */
    public record Item(String id, String label) {}

    private final Map<String, Button> steps = new LinkedHashMap<>();
    private final Map<String, Button> links = new LinkedHashMap<>();
    private String currentId;

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
        linksBox.getChildren().addAll(HelpButton.bar(), theme);
        getChildren().addAll(Ui.brand(), Ui.grow(), stepsBox, Ui.grow(), linksBox);
    }

    /** Marca el paso o enlace de la pantalla visible; un id que no está en la barra no marca ninguno. */
    public void select(String id) {
        currentId = steps.containsKey(id) || links.containsKey(id) ? id : null;
        for (var group : List.of(steps, links)) {
            group.forEach((k, b) -> {
                b.getStyleClass().remove("current");
                if (k.equals(currentId)) {
                    b.getStyleClass().add("current");
                }
            });
        }
    }

    /** Id del paso o enlace activo, o {@code null}. */
    public String currentId() {
        return currentId;
    }

    /** Ids de los pasos en orden (para pruebas y atajos). */
    public List<String> stepIds() {
        return List.copyOf(steps.keySet());
    }
}
