package com.sigebox.central.ui.setup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.server.JudgeHub;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.server.ServerEvents;
import com.sigebox.central.service.FightService;
import com.sigebox.central.ui.LiveRefresh;
import com.sigebox.central.ui.theme.FxTest;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Spec 005 §Jueces: las peleas se crean en Evento; Jueces solo empareja la pelea que sigue y muestra a los jueces
 * únicamente códigos de 6 números.
 */
class SetupScreenTest {

    @TempDir
    Path dir;

    private Database db;
    private FightService fights;
    private JudgeHub hub;

    @BeforeAll
    static void fx() throws Exception {
        FxTest.start();
    }

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        var tokens = new SeatTokens();
        fights = new FightService(db, tokens, () -> LocalDate.of(2026, 11, 6));
        hub = new JudgeHub(db, tokens, ServerEvents.NONE);
    }

    private final List<Scene> scenes = new ArrayList<>();

    @AfterEach
    void tearDown() throws Exception {
        // Fuera de la escena la pantalla deja de leer; así no lee una base ya cerrada.
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

    private static List<String> texts(Node root) {
        return all(root).stream().filter(n -> n instanceof Labeled l && l.getText() != null)
                .map(n -> ((Labeled) n).getText()).toList();
    }

    /** Arma Jueces en una escena (así lee) y espera a que deje de mostrar el skeleton. */
    private Node open(String expected) throws Exception {
        var screen = new SetupScreen(fights, hub, 8765, () -> { }, LiveRefresh.DIRECT);
        var root = FxTest.onFx(() -> {
            var node = screen.build();
            scenes.add(new Scene(new StackPane(node), 1280, 800));
            return node;
        });
        long deadline = System.nanoTime() + 3_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (FxTest.onFx(() -> texts(root).stream().anyMatch(t -> t.contains(expected)))) {
                return root;
            }
            Thread.sleep(20);
        }
        throw new AssertionError(expected + " no apareció: " + FxTest.onFx(() -> texts(root)));
    }

    @Test
    void sinPeleaNoHayFormularioParaCrearlaYSeñalaEvento() throws Exception {
        var root = open("en Evento");
        var inputs = FxTest.onFx(() -> all(root).stream()
                .filter(n -> n instanceof TextField || n instanceof Spinner).count());
        assertTrue(inputs == 0, "Jueces no debe pedir esquinas, rounds ni jueces: " + inputs + " campos");
        assertFalse(FxTest.onFx(() -> texts(root)).contains("Crear pelea"));
    }

    @Test
    void conPeleaSoloSeVenCodigosDeSeisNumeros() throws Exception {
        var pelea = fights.create("Rojo", "Azul", 3, 2);
        var root = open("Juez 2");
        var shown = FxTest.onFx(() -> texts(root));
        assertFalse(shown.stream().anyMatch(t -> t.contains("Código de la pelea")), shown.toString());
        assertFalse(shown.contains(pelea.id()), "el id de la pelea no se muestra: " + shown);
        long codes = shown.stream().filter(t -> t.matches("\\d{3} \\d{3}")).count();
        assertTrue(codes == 2, "un código de 6 números por lugar: " + shown);
    }
}
