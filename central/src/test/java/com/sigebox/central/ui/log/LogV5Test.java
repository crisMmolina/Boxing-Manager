package com.sigebox.central.ui.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.ui.LiveRefresh;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.central.ui.theme.PageHeader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Parent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Spec 006 · DSK-045: Bitácora con el encabezado v5 y el tipo de cada evento como chip. */
class LogV5Test {

    @TempDir
    Path dir;

    private Database db;

    @BeforeAll
    static void fx() throws Exception {
        FxTest.start();
    }

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private static List<Node> all(Node node) {
        var out = new ArrayList<Node>();
        out.add(node);
        if (node instanceof Parent p) {
            p.getChildrenUnmodifiable().forEach(c -> out.addAll(all(c)));
        }
        return out;
    }

    @Test
    void laBitacoraUsaElEncabezadoV5() throws Exception {
        var root = FxTest.onFx(() -> LogScreen.build(new LogQuery(db), LiveRefresh.DIRECT));

        var header = FxTest.onFx(() -> all(root).stream().filter(n -> n instanceof PageHeader)
                .map(PageHeader.class::cast).findFirst().orElseThrow());
        assertEquals("Bitácora", header.titleText());
    }

    @Test
    void elTipoSeMuestraComoChipConSuEtiquetaYUnColorPorClase() throws Exception {
        var ok = FxTest.onFx(() -> LogScreen.typeChip("JOIN_APPROVED"));
        var waiting = FxTest.onFx(() -> LogScreen.typeChip("JOIN_REQUESTED"));
        var other = FxTest.onFx(() -> LogScreen.typeChip("CARTELERA_CAMBIADA"));

        assertTrue(ok.getStyleClass().containsAll(List.of("chip", "success")), ok.getStyleClass().toString());
        assertTrue(waiting.getStyleClass().contains("pending"), waiting.getStyleClass().toString());
        assertTrue(other.getStyleClass().contains("neutral"), other.getStyleClass().toString());
        assertEquals(com.sigebox.central.ui.Labels.evento("CARTELERA_CAMBIADA"), other.getText());
    }
}
