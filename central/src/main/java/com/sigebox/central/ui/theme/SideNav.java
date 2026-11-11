package com.sigebox.central.ui.theme;

import com.sigebox.central.ui.help.HelpButton;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.animation.Animation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/**
 * Barra lateral de la central v5 (spec 006 §Central): logo, el evento actual con su avance ("2 de 5") y los pasos con
 * ícono; los pasos terminados llevan ✓ y En vivo un punto que late mientras hay un round abierto. Abajo, los enlaces
 * (Bitácora, Inicio), el "?" de ayuda y el tema. Misma API que {@link StepsBar}: {@link #select} y {@link #showEvent}.
 */
public final class SideNav extends VBox implements PageHeader.NavBar {

    /** Ícono de cada paso conocido; un paso sin ícono no lleva ninguno. */
    private static final Map<String, String> ICONS = Map.of(
            "funcion", "calendar", "jueces", "users", "rounds", "activity", "resultado", "trophy");

    private final Map<String, Button> steps = new LinkedHashMap<>();
    private final Map<String, Button> links = new LinkedHashMap<>();
    private final Map<String, Node> checks = new LinkedHashMap<>();
    private final VBox stepsBox = new VBox(2);
    private final VBox eventBox = new VBox(2);
    private final Label eventName = new Label();
    private final Label eventProgress = new Label();
    private final StackPane liveDot = new StackPane();
    private final String liveId;
    private Animation pulse;
    private String currentId;
    private boolean live;

    /**
     * @param liveId id del paso que muestra el punto en vivo (o {@code null})
     */
    public SideNav(List<StepsBar.Item> stepItems, List<StepsBar.Item> linkItems, String liveId,
            Consumer<String> onSelect) {
        this.liveId = liveId;
        getStyleClass().add("side-nav");
        setFillWidth(true);
        eventBox.getStyleClass().add("side-event");
        eventName.getStyleClass().add("side-event-name");
        eventProgress.getStyleClass().add("side-event-progress");
        eventBox.getChildren().addAll(eventName, eventProgress);
        stepsBox.getStyleClass().add("side-steps");
        for (var item : stepItems) {
            var b = new Button(item.label());
            b.getStyleClass().setAll("side-step");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(Pos.CENTER_LEFT);
            var icon = ICONS.get(item.id());
            if (icon != null) {
                b.setGraphic(Icons.icon(icon, 18));
            }
            var check = Icons.icon("check", 14);
            check.getStyleClass().add("side-check");
            show(check, false);
            checks.put(item.id(), check);
            var row = new HBox(b, check);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(b, javafx.scene.layout.Priority.ALWAYS);
            if (item.id().equals(liveId)) {
                var core = new Circle(4);
                core.getStyleClass().add("live-dot");
                var ring = new Circle(4);
                ring.getStyleClass().add("live-ring");
                liveDot.getChildren().addAll(ring, core);
                liveDot.getStyleClass().add("side-live");
                show(liveDot, false);
                row.getChildren().add(1, liveDot);
            }
            b.setOnAction(e -> onSelect.accept(item.id()));
            steps.put(item.id(), b);
            stepsBox.getChildren().add(row);
        }
        var bottom = new VBox(2);
        bottom.getStyleClass().add("side-bottom");
        for (var item : linkItems) {
            var b = new Button(item.label());
            b.getStyleClass().setAll("side-link");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(Pos.CENTER_LEFT);
            b.setOnAction(e -> onSelect.accept(item.id()));
            links.put(item.id(), b);
            bottom.getChildren().add(b);
        }
        var theme = new Button(Theme.mode().toggleLabel());
        theme.getStyleClass().setAll("side-link", "theme-toggle");
        theme.setOnAction(e -> {
            var mode = Theme.toggle();
            Logo.recolor(getScene() != null ? getScene().getRoot() : this, mode);
            theme.setText(mode.toggleLabel());
        });
        var tools = new HBox(4, HelpButton.bar(), theme);
        tools.setAlignment(Pos.CENTER_LEFT);
        bottom.getChildren().add(tools);
        var spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        getChildren().addAll(Ui.brand(), eventBox, stepsBox, spacer, bottom);
        show(eventBox, false);
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

    /** Igual que {@link StepsBar#showEvent}: fuera de un evento solo logo, "?", tema y {@code keepLink}. */
    public void showEvent(boolean inEvent, String keepLink) {
        show(stepsBox, inEvent);
        show(eventBox, inEvent && !eventName.getText().isEmpty());
        links.forEach((k, b) -> show(b, inEvent || k.equals(keepLink)));
        setLive(live && inEvent);
    }

    /** Evento actual y su avance: pelea {@code current} de {@code total}; {@code name} nulo lo oculta. */
    public void showProgress(String name, int current, int total) {
        eventName.setText(name == null ? "" : name);
        eventProgress.setText(name == null || total <= 0 ? "" : current + " de " + total);
        show(eventBox, name != null && stepsBox.isVisible());
    }

    /** Marca con ✓ los pasos terminados ({@code done}) y quita la marca a los demás. */
    public void markDone(java.util.Collection<String> done) {
        checks.forEach((k, c) -> {
            show(c, done.contains(k));
            steps.get(k).getStyleClass().remove("done");
            if (done.contains(k)) {
                steps.get(k).getStyleClass().add("done");
            }
        });
    }

    /** Punto en vivo junto al paso {@code liveId}: late mientras hay un round abierto (sin latido si se reduce). */
    public void setLive(boolean on) {
        live = on;
        boolean visible = on && liveId != null && stepsBox.isVisible();
        show(liveDot, visible);
        if (visible && pulse == null && !liveDot.getChildren().isEmpty()) {
            pulse = Motion.pulse(liveDot.getChildren().get(0));
        } else if (!visible && pulse != null) {
            pulse.stop();
            pulse = null;
        }
    }

    /** {@code true} si el punto en vivo se ve. */
    public boolean isLive() {
        return liveDot.isVisible();
    }

    /** {@code true} si el punto en vivo late (falso con movimiento reducido). */
    public boolean isPulsing() {
        return pulse != null;
    }

    /** Texto del avance del evento ("2 de 5"), vacío si no hay. */
    public String progressText() {
        return eventProgress.getText();
    }

    /** {@code true} si el paso lleva ✓. */
    public boolean isDone(String id) {
        var c = checks.get(id);
        return c != null && c.isVisible();
    }

    /** {@code true} si el paso tiene ícono. */
    public boolean hasIcon(String id) {
        var b = steps.get(id);
        return b != null && b.getGraphic() != null;
    }

    private static void show(Node node, boolean on) {
        node.setVisible(on);
        node.setManaged(on);
    }

    /** Id del paso o enlace activo, o {@code null}. */
    public String currentId() {
        return currentId;
    }

    /** Ids de los pasos en orden. */
    public List<String> stepIds() {
        return List.copyOf(steps.keySet());
    }
}
