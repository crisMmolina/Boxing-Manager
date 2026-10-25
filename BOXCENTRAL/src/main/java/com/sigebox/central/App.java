package com.sigebox.central;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.export.ExportService;
import com.sigebox.central.server.JudgeHub;
import com.sigebox.central.server.JudgeServer;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.server.ServerEvents;
import com.sigebox.central.service.CorrectionService;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.RoundService;
import com.sigebox.central.service.TerminationService;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.correction.CorrectionScreen;
import com.sigebox.central.ui.log.LogQuery;
import com.sigebox.central.ui.log.LogScreen;
import com.sigebox.central.ui.result.ResultStepScreen;
import com.sigebox.central.ui.rounds.RoundsScreen;
import com.sigebox.central.ui.termination.TerminationScreen;
import com.sigebox.central.ui.terms.TermsGate;
import com.sigebox.central.ui.terms.TermsScreen;
import com.sigebox.central.ui.theme.StepsBar;
import com.sigebox.central.ui.theme.Theme;
import com.sigebox.central.ui.theme.Ui;
import java.util.List;
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

    /** Pasos de la barra superior (DESIGN v2 §7), en orden. */
    static final List<StepsBar.Item> STEPS = List.of(
            new StepsBar.Item(SetupPlaceholder.PELEA, "Pelea"),
            new StepsBar.Item(SetupPlaceholder.JUECES, "Jueces"),
            new StepsBar.Item(RoundsScreen.ID, "En vivo"),
            new StepsBar.Item(ResultStepScreen.ID, "Resultado"));

    /** Enlaces secundarios a la derecha de la barra. */
    static final List<StepsBar.Item> LINKS = List.of(
            new StepsBar.Item(TerminationScreen.ID, "Terminación"),
            new StepsBar.Item(CorrectionScreen.ID, "Corrección"),
            new StepsBar.Item(LogScreen.ID, "Bitácora"));

    private App() {
    }

    public static void main(String[] args) {
        Application.launch(Window.class, args);
    }

    /** Pantalla con la que arranca la app: los términos si no están aceptados. */
    static String firstScreen(TermsGate gate) {
        return gate.firstScreen(RoundsScreen.ID);
    }

    /** Crear pelea y emparejar jueces llegan con DSK-005; hasta entonces, un aviso. */
    static final class SetupPlaceholder {
        static final String PELEA = "pelea";
        static final String JUECES = "jueces";

        private SetupPlaceholder() {
        }

        static Node build(String title) {
            var card = Ui.glass(Ui.cardTitle(title),
                    Ui.support("Crear la pelea, el QR y el código de cada lugar, y aprobar a los jueces."));
            card.setMaxWidth(520);
            return new VBox(16, Ui.screenTitle(title), card);
        }
    }

    /** Ventana principal: barra de pasos arriba y la pantalla activa al centro. */
    public static final class Window extends Application {

        private Database db;
        private JudgeHub hub;
        private JudgeServer server;

        @Override
        public void start(Stage stage) {
            Theme.loadFonts();
            db = Database.openDefault();
            hub = new JudgeHub(db, new SeatTokens(), ServerEvents.NONE);
            String serverError = null;
            try {
                server = JudgeServer.start(hub, JudgeServer.DEFAULT_PORT);
            } catch (RuntimeException e) {
                serverError = "No se pudo iniciar la conexión local. Revisa que la computadora esté en la red "
                        + "de la función.";
            }

            var peleas = new PeleaRepository(db);
            Supplier<String> fightId = () -> peleas.findActive().map(p -> p.id()).orElse(null);
            var gate = new TermsGate(db);

            var content = new StackPane();
            var root = new BorderPane();
            root.getStyleClass().add("app-root");
            var screens = new Screens<Node>();
            var bar = new StepsBar(STEPS, LINKS, id -> show(screens, root, content, id));
            root.setTop(bar);
            root.setCenter(content);

            TermsScreen.register(screens, gate, () -> show(screens, root, content, RoundsScreen.ID));
            screens.register(SetupPlaceholder.PELEA, "Pelea", () -> SetupPlaceholder.build("Pelea"));
            screens.register(SetupPlaceholder.JUECES, "Jueces", () -> SetupPlaceholder.build("Jueces"));
            RoundsScreen.register(screens, RoundService.forHub(db, hub), fightId);
            ResultStepScreen.register(screens, ResultService.forHub(db, hub), new ExportService(), fightId);
            TerminationScreen.register(screens, TerminationService.forHub(db, hub), fightId);
            CorrectionScreen.register(screens, new CorrectionService(db, System::currentTimeMillis), fightId);
            LogScreen.register(screens, new LogQuery(db));
            screens.onChange(s -> {
                bar.select(s.id());
                bar.setDisable(TermsScreen.ID.equals(s.id()));
            });

            show(screens, root, content, firstScreen(gate));

            var scene = new Scene(root, 1280, 800);
            Theme.apply(scene);
            // Ctrl+Enter lleva al tablero en vivo (DESIGN v2 §7.2).
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
                    () -> show(screens, root, content, RoundsScreen.ID));
            stage.setTitle(TITLE);
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setScene(scene);
            stage.show();
            if (serverError != null) {
                var msg = serverError;
                Platform.runLater(() -> Ui.toast(content, msg));
            }
        }

        private static void show(Screens<Node> screens, BorderPane root, StackPane content, String id) {
            var node = screens.show(id);
            var page = new VBox(node);
            page.getStyleClass().add("content");
            content.getChildren().setAll(page);
        }

        @Override
        public void stop() {
            if (server != null) {
                server.close();
            }
            if (hub != null) {
                hub.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }
}
