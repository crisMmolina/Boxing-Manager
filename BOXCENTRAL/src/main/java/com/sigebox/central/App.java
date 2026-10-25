package com.sigebox.central;

import com.sigebox.central.ui.Screens;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Punto de entrada de la central. {@code App} no extiende {@link Application} a propósito: con
 * JavaFX en el classpath (jpackage, installDist) el lanzador de Java exige que la clase main no
 * sea una {@code Application}; por eso la ventana vive en {@link Window}.
 */
public final class App {

    public static final String TITLE = "SIGEBOX · Central";

    private App() {
    }

    public static void main(String[] args) {
        Application.launch(Window.class, args);
    }

    /** Pantallas de arranque; las tareas siguientes registran las suyas aquí. */
    static Screens<Node> screens() {
        return new Screens<Node>()
                .register("inicio", "Inicio", () -> placeholder("Sin pelea activa"));
    }

    private static Node placeholder(String text) {
        var label = new Label(text);
        label.getStyleClass().add("placeholder");
        return label;
    }

    /** Ventana principal: navegación a la izquierda, pantalla activa al centro. */
    public static final class Window extends Application {

        @Override
        public void start(Stage stage) {
            var screens = screens();
            var root = new BorderPane();
            root.getStyleClass().add("root-pane");

            var nav = new VBox(8);
            nav.getStyleClass().add("nav");
            nav.setPadding(new Insets(16));
            for (var screen : screens.all()) {
                var button = new Button(screen.title());
                button.setMaxWidth(Double.MAX_VALUE);
                button.setOnAction(e -> root.setCenter(screens.show(screen.id())));
                nav.getChildren().add(button);
            }
            root.setLeft(nav);
            root.setCenter(screens.show(screens.all().getFirst().id()));

            var scene = new Scene(root, 1024, 680);
            var css = App.class.getResource("app.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
            stage.setTitle(TITLE);
            stage.setScene(scene);
            stage.show();
        }
    }
}
