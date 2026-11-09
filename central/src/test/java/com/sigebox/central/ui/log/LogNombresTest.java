package com.sigebox.central.ui.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.ui.LiveRefresh;
import com.sigebox.central.ui.theme.FxTest;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Spec 005 (DSK-039): la Bitácora nombra cada pelea por su lugar en el evento; el id interno no se muestra. */
class LogNombresTest {

    @TempDir
    Path dir;

    private Database db;
    private FuncionService funciones;

    @BeforeAll
    static void fx() throws Exception {
        FxTest.start();
    }

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        funciones = new FuncionService(db, new FightService(db, new SeatTokens(), () -> LocalDate.of(2026, 11, 9)));
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void cadaPeleaSeNombraPorSuLugarYSusEsquinas() {
        var id = funciones.crear(10, 3, List.of(new Esquinas("Ana", "Bea"), new Esquinas("Caro", "Dani"))).id();
        var pelea = funciones.siguiente(id);

        assertEquals("Pelea 1 · Ana vs Bea", new LogQuery(db).nombres().get(pelea.id()));
    }

    @Test
    void laTablaYElFiltroNoMuestranElIdDeLaPelea() throws Exception {
        var id = funciones.crear(10, 3, List.of(new Esquinas("Ana", "Bea"))).id();
        var pelea = funciones.siguiente(id);
        new EventLog(db).append(pelea.id(), "JOIN_APPROVED", "lugar 1");

        var texts = FxTest.onFx(() -> {
            var root = new StackPane(LogScreen.build(new LogQuery(db), LiveRefresh.DIRECT));
            new Scene(root, 1280, 800);
            root.applyCss();
            root.layout();
            var out = new ArrayList<String>();
            for (var n : all(root)) {
                if (n instanceof TableView<?> t) {
                    for (var c : t.getColumns()) {
                        for (int r = 0; r < t.getItems().size(); r++) {
                            out.add(String.valueOf(c.getCellData(r)));
                        }
                    }
                }
                if (n instanceof ComboBox<?> c && c.getConverter() != null) {
                    for (var item : c.getItems()) {
                        @SuppressWarnings("unchecked")
                        var conv = (javafx.util.StringConverter<Object>) c.getConverter();
                        out.add(conv.toString(item));
                    }
                }
            }
            root.getScene().setRoot(new StackPane());
            return out;
        });

        assertTrue(texts.contains("Pelea 1 · Ana vs Bea"), texts.toString());
        assertFalse(texts.stream().anyMatch(t -> t.contains(pelea.id())), texts.toString());
    }

    private static List<Node> all(Node node) {
        var out = new ArrayList<Node>();
        out.add(node);
        if (node instanceof Parent p) {
            p.getChildrenUnmodifiable().forEach(c -> out.addAll(all(c)));
        }
        return out;
    }
}
