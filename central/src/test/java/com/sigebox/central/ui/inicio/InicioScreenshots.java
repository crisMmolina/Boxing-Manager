package com.sigebox.central.ui.inicio;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.ui.splash.Splash;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.central.ui.theme.StepsBar;
import com.sigebox.central.ui.theme.Theme;
import com.sigebox.core.model.EstadoPelea;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/**
 * Capturas de la pantalla de carga, Inicio, Nuevo evento y la cartelera de consulta en claro y oscuro (evidencia
 * de DSK-029). Solo con {@code SIGEBOX_SCREENSHOTS=<carpeta>} y una pantalla disponible.
 */
@EnabledIfEnvironmentVariable(named = "SIGEBOX_SCREENSHOTS", matches = ".+")
class InicioScreenshots {

    @TempDir
    Path dir;

    @Test
    void captureInBothThemes() throws Exception {
        var out = Path.of(System.getenv("SIGEBOX_SCREENSHOTS"));
        Files.createDirectories(out);
        try (var db = Database.open(dir.resolve("sigebox.db"))) {
            var funciones = new FuncionService(db, new FightService(db, new SeatTokens(), LocalDate::now));
            var mayo = funciones.crear("Velada de mayo", LocalDate.of(2026, 5, 1), 6, 3,
                    List.of(new Esquinas("Ana Torres", "Bea Ruiz"), new Esquinas("Caro Díaz", "Dani Gil")));
            var p = funciones.siguiente(mayo.id());
            new PeleaRepository(db).updateEstado(p.id(), EstadoPelea.CERRADA);
            funciones.crear("", LocalDate.of(2026, 10, 27), 4, 3, Collections.nCopies(6, new Esquinas("", "")));
            funciones.crear("Noche de box Iztapalapa", LocalDate.of(2026, 11, 3), 10, 3,
                    Collections.nCopies(8, new Esquinas("", "")));
            var query = new InicioQuery(db, funciones);
            FxTest.onFx(() -> {
                Theme.loadFonts();
                Theme.setReducedMotion(true);
                return null;
            });
            for (var mode : Theme.Mode.values()) {
                if (Theme.mode() != mode) {
                    Theme.toggle();
                }
                var suffix = mode == Theme.Mode.LIGHT ? "claro" : "oscuro";
                write(out.resolve("01-carga-" + suffix + ".png"), FxTest.onFx(() -> snapshot(
                        new Splash<String>(() -> "x", v -> { }))));
                write(out.resolve("02-carga-error-" + suffix + ".png"), errorShot());
                write(out.resolve("03-inicio-" + suffix + ".png"), FxTest.onFx(() -> snapshot(page(InicioScreen.ID,
                        () -> new InicioScreen(query, id -> { }, () -> { }).build()))));
                write(out.resolve("04-nuevo-evento-" + suffix + ".png"), FxTest.onFx(() -> snapshot(page(InicioScreen.ID,
                        () -> new NuevoEventoScreen(funciones, id -> { }).build()))));
                write(out.resolve("05-cartelera-anterior-" + suffix + ".png"), FxTest.onFx(() -> snapshot(page(
                        InicioScreen.ID, () -> new CarteleraScreen(funciones, () -> mayo.id()).build()))));
            }
            Theme.setReducedMotion(null);
        }
    }

    /** La carga con error: el trabajo falla en otro hilo, así que se espera a que el error quede a la vista. */
    private static WritableImage errorShot() throws Exception {
        var holder = new Splash<?>[1];
        FxTest.onFx(() -> {
            var s = new Splash<String>(() -> {
                throw new IllegalStateException("x");
            }, v -> { });
            holder[0] = s;
            s.start();
            return null;
        });
        for (int i = 0; i < 50; i++) {
            Thread.sleep(100);
            if (FxTest.onFx(() -> holder[0].errorText()) != null) {
                break;
            }
        }
        return FxTest.onFx(() -> snapshot(holder[0]));
    }

    private static Parent page(String selected, Supplier<Node> screen) {
        var root = new BorderPane();
        root.getStyleClass().add("app-root");
        var bar = new StepsBar(Nav.STEPS, Nav.LINKS, id -> { });
        bar.select(selected);
        root.setTop(bar);
        var node = screen.get();
        node.getStyleClass().add("screen");
        var col = new VBox(node);
        col.getStyleClass().add("content");
        col.setAlignment(Pos.TOP_CENTER);
        var scroll = new ScrollPane(col);
        scroll.setFitToWidth(true);
        root.setCenter(scroll);
        return root;
    }

    private static WritableImage snapshot(Parent root) {
        var scene = new Scene(root, 1280, 800);
        Theme.apply(scene);
        root.applyCss();
        root.layout();
        return scene.snapshot(null);
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
