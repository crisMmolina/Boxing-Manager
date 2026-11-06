package com.sigebox.central;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.RecoveryService;
import com.sigebox.central.export.ExportService;
import com.sigebox.central.server.JudgeHub;
import com.sigebox.central.server.JudgeServer;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.RoundService;
import com.sigebox.central.service.TerminationService;
import com.sigebox.central.ui.LiveRefresh;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.funcion.FuncionFlow;
import com.sigebox.central.ui.funcion.FuncionQuery;
import com.sigebox.central.ui.funcion.FuncionScreen;
import com.sigebox.central.ui.inicio.InicioQuery;
import com.sigebox.central.ui.inicio.InicioScreen;
import com.sigebox.central.ui.inicio.Nav;
import com.sigebox.central.ui.log.LogQuery;
import com.sigebox.central.ui.log.LogScreen;
import com.sigebox.central.ui.result.ResultStepScreen;
import com.sigebox.central.ui.rounds.RoundsScreen;
import com.sigebox.central.ui.setup.SetupScreen;
import com.sigebox.central.ui.splash.Splash;
import com.sigebox.central.ui.termination.TerminationScreen;
import com.sigebox.central.ui.terms.TermsGate;
import com.sigebox.central.ui.terms.TermsScreen;
import com.sigebox.central.ui.theme.StepsBar;
import com.sigebox.central.ui.theme.Theme;
import com.sigebox.central.ui.theme.Ui;
import java.util.function.Supplier;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Punto de entrada de la central. {@code App} no extiende {@link Application} a propósito: con
 * JavaFX en el classpath (jpackage, installDist) el lanzador de Java exige que la clase main no
 * sea una {@code Application}; por eso la ventana vive en {@link Window}.
 */
public final class App {

    public static final String TITLE = "SIGEBOX · Central";

    /** El paso "Jueces" muestra la misma pantalla de preparar la pelea (DESIGN v4 §7). */
    static final String JUECES = Nav.JUECES;

    private App() {
    }

    public static void main(String[] args) {
        Application.launch(Window.class, args);
    }

    /**
     * Funciones con cartelera. Al iniciar la siguiente pelea, el servicio copia los lugares aprobados y
     * después {@link JudgeHub#nextFight} avisa a cada teléfono (ADR 0002); sin este enlace los teléfonos
     * se quedarían en la pelea cerrada.
     */
    public static FuncionService funciones(Database db, FightService fights, JudgeHub hub) {
        var funciones = new FuncionService(db, fights);
        funciones.alSiguiente(hub::nextFight);
        return funciones;
    }

    /** Pantalla después de la carga: los términos si no están aceptados; si no, Inicio. */
    static String firstScreen(TermsGate gate) {
        return gate.firstScreen(InicioScreen.ID);
    }

    /** Ancho máximo de la columna centrada (DESIGN v3 §7). */
    static final double COLUMN_WIDTH = 1100;

    /** Ventana principal: barra de pasos arriba y la pantalla activa al centro. */
    public static final class Window extends Application {

        private volatile Database db;
        private volatile JudgeHub hub;
        private volatile JudgeServer server;

        /** Lo que deja la carga; {@code serverError} si la conexión local no arrancó (la app sigue). */
        private record Loaded(SeatTokens tokens, String serverError) {}

        @Override
        public void start(Stage stage) {
            Theme.loadFonts();
            Theme.followSystem();
            var scene = new Scene(new StackPane(), 1280, 800);
            Theme.apply(scene);
            stage.setTitle(TITLE);
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setScene(scene);
            // Pantalla de carga (DESIGN v4 §5.1) mientras la base migra y se reconstruye la pelea activa.
            var splash = new Splash<Loaded>(this::load, loaded -> main(scene, loaded));
            scene.setRoot(splash);
            stage.show();
            splash.start();
        }

        /** Fuera del hilo de JavaFX: abre y migra la base, reconstruye la pelea activa y arranca el servidor. */
        private Loaded load() {
            stop(); // un reintento empieza limpio
            db = Database.openDefault();
            new RecoveryService(db).load();
            var tokens = new SeatTokens(); // uno solo: lo emite la pantalla de pelea y lo valida el servidor
            hub = new JudgeHub(db, tokens, LiveRefresh.EVENTS);
            String serverError = null;
            try {
                server = JudgeServer.start(hub, JudgeServer.DEFAULT_PORT);
            } catch (RuntimeException e) {
                serverError = "No se pudo iniciar la conexión local. Revisa que la computadora esté en la red "
                        + "del evento.";
            }
            return new Loaded(tokens, serverError);
        }

        /** Arma la ventana principal en la misma escena (el tema y los atajos viven en ella). */
        private void main(Scene scene, Loaded loaded) {
            var peleas = new PeleaRepository(db);
            Supplier<String> fightId = () -> peleas.findActive().map(p -> p.id()).orElse(null);
            var gate = new TermsGate(db);

            var content = new StackPane();
            var root = new BorderPane();
            root.getStyleClass().add("app-root");
            var screens = new Screens<Node>();
            var bar = new StepsBar(Nav.STEPS, Nav.LINKS, id -> show(screens, root, content, id));
            root.setTop(bar);
            root.setCenter(content);

            TermsScreen.register(screens, gate, () -> show(screens, root, content, InicioScreen.ID));
            var fights = new FightService(db, loaded.tokens());
            var service = funciones(db, fights, hub);
            var flow = new FuncionFlow(service, new FuncionQuery(db));
            InicioScreen.register(screens, new InicioQuery(db, service), service,
                    id -> show(screens, root, content, id));
            var setup = new SetupScreen(fights, hub, JudgeServer.DEFAULT_PORT,
                    () -> show(screens, root, content, RoundsScreen.ID));
            setup.register(screens, SetupScreen.ID, "Pelea");
            setup.register(screens, JUECES, "Jueces");
            RoundsScreen.register(screens, RoundService.forHub(db, hub), fightId,
                    () -> show(screens, root, content, ResultStepScreen.ID));
            FuncionScreen.register(screens, flow, () -> show(screens, root, content, JUECES));
            ResultStepScreen.register(screens, ResultService.forHub(db, hub), new ExportService(), fightId,
                    flow::peek, () -> {
                        flow.start();
                        show(screens, root, content, JUECES);
                    });
            TerminationScreen.register(screens, TerminationService.forHub(db, hub), fightId);
            LogScreen.register(screens, new LogQuery(db));
            var current = new String[1];
            screens.onChange(s -> {
                current[0] = s.id();
                Nav.apply(bar, s.id()); // spec 005: Inicio sin barra del evento
                bar.setDisable(TermsScreen.ID.equals(s.id()));
            });

            show(screens, root, content, firstScreen(gate));
            scene.setRoot(root);

            // Ctrl+Enter (DESIGN §7): en el tablero abre o cierra el round; en otra pantalla lleva al tablero.
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN), () -> {
                if (!(RoundsScreen.ID.equals(current[0]) && RoundsScreen.firePrimary(content))) {
                    show(screens, root, content, RoundsScreen.ID);
                }
            });
            if (loaded.serverError() != null) {
                var msg = loaded.serverError();
                Platform.runLater(() -> Ui.toast(content, msg));
            }
        }

        private static void show(Screens<Node> screens, BorderPane root, StackPane content, String id) {
            var node = screens.show(id);
            // Columna centrada de hasta 1100 px, alineada arriba y con desplazamiento si no cabe.
            node.getStyleClass().add("screen");
            if (node instanceof javafx.scene.layout.Region r) {
                r.setMaxWidth(COLUMN_WIDTH);
            }
            var page = new VBox(node);
            page.getStyleClass().add("content");
            page.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            var scroll = new javafx.scene.control.ScrollPane(page);
            scroll.setFitToWidth(true);
            scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            content.getChildren().setAll(scroll);
        }

        @Override
        public void stop() {
            if (server != null) {
                server.close();
                server = null;
            }
            if (hub != null) {
                hub.close();
                hub = null;
            }
            if (db != null) {
                db.close();
                db = null;
            }
        }
    }
}
