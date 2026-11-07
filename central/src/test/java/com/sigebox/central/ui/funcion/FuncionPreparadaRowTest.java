package com.sigebox.central.ui.funcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.PeleaRepository;
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
import javafx.scene.control.Spinner;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Spec 005 §Evento (DSK-040): en Evento, la fila de la pelea que espera en Jueces baja sus jueces. */
class FuncionPreparadaRowTest {

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

    private static Optional<Spinner<?>> spinner(Node root, String text) {
        return all(root).stream().filter(n -> n instanceof Spinner<?> s && text.equals(s.getAccessibleText()))
                .<Spinner<?>>map(n -> (Spinner<?>) n).findFirst();
    }

    @Test
    void bajarLosJuecesDelEventoBajaLaPeleaEnJueces() throws Exception {
        var id = funciones.crear(10, 3, List.of(new Esquinas("Ana", "Bea"), new Esquinas("Caro", "Dani"))).id();
        var pelea = funciones.siguiente(id);
        var screens = new Screens<Node>();
        FuncionScreen.register(screens, new FuncionFlow(funciones, new FuncionQuery(db)), () -> { });
        var root = FxTest.onFx(() -> {
            var node = screens.show(FuncionScreen.ID);
            scenes.add(new Scene(new StackPane(node), 1280, 800));
            return node;
        });
        long deadline = System.nanoTime() + 3_000_000_000L;
        while (System.nanoTime() < deadline && !FxTest.onFx(() -> spinner(root, "Jueces del evento").isPresent())) {
            Thread.sleep(20);
        }
        var judges = FxTest.onFx(() -> spinner(root, "Jueces del evento").orElseThrow());
        assertTrue(FxTest.onFx(() -> spinner(root, "Jueces de la pelea 1").isEmpty()), "la fila en Jueces no se edita");

        FxTest.onFx(() -> {
            judges.decrement();
            return null;
        });

        assertEquals(2, new PeleaRepository(db).find(pelea.id()).orElseThrow().jueces());
    }
}
