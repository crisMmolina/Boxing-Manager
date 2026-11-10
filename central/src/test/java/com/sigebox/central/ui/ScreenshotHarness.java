package com.sigebox.central.ui;

import com.google.gson.Gson;
import com.sigebox.central.db.Database;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.db.TarjetaRoundRepository;
import com.sigebox.central.export.ExportService;
import com.sigebox.central.server.JudgeHub;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.server.ServerEvents;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.RoundService;
import com.sigebox.central.service.TerminationService;
import com.sigebox.central.ui.LiveRefresh;
import com.sigebox.central.ui.log.LogQuery;
import com.sigebox.central.ui.log.LogScreen;
import com.sigebox.central.ui.result.ResultScreen;
import com.sigebox.central.ui.result.ResultStepScreen;
import com.sigebox.central.ui.rounds.RoundsScreen;
import com.sigebox.central.ui.setup.SetupScreen;
import com.sigebox.central.ui.termination.TerminarEnVivo;
import com.sigebox.central.ui.termination.TerminationScreen;
import com.sigebox.central.ui.terms.TermsGate;
import com.sigebox.central.ui.terms.TermsScreen;
import com.sigebox.central.ui.theme.StepsBar;
import com.sigebox.central.ui.theme.Theme;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaRound;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/**
 * Capturas de cada pantalla de la central con la pelea de referencia, en tema claro y oscuro (evidencia de
 * DSK-018). No corre en CI: solo con {@code SIGEBOX_SCREENSHOTS=<carpeta>} y una pantalla disponible.
 */
@EnabledIfEnvironmentVariable(named = "SIGEBOX_SCREENSHOTS", matches = ".+")
class ScreenshotHarness {

    private static final Path REFERENCE = Path.of("..", "demo_data", "pelea_referencia.json");

    /** La parte de {@code pelea_referencia.json} que necesitan las pantallas. */
    record Reference(Pelea pelea, List<LugarJuez> lugares, List<Round> rounds, List<TarjetaRound> tarjetas) {}

    @TempDir
    Path tmp;

    @Test
    void captureEveryScreenInBothThemes() throws Exception {
        var out = Path.of(System.getenv("SIGEBOX_SCREENSHOTS"));
        Files.createDirectories(out);
        var ref = new Gson().fromJson(Files.readString(REFERENCE), Reference.class);
        startFx();
        onFx(() -> {
            Theme.loadFonts();
            return null;
        });

        // Preparar la pelea: una pelea nueva con sus 3 lugares por emparejar.
        var setupDb = Database.open(tmp.resolve("setup.db"));
        var tokens = new SeatTokens();
        var setupHub = new JudgeHub(setupDb, tokens, ServerEvents.NONE);
        var fights = new FightService(setupDb, tokens);
        fights.create(ref.pelea().esquinaRoja(), ref.pelea().esquinaAzul(), ref.pelea().rounds(), ref.pelea().jueces());

        // En vivo: rounds 1 y 2 cerrados, round 3 abierto y el juez 2 todavía sin enviar.
        var liveDb = Database.open(tmp.resolve("live.db"));
        seed(liveDb, ref, EstadoPelea.EN_CURSO, 3);
        var liveHub = new JudgeHub(liveDb, new SeatTokens(), ServerEvents.NONE);

        // Pelea terminada con las 12 tarjetas: resultado Roja, dividida 2–1.
        var doneDb = Database.open(tmp.resolve("done.db"));
        seed(doneDb, ref, EstadoPelea.TERMINADA, 0);
        var doneHub = new JudgeHub(doneDb, new SeatTokens(), ServerEvents.NONE);
        String f = ref.pelea().id();

        for (var mode : Theme.Mode.values()) {
            onFx(() -> {
                if (Theme.mode() != mode) {
                    Theme.toggle();
                }
                return null;
            });
            String suffix = mode == Theme.Mode.LIGHT ? "claro" : "oscuro";
            capture(out, "01-terminos-" + suffix, TermsScreen.ID,
                    s -> TermsScreen.register(s, new TermsGate(setupDb), () -> { }));
            capture(out, "02-pelea-" + suffix, "judges",
                    s -> new SetupScreen(fights, setupHub, 8765, () -> { }, LiveRefresh.DIRECT).register(s, "judges", "Jueces"));
            capture(out, "03-en-vivo-" + suffix, RoundsScreen.ID,
                    s -> RoundsScreen.register(s, RoundService.forHub(liveDb, liveHub), () -> f, () -> { },
                            new TerminarEnVivo(TerminationService.forHub(liveDb, liveHub),
                                    ResultService.forHub(liveDb, liveHub), TerminarEnVivo.DIALOGS, x -> { })));
            capture(out, "04-terminacion-" + suffix, TerminationScreen.ID,
                    s -> TerminationScreen.register(s, TerminationService.forHub(liveDb, liveHub), () -> f));
            capture(out, "06-bitacora-" + suffix, LogScreen.ID, s -> LogScreen.register(s, new LogQuery(liveDb)));
            capture(out, "07-cerrar-pelea-" + suffix, ResultStepScreen.ID,
                    s -> ResultStepScreen.register(s, ResultService.forHub(doneDb, doneHub), new ExportService(), () -> f));
            var result = ResultService.forHub(doneDb, doneHub);
            write(out.resolve("08-resultado-" + suffix + ".png"), onFx(() -> {
                var stage = ResultScreen.show(null, result.sheet(f), new ExportService());
                stage.setFullScreen(false);
                stage.setWidth(1280);
                stage.setHeight(800);
                var img = stage.getScene().getRoot().snapshot(null, null);
                stage.close();
                return img;
            }));
        }
        setupHub.close();
        liveHub.close();
        doneHub.close();
        setupDb.close();
        liveDb.close();
        doneDb.close();
    }

    /** Guarda la pelea de referencia; {@code openRound} > 0 deja ese round abierto y los siguientes por abrir. */
    private static void seed(Database db, Reference ref, EstadoPelea estado, int openRound) {
        var p = ref.pelea();
        new PeleaRepository(db).save(new Pelea(p.id(), p.esquinaRoja(), p.esquinaAzul(), p.rounds(), p.jueces(), estado,
                p.fecha()));
        var lugares = new LugarJuezRepository(db);
        ref.lugares().forEach(lugares::save);
        var rounds = new RoundRepository(db);
        for (var r : ref.rounds()) {
            if (openRound == 0 || r.numero() < openRound) {
                rounds.save(r);
            } else if (r.numero() == openRound) {
                rounds.save(new Round(r.peleaId(), r.numero(), EstadoRound.ABIERTO, r.abiertoEnMs(), null, false));
            } else {
                rounds.save(new Round(r.peleaId(), r.numero(), EstadoRound.PENDIENTE, null, null, false));
            }
        }
        var tarjetas = new TarjetaRoundRepository(db);
        for (var t : ref.tarjetas()) {
            boolean missingJudge2 = t.round() == openRound && t.juez() == 2;
            if (openRound == 0 || t.round() < openRound || (t.round() == openRound && !missingJudge2)) {
                tarjetas.save(t);
            }
        }
    }

    private interface Registrar {
        void register(Screens<Node> screens);
    }

    private static void capture(Path out, String name, String id, Registrar registrar) throws Exception {
        write(out.resolve(name + ".png"), onFx(() -> {
            var screens = new Screens<Node>();
            registrar.register(screens);
            var root = new BorderPane();
            root.getStyleClass().add("app-root");
            var bar = new StepsBar(List.of(new StepsBar.Item("setup", "Pelea"), new StepsBar.Item("judges", "Jueces"),
                    new StepsBar.Item(RoundsScreen.ID, "En vivo"), new StepsBar.Item(ResultStepScreen.ID, "Resultado")),
                    List.of(new StepsBar.Item(LogScreen.ID, "Bitácora")), x -> { });
            bar.select(id);
            root.setTop(bar);
            // Igual que App.show: columna centrada de hasta 1100 px con desplazamiento.
            var node = screens.show(id);
            node.getStyleClass().add("screen");
            if (node instanceof javafx.scene.layout.Region r) {
                r.setMaxWidth(1100);
            }
            var page = new javafx.scene.layout.VBox(node);
            page.getStyleClass().add("content");
            page.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            var scroll = new javafx.scene.control.ScrollPane(page);
            scroll.setFitToWidth(true);
            root.setCenter(new StackPane(scroll));
            return snapshot(root);
        }));
    }

    private static WritableImage snapshot(Parent root) {
        var scene = new Scene(root, 1280, 800);
        Theme.apply(scene);
        root.applyCss();
        root.layout();
        var img = scene.snapshot(null);
        // Sacar la pantalla de la escena detiene sus refrescos periódicos antes de cerrar la base.
        scene.setRoot(new StackPane());
        return img;
    }

    private static void write(Path file, WritableImage img) throws Exception {
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        var buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        var px = img.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                buf.setRGB(x, y, px.getArgb(x, y));
            }
        }
        ImageIO.write(buf, "png", file.toFile());
    }

    private static void startFx() throws InterruptedException {
        var ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyStarted) {
            ready.countDown();
        }
        Platform.setImplicitExit(false);
        ready.await(10, TimeUnit.SECONDS);
    }

    private static <T> T onFx(Callable<T> work) throws Exception {
        var result = new AtomicReference<T>();
        var error = new AtomicReference<Exception>();
        var done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(work.call());
            } catch (Exception e) {
                error.set(e);
            } finally {
                done.countDown();
            }
        });
        if (!done.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX no respondió");
        }
        if (error.get() != null) {
            throw error.get();
        }
        return result.get();
    }
}
