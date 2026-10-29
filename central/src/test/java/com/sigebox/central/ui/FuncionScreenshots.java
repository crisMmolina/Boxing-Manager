package com.sigebox.central.ui;

import com.sigebox.central.export.ExportService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.ui.funcion.FuncionFlow;
import com.sigebox.central.ui.funcion.FuncionQuery;
import com.sigebox.central.ui.funcion.FuncionScreen;
import com.sigebox.central.ui.result.ResultStepScreen;
import com.sigebox.central.ui.rounds.RoundsScreen;
import com.sigebox.central.ui.setup.SetupScreen;
import com.sigebox.central.ui.theme.StepsBar;
import com.sigebox.central.ui.theme.Theme;
import com.sigebox.judgesim.SimJudge;
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
 * Capturas de la función de 3 peleas con jueces de judge-sim (evidencia de DSK-021). No corre en CI: solo con
 * {@code SIGEBOX_SCREENSHOTS=<carpeta>} y una pantalla disponible. Usa un puerto libre, nunca el 8765.
 */
@EnabledIfEnvironmentVariable(named = "SIGEBOX_SCREENSHOTS", matches = ".+")
class FuncionScreenshots {

    private static final int ROUNDS = 3;

    @TempDir
    Path tmp;

    @Test
    void captureTheFunctionFlow() throws Exception {
        var out = Path.of(System.getenv("SIGEBOX_SCREENSHOTS"));
        Files.createDirectories(out);
        startFx();
        onFx(() -> {
            Theme.loadFonts();
            return null;
        });
        try (var rig = new JudgeSimRig(tmp.resolve("sigebox.db"))) {
            var flow = new FuncionFlow(rig.funciones, new FuncionQuery(rig.db));
            var f = rig.funciones.crear(ROUNDS, 2, List.of(new Esquinas("Marco Reyes", "Luis Vega"),
                    new Esquinas("Ana Torres", "Bea Salas"), new Esquinas("Diego Cruz", "Hugo Mena")));
            rig.funciones.cambiar(f.id(), 2, null, 3); // la segunda con 3 jueces
            rig.funciones.cambiar(f.id(), 3, 6, null); // la estelar a 6 rounds

            shots(out, "01-funcion-programada", FuncionScreen.ID, flow, rig);

            var f1 = flow.start().id();
            rig.join(f1, 1, ROUNDS);
            rig.join(f1, 2, ROUNDS);
            JudgeSimRig.await(() -> rig.judges.stream().allMatch(j -> j.phase() == SimJudge.Phase.APPROVED), 20_000,
                    "aprobación");
            rig.play(f1, ROUNDS);
            rig.finish(f1);
            shots(out, "02-resultado-siguiente-pelea", ResultStepScreen.ID, flow, rig);
            shots(out, "03-funcion-primera-cerrada", FuncionScreen.ID, flow, rig);

            var f2 = flow.start().id();
            JudgeSimRig.await(() -> rig.judges.stream().allMatch(j -> j.fightId().equals(f2)
                    && j.phase() == SimJudge.Phase.APPROVED) && rig.hub.connectedSeats(f2).equals(List.of(1, 2)),
                    5_000, "los dos jueces en la pelea 2");
            shots(out, "04-jueces-ya-aprobados-qr-solo-lugar-nuevo", "setup", flow, rig);
            shots(out, "05-funcion-segunda-en-curso", FuncionScreen.ID, flow, rig);

            rig.funciones.saltar(f.id(), 3);
            shots(out, "06-funcion-tercera-saltada", FuncionScreen.ID, flow, rig);
        }
    }

    private void shots(Path out, String name, String id, FuncionFlow flow, JudgeSimRig rig) throws Exception {
        for (var mode : Theme.Mode.values()) {
            onFx(() -> {
                if (Theme.mode() != mode) {
                    Theme.toggle();
                }
                return null;
            });
            String suffix = mode == Theme.Mode.LIGHT ? "claro" : "oscuro";
            write(out.resolve(name + "-" + suffix + ".png"), onFx(() -> snapshot(id, flow, rig)));
        }
    }

    private WritableImage snapshot(String id, FuncionFlow flow, JudgeSimRig rig) {
        var screens = new Screens<Node>();
        FuncionScreen.register(screens, flow, () -> { });
        ResultStepScreen.register(screens, rig.results, new ExportService(), () -> rig.fights.active().map(p -> p.id())
                .orElse(null), flow::peek, flow::start);
        new SetupScreen(rig.fights, rig.hub, 8765, () -> { }).register(screens, "setup", "Pelea");
        var root = new BorderPane();
        root.getStyleClass().add("app-root");
        var bar = new StepsBar(List.of(new StepsBar.Item("setup", "Pelea"), new StepsBar.Item("jueces", "Jueces"),
                new StepsBar.Item(RoundsScreen.ID, "En vivo"), new StepsBar.Item(ResultStepScreen.ID, "Resultado")),
                List.of(new StepsBar.Item(FuncionScreen.ID, "Función")), x -> { });
        bar.select(id);
        root.setTop(bar);
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
    }

    private static WritableImage snapshot(Parent root) {
        var scene = new Scene(root, 1280, 800);
        Theme.apply(scene);
        root.applyCss();
        root.layout();
        var img = scene.snapshot(null);
        scene.setRoot(new StackPane()); // detiene los refrescos periódicos de la pantalla
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
