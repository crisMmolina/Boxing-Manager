package com.sigebox.central.ui.funcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.FxTest;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Spec 005 §Evento: "Agregar pelea" antes de los nombres, y la base del evento se puede cambiar en la pantalla. */
class FuncionAddRowTest {

    @TempDir
    Path dir;

    private Database db;
    private FuncionService funciones;
    private final List<Scene> scenes = new ArrayList<>();

    @BeforeAll
    static void fx() throws Exception {
        FxTest.start();
    }

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        funciones = new FuncionService(db, new FightService(db, new SeatTokens(), () -> LocalDate.of(2026, 11, 6)));
    }

    @AfterEach
    void tearDown() throws Exception {
        FxTest.onFx(() -> {
            scenes.forEach(s -> s.setRoot(new StackPane()));
            return null;
        });
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

    private static Optional<Button> addButton(Node root) {
        return all(root).stream().filter(n -> n instanceof Button b && "Agregar pelea".equals(b.getText()))
                .map(Button.class::cast).findFirst();
    }

    private Node open() throws Exception {
        var screens = new Screens<Node>();
        FuncionScreen.register(screens, new FuncionFlow(funciones, new FuncionQuery(db)), () -> { });
        var root = FxTest.onFx(() -> {
            var node = screens.show(FuncionScreen.ID);
            scenes.add(new Scene(new StackPane(node), 1280, 800));
            return node;
        });
        long deadline = System.nanoTime() + 3_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (FxTest.onFx(() -> addButton(root).isPresent())) {
                return root;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("no apareció Agregar pelea");
    }

    @Test
    void agregarPeleaVaALaIzquierdaDeLosNombres() throws Exception {
        funciones.crear(10, 3, List.of(new Esquinas("Ana", "Bea")));
        var root = open();
        var first = FxTest.onFx(() -> {
            var button = addButton(root).orElseThrow();
            var row = (HBox) button.getParent();
            return row.getChildren().get(0) == button;
        });
        assertTrue(first, "Agregar pelea debe ser lo primero de su fila");
    }
}
