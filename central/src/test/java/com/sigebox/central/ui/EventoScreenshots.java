package com.sigebox.central.ui;

import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.export.ExportService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.service.TerminationService;
import com.sigebox.central.ui.funcion.FuncionFlow;
import com.sigebox.central.ui.funcion.FuncionQuery;
import com.sigebox.central.ui.funcion.FuncionScreen;
import com.sigebox.central.ui.inicio.InicioQuery;
import com.sigebox.central.ui.inicio.InicioScreen;
import com.sigebox.central.ui.inicio.Nav;
import com.sigebox.central.ui.log.LogQuery;
import com.sigebox.central.ui.log.LogScreen;
import com.sigebox.central.ui.result.ResultScreen;
import com.sigebox.central.ui.result.ResultStepScreen;
import com.sigebox.central.ui.rounds.RoundsScreen;
import com.sigebox.central.ui.setup.SetupScreen;
import com.sigebox.central.ui.termination.TerminationScreen;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.central.ui.theme.StepsBar;
import com.sigebox.central.ui.theme.Theme;
import com.sigebox.judgesim.SimJudge;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/**
 * Capturas de un evento completo de 2 peleas con jueces de judge-sim (evidencia de DSK-027). No corre en CI:
 * solo con {@code SIGEBOX_SCREENSHOTS=<carpeta>} y una pantalla disponible.
 */
@EnabledIfEnvironmentVariable(named = "SIGEBOX_SCREENSHOTS", matches = ".+")
class EventoScreenshots {

    private static final int ROUNDS = 2;
    /** Lo que se deja a las lecturas de fondo para llegar a la pantalla antes de la captura. */
    private static final long SETTLE_MS = 900;

    @TempDir
    Path tmp;

    private Path out;
    private JudgeSimRig rig;
    private FuncionFlow flow;
    private int shot;

    @Test
    void captureACompleteEvent() throws Exception {
        out = Path.of(System.getenv("SIGEBOX_SCREENSHOTS"));
        Files.createDirectories(out);
        FxTest.start();
        FxTest.onFx(() -> {
            Theme.loadFonts();
            return null;
        });
        try (var r = new JudgeSimRig(tmp.resolve("sigebox.db"))) {
            rig = r;
            flow = new FuncionFlow(rig.funciones, new FuncionQuery(rig.db));
            shot("inicio-vacio", InicioScreen.ID);
            rig.funciones.crear(ROUNDS, 2, List.of(new Esquinas("Marco Reyes", "Luis Vega"),
                    new Esquinas("Ana Torres", "Bea Salas")));
            shot("evento-creado", FuncionScreen.ID);

            var first = flow.start().id();
            shot("jueces-qr", Nav.JUECES);
            rig.join(first, 1, ROUNDS);
            rig.join(first, 2, ROUNDS);
            awaitApproved(first);
            shot("jueces-conectados", Nav.JUECES);

            rig.rounds.openRound(first, 1);
            JudgeSimRig.await(() -> rig.rounds.board(first, 1).missing().isEmpty(), 10_000, "tarjetas del round 1");
            shot("en-vivo-round-abierto", RoundsScreen.ID);
            rig.rounds.closeRound(first, 1, null);
            rig.rounds.openRound(first, 2);
            JudgeSimRig.await(() -> rig.rounds.board(first, 2).missing().isEmpty(), 10_000, "tarjetas del round 2");
            rig.rounds.closeRound(first, 2, null);
            shot("en-vivo-rounds-completos", RoundsScreen.ID);
            rig.finish(first);
            shotResultWindow("ventana-resultado", first);
            shot("resultado-siguiente-pelea", ResultStepScreen.ID);
            shot("evento-primera-cerrada", FuncionScreen.ID);

            var second = flow.start().id();
            awaitApproved(second);
            shot("jueces-segunda-pelea", Nav.JUECES);

            var lost = rig.judges.get(1);
            rig.judges.remove(lost); // sin esto judge-sim reconecta enseguida
            lost.drop();
            JudgeSimRig.await(() -> !rig.hub.connectedSeats(second).contains(2), 5_000, "juez 2 desconectado");
            shot("jueces-juez-desconectado", Nav.JUECES);
            shot("en-vivo-juez-desconectado", RoundsScreen.ID);
            rig.judges.add(lost);
            awaitApproved(second);

            rig.play(second, ROUNDS);
            rig.finish(second);
            shot("resultado-final", ResultStepScreen.ID);
            shot("evento-terminado", FuncionScreen.ID);
            shot("terminacion", TerminationScreen.ID);
            shot("bitacora", LogScreen.ID);
            shot("inicio-con-evento", InicioScreen.ID);
        }
    }

    private void awaitApproved(String fightId) throws Exception {
        JudgeSimRig.await(() -> rig.judges.stream().allMatch(j -> j.fightId().equals(fightId)
                && j.phase() == SimJudge.Phase.APPROVED) && rig.hub.connectedSeats(fightId).equals(List.of(1, 2)),
                20_000, "los dos jueces en " + fightId);
    }

    private void shot(String name, String id) throws Exception {
        shot++;
        for (var mode : Theme.Mode.values()) {
            var scene = FxTest.onFx(() -> {
                if (Theme.mode() != mode) {
                    Theme.toggle();
                }
                return scene(id);
            });
            Thread.sleep(SETTLE_MS);
            var img = FxTest.onFx(() -> {
                scene.getRoot().applyCss();
                scene.getRoot().layout();
                var taken = scene.snapshot(null);
                scene.setRoot(new StackPane()); // detiene los refrescos periódicos de la pantalla
                return taken;
            });
            var suffix = mode == Theme.Mode.LIGHT ? "claro" : "oscuro";
            write(out.resolve("%02d-%s-%s.png".formatted(shot, name, suffix)), img);
        }
    }

    /** La ventana que se abre al cerrar la pelea, que no es un paso de la barra. */
    private void shotResultWindow(String name, String fightId) throws Exception {
        shot++;
        var sheet = rig.results.sheet(fightId);
        for (var mode : Theme.Mode.values()) {
            var img = FxTest.onFx(() -> {
                if (Theme.mode() != mode) {
                    Theme.toggle();
                }
                var root = ResultScreen.content(new Stage(), sheet, new ExportService());
                var scene = new Scene(root, 1280, 800);
                Theme.apply(scene);
                root.applyCss();
                root.layout();
                return scene.snapshot(null);
            });
            var suffix = mode == Theme.Mode.LIGHT ? "claro" : "oscuro";
            write(out.resolve("%02d-%s-%s.png".formatted(shot, name, suffix)), img);
        }
    }

    /** La pantalla {@code id} con la barra de pasos de la app. */
    private Scene scene(String id) {
        var db = rig.db;
        Supplier<String> fightId = () -> new PeleaRepository(db).findActive().map(p -> p.id()).orElse(null);
        var screens = new Screens<Node>();
        InicioScreen.register(screens, new InicioQuery(db, rig.funciones), rig.funciones, x -> { });
        new SetupScreen(rig.fights, rig.hub, rig.server.port(), () -> { }).register(screens, Nav.JUECES, "Jueces");
        RoundsScreen.register(screens, rig.rounds, fightId, () -> { });
        FuncionScreen.register(screens, flow, () -> { });
        ResultStepScreen.register(screens, rig.results, new ExportService(), fightId, flow::peek, () -> { });
        TerminationScreen.register(screens, TerminationService.forHub(db, rig.hub), fightId);
        LogScreen.register(screens, new LogQuery(db));

        var root = new BorderPane();
        root.getStyleClass().add("app-root");
        var bar = new StepsBar(Nav.STEPS, Nav.LINKS, x -> { });
        bar.select(id);
        root.setTop(bar);
        var node = screens.show(id);
        node.getStyleClass().add("screen");
        if (node instanceof Region r) {
            r.setMaxWidth(1100);
        }
        var page = new VBox(node);
        page.getStyleClass().add("content");
        page.setAlignment(Pos.TOP_CENTER);
        var scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        root.setCenter(new StackPane(scroll));
        var scene = new Scene(root, 1280, 800);
        Theme.apply(scene);
        return scene;
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
}
