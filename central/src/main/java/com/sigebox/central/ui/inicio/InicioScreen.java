package com.sigebox.central.ui.inicio;

import com.sigebox.central.service.FuncionService;
import com.sigebox.central.ui.LiveRefresh;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.funcion.FuncionScreen;
import com.sigebox.central.ui.rounds.RoundsScreen;
import com.sigebox.central.ui.theme.Skeleton;
import com.sigebox.central.ui.theme.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Inicio (DESIGN v4 §7.1): eventos del historial, del más reciente al más antiguo, y "Nuevo evento" como
 * acción principal. Cada fila es un botón: se abre con clic, Tab/flechas y Enter o Espacio.
 */
public final class InicioScreen {

    public static final String ID = "inicio";

    /** Rutas de Inicio: abrir un evento o crear uno; recuerda el evento anterior que se consulta. */
    public static final class Routes {
        private final InicioQuery query;
        private final Consumer<String> go;
        private String selected;

        Routes(InicioQuery query, Consumer<String> go) {
            this.query = query;
            this.go = go;
        }

        /** Lleva a la pelea activa del evento, a su cartelera editable o a su cartelera de consulta. */
        public void open(String id) {
            switch (query.destino(id)) {
                case EN_VIVO -> go.accept(RoundsScreen.ID);
                case EVENTO -> go.accept(FuncionScreen.ID);
                case CARTELERA -> {
                    selected = id;
                    go.accept(CarteleraScreen.ID);
                }
            }
        }

        public void nuevo() {
            go.accept(NuevoEventoScreen.ID);
        }

        /** El evento anterior que muestra {@link CarteleraScreen}. */
        public String selected() {
            return selected;
        }
    }

    private final InicioQuery query;
    private final Consumer<String> onOpen;
    private final Runnable onNuevo;
    private final Executor reads;

    public InicioScreen(InicioQuery query, Consumer<String> onOpen, Runnable onNuevo) {
        this(query, onOpen, onNuevo, LiveRefresh.DIRECT);
    }

    /** Con las lecturas en {@code reads}: mientras no llegan, la lista muestra su skeleton. */
    public InicioScreen(InicioQuery query, Consumer<String> onOpen, Runnable onNuevo, Executor reads) {
        this.query = query;
        this.onOpen = onOpen;
        this.onNuevo = onNuevo;
        this.reads = reads;
    }

    /**
     * Registra Inicio, Nuevo evento y la cartelera de consulta. {@code go} muestra una pantalla por id; al crear un
     * evento lleva a su cartelera (paso Evento).
     */
    public static Routes register(Screens<Node> screens, InicioQuery query, FuncionService funciones,
            Consumer<String> go) {
        var routes = new Routes(query, go);
        var inicio = new InicioScreen(query, routes::open, routes::nuevo, LiveRefresh.BACKGROUND);
        screens.register(ID, "Inicio", inicio::build);
        var nuevo = new NuevoEventoScreen(funciones, id -> go.accept(FuncionScreen.ID));
        screens.register(NuevoEventoScreen.ID, "Nuevo evento", nuevo::build);
        var cartelera = new CarteleraScreen(funciones, routes::selected, LiveRefresh.BACKGROUND);
        screens.register(CarteleraScreen.ID, "Cartelera", cartelera::build);
        return routes;
    }

    public Node build() {
        var nuevo = Ui.primary("Nuevo evento");
        nuevo.setOnAction(e -> onNuevo.run());
        var head = new HBox(16, Ui.screenTitle("Eventos"), Ui.grow(), nuevo);
        head.setAlignment(Pos.CENTER_LEFT);
        var list = new VBox(8, Skeleton.lines(3));
        var next = Ui.next("Abre un evento o crea uno nuevo.");
        var root = new VBox(24, head, next, list);
        root.setMaxWidth(900);
        LiveRefresh.watch(root, reads, query::eventos, eventos -> {
            Ui.next(next, eventos.isEmpty() ? "Crea tu primer evento con Nuevo evento." : "Abre un evento o crea uno nuevo.");
            LiveRefresh.swap(list, rows(eventos));
        });
        return root;
    }

    private List<Node> rows(List<InicioQuery.Evento> eventos) {
        var out = new ArrayList<Node>();
        if (eventos.isEmpty()) {
            // DESIGN v5: estado vacío dibujado, con el ícono del evento en una insignia.
            var badge = new javafx.scene.layout.StackPane(com.sigebox.central.ui.theme.Icons.icon("calendar", 32));
            badge.getStyleClass().add("empty-badge");
            badge.setMinSize(72, 72);
            badge.setMaxSize(72, 72);
            var empty = Ui.card(badge, Ui.body("Aún no hay eventos."), Ui.support("Crea el primero con \"Nuevo evento\"."));
            empty.setAlignment(Pos.CENTER);
            empty.getStyleClass().add("empty-state");
            out.add(empty);
        }
        for (var ev : eventos) {
            out.add(row(ev));
        }
        return out;
    }

    private Button row(InicioQuery.Evento ev) {
        var name = Ui.body(ev.nombre());
        name.setStyle("-fx-font-weight: 600; -fx-font-size: 16px;");
        var top = new HBox(12, name, Ui.grow(), Ui.support(ev.fecha()));
        top.setAlignment(Pos.CENTER_LEFT);
        var state = ev.ultimo() ? Ui.chip("En curso", "pending", null) : Ui.support("");
        var bottom = new HBox(12, Ui.support(ev.resumen()), Ui.grow(), state, Ui.support("›"));
        bottom.setAlignment(Pos.CENTER_LEFT);
        // DESIGN v5: tarjeta de evento con su ícono a la izquierda; se eleva al pasar el mouse.
        var icon = new javafx.scene.layout.StackPane(com.sigebox.central.ui.theme.Icons.icon("calendar", 20));
        icon.getStyleClass().add("event-icon");
        icon.setMinSize(44, 44);
        icon.setMaxSize(44, 44);
        var text = new VBox(4, top, bottom);
        HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);
        var line = new HBox(16, icon, text);
        line.setAlignment(Pos.CENTER_LEFT);
        var content = Ui.card(line);
        content.setPadding(new javafx.geometry.Insets(16, 20, 16, 20));
        content.setSpacing(4);
        com.sigebox.central.ui.theme.Motion.lift(content);
        var b = new Button();
        b.getStyleClass().setAll("event-row");
        b.setGraphic(content);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFocusTraversable(true);
        b.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");
        content.prefWidthProperty().bind(b.widthProperty());
        b.setAccessibleText(ev.nombre() + ", " + ev.fecha() + ", " + ev.resumen());
        b.setOnAction(e -> onOpen.accept(ev.id()));
        return b;
    }
}
