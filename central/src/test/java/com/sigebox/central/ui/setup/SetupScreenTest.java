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

    @Test
    void cadaLugarEsUnaTarjetaConCodigoGrandeQrYChip() throws Exception {
        fights.create("Rojo", "Azul", 3, 2);
        var root = open("Lugar 2");
        var cards = FxTest.onFx(() -> all(root).stream()
                .filter(n -> n.getStyleClass().contains("seat-card")).toList());
        assertTrue(cards.size() == 2, "una tarjeta por lugar: " + cards.size());
        for (var card : cards) {
            var nodes = FxTest.onFx(() -> all(card));
            var shown = FxTest.onFx(() -> texts(card));
            assertTrue(nodes.stream().anyMatch(n -> n.getStyleClass().contains("chip")), "chip de estado: " + shown);
            assertTrue(shown.contains("Sin conectar"), shown.toString());
            assertTrue(nodes.stream().anyMatch(n -> n.getStyleClass().contains("seat-code")
                    && n instanceof Labeled l && l.getText().matches("\\d{3} \\d{3}")), "código grande: " + shown);
            assertTrue(nodes.stream().anyMatch(n -> n instanceof javafx.scene.image.ImageView
                    && n.getStyleClass().contains("seat-qr")), "QR sobre blanco: " + shown);
            assertTrue(shown.stream().anyMatch(t -> t.startsWith("Lugar ")), shown.toString());
        }
    }

    private static List<String> paths(Node node) {
        return all(node).stream().filter(n -> n instanceof javafx.scene.shape.SVGPath)
                .map(n -> ((javafx.scene.shape.SVGPath) n).getContent()).toList();
    }

    private static void layout(Node root) {
        var top = root.getScene().getRoot();
        top.applyCss();
        top.layout();
    }

    @Test
    void sinConectarUsaElIconoWifiOff() throws Exception {
        fights.create("Rojo", "Azul", 3, 2);
        var root = open("Lugar 2");
        var chips = FxTest.onFx(() -> all(root).stream()
                .filter(n -> n instanceof Labeled l && "Sin conectar".equals(l.getText()))
                .map(n -> paths(((Labeled) n).getGraphic())).toList());
        var wifiOff = FxTest.onFx(() -> paths(com.sigebox.central.ui.theme.Icons.icon("wifi-off", 14)));
        assertFalse(chips.isEmpty(), "hay chips Sin conectar");
        for (var chip : chips) {
            assertTrue(chip.equals(wifiOff), "Sin conectar usa wifi-off: " + chip);
        }
    }

    @Test
    void lasTarjetasDeLugarLlenanElAnchoDeLaFila() throws Exception {
        fights.create("Rojo", "Azul", 3, 2);
        var root = open("Lugar 2");
        var gap = FxTest.onFx(() -> {
            layout(root);
            var cards = all(root).stream().filter(n -> n.getStyleClass().contains("seat-card")).toList();
            var row = (javafx.scene.layout.Region) cards.get(0).getParent();
            double used = cards.stream().mapToDouble(c -> c.getLayoutBounds().getWidth()).sum()
                    + 16 * (cards.size() - 1);
            return row.getWidth() - row.snappedLeftInset() - row.snappedRightInset() - used;
        });
        assertTrue(Math.abs(gap) <= 2, "las tarjetas llenan la fila; sobran " + gap + " px");
    }

    @Test
    void elAvisoDeJuecesPendientesVaCentrado() throws Exception {
        fights.create("Rojo", "Azul", 3, 2);
        var root = open("Falta que se conecten");
        var sides = FxTest.onFx(() -> {
            layout(root);
            var why = all(root).stream().filter(n -> n instanceof Labeled l && l.getText() != null
                    && l.getText().startsWith("Falta que se conecte")).findFirst().orElseThrow();
            var row = (javafx.scene.layout.Region) why.getParent();
            double mid = why.getBoundsInParent().getCenterX();
            return new double[] {mid, row.getWidth() / 2};
        });
        assertTrue(Math.abs(sides[0] - sides[1]) <= 2,
                "el aviso va al centro de la fila: " + sides[0] + " vs " + sides[1]);
    }
}
