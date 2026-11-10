package com.sigebox.central.ui.theme;

import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Encabezado de pantalla v5 (spec 006 §Central): arriba una línea de contexto (evento o pelea "Roja vs Azul"), debajo
 * el título y a la derecha la acción principal.
 */
public final class PageHeader extends HBox {

    private final Label context = new Label();
    private final Label title;
    private final HBox actionBox = new HBox(8);

    public PageHeader(String titleText) {
        getStyleClass().add("page-header");
        setAlignment(Pos.BOTTOM_LEFT);
        setSpacing(16);
        context.getStyleClass().add("page-context");
        title = Ui.screenTitle(titleText);
        var left = new VBox(2, context, title);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        getChildren().addAll(left, Ui.grow(), actionBox);
        context(null);
    }

    /** Línea de contexto; nula o vacía la oculta. */
    public PageHeader context(String text) {
        boolean on = text != null && !text.isBlank();
        context.setText(on ? text : "");
        context.setVisible(on);
        context.setManaged(on);
        return this;
    }

    /** Contexto de pelea: "Roja vs Azul". */
    public PageHeader fight(String roja, String azul) {
        return context(roja + " vs " + azul);
    }

    /** Acción principal a la derecha (reemplaza la anterior; nula la quita). */
    public PageHeader action(Node node) {
        if (node == null) {
            actionBox.getChildren().clear();
        } else {
            actionBox.getChildren().setAll(node);
        }
        return this;
    }

    public String contextText() {
        return context.isVisible() ? context.getText() : "";
    }

    public String titleText() {
        return title.getText();
    }

    public Node actionNode() {
        return actionBox.getChildren().isEmpty() ? null : actionBox.getChildren().get(0);
    }

    /** Navegación de la central: la barra superior ({@link StepsBar}) o la lateral v5 ({@link SideNav}). */
    public interface NavBar {

        /** Marca el paso o enlace de la pantalla visible. */
        void select(String id);

        /** Dentro de un evento muestra sus pasos; fuera, solo los fijos y {@code keepLink}. */
        void showEvent(boolean inEvent, String keepLink);

        /** Id del paso o enlace activo, o {@code null}. */
        String currentId();

        /** Ids de los pasos en orden. */
        List<String> stepIds();
    }
}
