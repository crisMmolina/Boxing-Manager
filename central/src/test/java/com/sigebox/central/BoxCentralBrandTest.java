package com.sigebox.central;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.central.ui.theme.Ui;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;

/** DSK-054: la central se ve como «Box Central»; la carpeta de datos no cambia. */
class BoxCentralBrandTest {

    @Test
    void elTituloDeLaVentanaDiceBoxCentral() {
        assertEquals("Box Central", App.TITLE);
    }

    @Test
    void laMarcaDeLaBarraDiceBoxCentral() throws Exception {
        var texts = FxTest.onFx(() -> {
            var out = new ArrayList<String>();
            for (var n : all(Ui.brand())) {
                if (n instanceof Label l) {
                    out.add(l.getText());
                }
            }
            return out;
        });
        assertEquals(List.of("Box Central"), texts);
    }

    @Test
    void elInstaladorSeLlamaBoxCentral() throws Exception {
        var gradle = Files.readString(Path.of("build.gradle.kts"));
        assertTrue(gradle.contains("\"--name\", \"Box Central\""), "jpackage --name");
    }

    @Test
    void laCarpetaDeDatosSigueSiendoSigebox() {
        var appData = System.getenv("APPDATA");
        var expected = appData != null && !appData.isBlank()
                ? Path.of(appData, "SIGEBOX")
                : Path.of(System.getProperty("user.home"), ".sigebox");
        assertEquals(expected, Database.defaultDir());
    }

    private static List<Node> all(Node root) {
        var out = new ArrayList<Node>();
        out.add(root);
        if (root instanceof Parent p) {
            for (var c : p.getChildrenUnmodifiable()) {
                out.addAll(all(c));
            }
        }
        return out;
    }
}
