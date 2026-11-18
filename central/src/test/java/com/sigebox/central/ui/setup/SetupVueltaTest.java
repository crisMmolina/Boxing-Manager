package com.sigebox.central.ui.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.server.Conn;
import com.sigebox.central.server.JudgeHub;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.server.ServerEvents;
import com.sigebox.central.service.FightService;
import com.sigebox.central.ui.LiveRefresh;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.core.protocol.Challenge;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.Join;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** DSK-055 (spec 008 §Volver al lugar): Jueces da un código nuevo a un juez aprobado sin conexión y deja cambiar el teléfono. */
class SetupVueltaTest {

    @TempDir
    Path dir;

    private Database db;
    private FightService fights;
    private JudgeHub hub;
    private String fight;
    private final List<Scene> scenes = new ArrayList<>();

    /** Conexión falsa: guarda lo enviado. */
    private static final class FakeConn implements Conn {
        private static int next;
        final String id = "v" + (++next);
        final List<Message> sent = new ArrayList<>();

        @Override
        public String id() {
            return id;
        }

        @Override
        public void send(String text) {
            sent.add(Messages.decode(text));
        }

        @Override
        public void close(int code, String reason) {
        }

        String nonce() {
            return ((Challenge) sent.get(0)).nonce();
        }
    }

    private static KeyPair keys() throws Exception {
        var gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        return gen.generateKeyPair();
    }

    private void join(KeyPair keys, String device, String code) {
        var c = new FakeConn();
        hub.onOpen(c);
        var sig = DeviceProof.sign(keys.getPrivate(), DeviceProof.joinText(fight, 1, code, device, c.nonce()));
        hub.onMessage(c.id(), Messages.encode(new Join(fight, 1, code, "Cris", device,
                DeviceProof.encodePublicKey(keys.getPublic()), c.nonce(), sig)));
        if (hub.connectedSeats(fight).contains(1) || hub.replacementFingerprint(fight, 1).isEmpty()) {
            hub.onClose(c.id());
        }
    }

    @BeforeAll
    static void fx() throws Exception {
        FxTest.start();
    }

    @BeforeEach
    void setUp() throws Exception {
        db = Database.open(dir.resolve("sigebox.db"));
        var tokens = new SeatTokens();
        fights = new FightService(db, tokens, () -> LocalDate.of(2026, 11, 17));
        hub = new JudgeHub(db, tokens, ServerEvents.NONE);
        fight = fights.create("Rojo", "Azul", 3, 1).id();
        join(keys(), "A-1", fights.invite(fight, 1, "192.168.0.2", 8765).code());
        assertTrue(hub.approve(fight, 1));
        assertFalse(hub.connectedSeats(fight).contains(1));
    }

    @AfterEach
    void tearDown() throws Exception {
        FxTest.onFx(() -> {
            scenes.forEach(s -> s.setRoot(new StackPane()));
            return null;
        });
        hub.close();
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

    private Node open() throws Exception {
        var screen = new SetupScreen(fights, hub, 8765, () -> { }, LiveRefresh.DIRECT);
        return FxTest.onFx(() -> {
            var node = screen.build();
            scenes.add(new Scene(new StackPane(node), 1280, 800));
            return node;
        });
    }

    private static boolean waitFor(Node root, Predicate<List<String>> ok) throws Exception {
        long deadline = System.nanoTime() + 3_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (FxTest.onFx(() -> ok.test(texts(root)))) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }

    /** Se llama dentro de {@code FxTest.onFx}. */
    private static Button button(Node root, String text) {
        return all(root).stream().filter(n -> n instanceof Button b && text.equals(b.getText()))
                .map(Button.class::cast).findFirst().orElseThrow(() -> new AssertionError(text + ": " + texts(root)));
    }

    private static long codes(List<String> shown) {
        return shown.stream().filter(t -> t.matches("\\d{3} \\d{3}")).count();
    }

    @Test
    void unJuezAprobadoSinConexionTieneNuevoCodigoQueSeEmiteAlPulsar() throws Exception {
        var root = open();
        assertTrue(waitFor(root, t -> t.contains("Nuevo código")), FxTest.onFx(() -> texts(root)).toString());
        assertEquals(0, codes(FxTest.onFx(() -> texts(root))), "sin pulsar no hay código vivo en pantalla");

        FxTest.onFx(() -> {
            button(root, "Nuevo código").fire();
            return null;
        });
        assertTrue(waitFor(root, t -> codes(t) == 1), FxTest.onFx(() -> texts(root)).toString());
    }

    @Test
    void otroTelefonoConElCodigoNuevoMuestraCambiarTelefonoConLasDosHuellas() throws Exception {
        var before = FightService.shortFingerprint(
                new LugarJuezRepository(db).find(fight, 1).orElseThrow().huellaCredencial());
        join(keys(), "B-2", fights.reinvite(fight, 1, "192.168.0.2", 8765).code());
        var after = hub.replacementFingerprint(fight, 1).orElseThrow();

        var root = open();
        assertTrue(waitFor(root, t -> t.contains("Cambiar teléfono")), FxTest.onFx(() -> texts(root)).toString());
        var shown = FxTest.onFx(() -> texts(root));
        assertTrue(shown.contains(before) && shown.contains(after), "las dos huellas: " + shown);

        FxTest.onFx(() -> {
            button(root, "Cambiar teléfono").fire();
            return null;
        });
        assertTrue(hub.replacementFingerprint(fight, 1).isEmpty());
        assertEquals(after, FightService.shortFingerprint(
                new LugarJuezRepository(db).find(fight, 1).orElseThrow().huellaCredencial()));
    }
}
