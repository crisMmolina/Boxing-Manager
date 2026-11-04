package com.sigebox.central.ui.theme;

import com.sigebox.central.ui.help.HelpButton;
import com.sigebox.central.ui.help.HelpPanel;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Capturas de los componentes v4 (barra, campos, botones, skeleton, "?" y panel de ayuda) en claro y oscuro,
 * evidencia de DSK-026. Solo con {@code SIGEBOX_SCREENSHOTS=<carpeta>} y una pantalla disponible.
 */
@EnabledIfEnvironmentVariable(named = "SIGEBOX_SCREENSHOTS", matches = ".+")
class ThemeScreenshots {

    @Test
    void captureComponentsInBothThemes() throws Exception {
        var out = Path.of(System.getenv("SIGEBOX_SCREENSHOTS"));
        Files.createDirectories(out);
        FxTest.onFx(() -> {
            Theme.loadFonts();
            Theme.setReducedMotion(true); // capturas quietas
            return null;
        });
        for (var mode : Theme.Mode.values()) {
            var suffix = mode == Theme.Mode.LIGHT ? "claro" : "oscuro";
            if (Theme.mode() != mode) {
                Theme.toggle();
            }
            write(out.resolve("componentes-" + suffix + ".png"), FxTest.onFx(() -> snapshot(page(false))));
            write(out.resolve("ayuda-" + suffix + ".png"), FxTest.onFx(() -> snapshot(page(true))));
        }
        Theme.setReducedMotion(null);
    }

    private static BorderPane page(boolean help) {
        var root = new BorderPane();
        root.getStyleClass().add("app-root");
        var bar = new StepsBar(
                List.of(new StepsBar.Item("evento", "Evento"), new StepsBar.Item("jueces", "Jueces"),
                        new StepsBar.Item("vivo", "En vivo"), new StepsBar.Item("resultado", "Resultado")),
                List.of(new StepsBar.Item("terminacion", "Terminación"), new StepsBar.Item("bitacora", "Bitácora")),
                id -> { });
        bar.select("jueces");
        root.setTop(bar);

        var name = new TextField();
        var code = new TextField();
        var form = Ui.card(Ui.cardTitle("Nuevo evento"),
                Ui.field("Nombre (opcional)", "Noche de box Iztapalapa", name),
                Ui.field("Código", "123456", code),
                Ui.support("6 dígitos, te lo da la mesa."),
                new HBox(12, Ui.primary("Crear evento"), Ui.secondary("Cancelar")));
        var judges = Ui.card(Ui.cardTitle("Jueces"),
                HelpButton.connection(true, "Juez 1"),
                HelpButton.connection(false, "Juez 2"),
                Ui.cardTitle("Cargando"), Skeleton.lines(3), Skeleton.block(320, 56));
        form.setPrefWidth(460);
        judges.setPrefWidth(460);
        var content = new HBox(24, form, judges);
        content.getStyleClass().add("content");
        content.setAlignment(Pos.TOP_CENTER);
        root.setCenter(new VBox(Ui.screenTitle("Componentes v4"), content));
        if (help) {
            HelpPanel.openIn(bar);
        }
        return root;
    }

    private static WritableImage snapshot(BorderPane root) {
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
