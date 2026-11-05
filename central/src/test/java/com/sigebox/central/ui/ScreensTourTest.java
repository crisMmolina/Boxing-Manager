package com.sigebox.central.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.export.ExportService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.service.TerminationService;
import com.sigebox.central.ui.funcion.FuncionFlow;
import com.sigebox.central.ui.funcion.FuncionQuery;
import com.sigebox.central.ui.funcion.FuncionScreen;
import com.sigebox.central.ui.inicio.InicioQuery;
import com.sigebox.central.ui.inicio.InicioScreen;
import com.sigebox.central.ui.inicio.Nav;
import com.sigebox.central.ui.log.LogQuery;
import com.sigebox.central.ui.log.LogScreen;
import com.sigebox.central.ui.result.ResultStepScreen;
import com.sigebox.central.ui.rounds.RoundsScreen;
import com.sigebox.central.ui.setup.SetupScreen;
import com.sigebox.central.ui.termination.TerminationScreen;
import com.sigebox.central.ui.terms.TermsGate;
import com.sigebox.central.ui.terms.TermsScreen;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.central.ui.theme.Ui;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.LugarJuez;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Recorre todas las pantallas registradas como en {@code App}, en cada momento de un evento de dos
 * peleas con judge-sim, y anota lo que incumple spec 003 §Sin capacitación y spec 004 §Formularios.
 * Cada prueba afirma un criterio de DSK-027 sobre ese único recorrido.
 */
class ScreensTourTest {

    private static final Pattern FUNCION = Pattern.compile("(?i)funci[oó]n");
    private static final Pattern CODE = Pattern.compile("\\b[A-ZÁÉÍÓÚÑ][A-ZÁÉÍÓÚÑ_]{3,}\\b");
    private static final Pattern QR_URI = Pattern.compile("sigebox://join\\?h=[^&]+&p=\\d+&c=(\\d{6})");

    private static final Set<String> unlabeled = new TreeSet<>();
    private static final Set<String> noExample = new TreeSet<>();
    private static final Set<String> noNext = new TreeSet<>();
    private static final Set<String> noWhy = new TreeSet<>();
    private static final Set<String> codes = new TreeSet<>();
    private static final Set<String> funcion = new TreeSet<>();
    private static final Set<String> visited = new TreeSet<>();
    private static final Set<String> allowed = new TreeSet<>(Set.of("SIGEBOX"));

    private static final List<String> decodedQrs = new ArrayList<>();
    private static final List<String> shownCodes = new ArrayList<>();
    private static final Set<String> helpOn = new TreeSet<>();

    private static Path dir;
    private static JudgeSimRig rig;
    private static Screens<Node> screens;

    @BeforeAll
    static void tour() throws Exception {
        FxTest.start();
        dir = Files.createTempDirectory("tour");
        rig = new JudgeSimRig(dir.resolve("tour.db"));
        var flow = new FuncionFlow(rig.funciones, new FuncionQuery(rig.db));
        register(flow);

        visit("sin datos");

        rig.funciones.crear(2, 1, List.of(new Esquinas("Ana", "Bea"), new Esquinas("Carla", "Dora")));
        visit("evento creado");

        var first = flow.start().id();
        allowed.add(first);
        flow.funcionId().ifPresent(allowed::add);
        visit("pelea sin jueces");

        rig.join(first, 1, 2);
        JudgeSimRig.await(() -> rig.hub.connectedSeats(first).contains(1), 10_000, "juez conectado");
        visit("juez conectado");

        rig.rounds.openRound(first, 1);
        visit("round abierto");

        JudgeSimRig.await(() -> rig.rounds.board(first, 1).missing().isEmpty(), 10_000, "tarjeta del round 1");
        rig.rounds.closeRound(first, 1, null);
        visit("round cerrado");

        rig.rounds.openRound(first, 2);
        JudgeSimRig.await(() -> rig.rounds.board(first, 2).missing().isEmpty(), 10_000, "tarjeta del round 2");
        rig.rounds.closeRound(first, 2, null);
        visit("rounds completos");

        rig.results.endFight(first);
        visit("pelea terminada");

        JudgeSimRig.await(() -> rig.results.canClose(first), 10_000, "tarjeta final");
        rig.results.close(first);
        visit("pelea cerrada");

        var second = flow.start().id();
        allowed.add(second);
        new LugarJuezRepository(rig.db).save(new LugarJuez(second, 1, "Ana", "A", "fp",
                EstadoConexion.DESCONECTADO, "central", 1L));
        visit("juez desconectado");
    }

    @AfterAll
    static void close() throws Exception {
        rig.close();
    }

    private static void register(FuncionFlow flow) {
        var hub = rig.hub;
        var db = rig.db;
        java.util.function.Supplier<String> fightId =
                () -> new PeleaRepository(db).findActive().map(p -> p.id()).orElse(null);
        screens = new Screens<>();
        TermsScreen.register(screens, new TermsGate(db), () -> { });
        InicioScreen.register(screens, new InicioQuery(db, rig.funciones), rig.funciones, id -> { });
        var setup = new SetupScreen(rig.fights, hub, rig.server.port(), () -> { });
        setup.register(screens, SetupScreen.ID, "Pelea");
        setup.register(screens, Nav.JUECES, "Jueces");
        RoundsScreen.register(screens, rig.rounds, fightId, () -> { });
        FuncionScreen.register(screens, flow, () -> { });
        ResultStepScreen.register(screens, rig.results, new ExportService(), fightId, flow::peek, () -> { });
        TerminationScreen.register(screens, TerminationService.forHub(db, hub), fightId);
        LogScreen.register(screens, new LogQuery(db));
    }

    private static void visit(String moment) throws Exception {
        for (var screen : screens.all()) {
            var where = screen.title() + " / " + moment;
            var root = new StackPane();
            FxTest.onFx(() -> {
                root.getChildren().setAll(screen.factory().get());
                new Scene(root, 1280, 800);
                return null;
            });
            settle();
            FxTest.onFx(() -> {
                root.applyCss();
                root.layout();
                inspect(screen.id(), where, root);
                root.getScene().setRoot(new StackPane()); // para sus refrescos
                return null;
            });
            visited.add(screen.title());
        }
    }

    /** Deja que la lectura de fondo termine y que su dibujo pase por el hilo de JavaFX. */
    private static void settle() throws Exception {
        for (int i = 0; i < 2; i++) {
            var done = new CountDownLatch(1);
            LiveRefresh.BACKGROUND.execute(done::countDown);
            assertTrue(done.await(10, TimeUnit.SECONDS));
            FxTest.onFx(() -> null);
        }
    }

    private static void inspect(String id, String where, Parent root) throws Exception {
        var nodes = new ArrayList<Node>();
        walk(root, nodes);
        Set<Node> labelled = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var n : nodes) {
            if (n instanceof Label l && l.getLabelFor() != null) {
                labelled.add(l.getLabelFor());
            }
        }
        boolean next = false;
        for (var n : nodes) {
            if (isField(n) && !labelled.contains(n)) {
                unlabeled.add(where + ": " + describe(n));
            }
            if (isField(n) && n instanceof TextInputControl t && t.isEditable() && blank(t.getPromptText())) {
                noExample.add(where + ": " + describe(n));
            }
            if (n instanceof Label l && l.getStyleClass().contains("next-step") && l.getText().startsWith("Qué sigue: ")
                    && l.getText().length() > "Qué sigue: ".length()) {
                next = true;
            }
            if (n instanceof ButtonBase b && b.isDisable() && !saysWhy(b)) {
                noWhy.add(where + ": " + b.getText());
            }
            for (var text : texts(n)) {
                check(where, text);
            }
            if (n instanceof ComboBox<?> c) {
                checkEnumItems(where, c);
            }
            if (n instanceof ButtonBase b && b.getText() != null && b.getText().contains("Cómo reconectar")) {
                helpOn.add(id);
            }
            if (n instanceof ImageView v && id.equals(Nav.JUECES) && v.getImage() != null) {
                decodedQrs.add(decode(v.getImage()));
            }
            if (n instanceof Labeled l && l.getAccessibleText() != null && l.getAccessibleText().startsWith("Código ")
                    && id.equals(Nav.JUECES)) {
                shownCodes.add(l.getText().replace(" ", ""));
            }
        }
        if (!next) {
            noNext.add(where);
        }
    }

    private static void walk(Node node, List<Node> out) {
        if (!node.isVisible()) {
            return;
        }
        out.add(node);
        if (node instanceof Parent p) {
            for (var child : p.getChildrenUnmodifiable()) {
                walk(child, out);
            }
        }
    }

    /** Un control donde el organizador escribe o elige; el editor interno de otro control no cuenta. */
    private static boolean isField(Node n) {
        if (n instanceof Spinner<?> || n instanceof ComboBoxBase<?> || n instanceof ChoiceBox<?>) {
            return true;
        }
        if (!(n instanceof TextInputControl)) {
            return false;
        }
        for (var p = n.getParent(); p != null; p = p.getParent()) {
            if (p instanceof Spinner<?> || p instanceof ComboBoxBase<?>) {
                return false;
            }
        }
        return true;
    }

    private static boolean saysWhy(ButtonBase b) {
        return b.getProperties().get(Ui.WHY) instanceof Label why && why.isVisible() && why.getScene() != null
                && !blank(why.getText());
    }

    private static List<String> texts(Node n) {
        var out = new ArrayList<String>();
        if (n instanceof Text t) {
            out.add(t.getText());
        }
        if (n instanceof Labeled l) {
            out.add(l.getText());
        }
        if (n instanceof TextInputControl t) {
            out.add(t.getPromptText());
            out.add(t.getText());
        }
        if (n instanceof Control c && c.getTooltip() != null) {
            out.add(c.getTooltip().getText());
        }
        out.add(n.getAccessibleText());
        return out;
    }

    private static void check(String where, String text) {
        if (text == null) {
            return;
        }
        if (FUNCION.matcher(text).find()) {
            funcion.add(where + ": " + text);
        }
        var m = CODE.matcher(text);
        while (m.find()) {
            if (!allowed.contains(m.group())) {
                codes.add(where + ": " + text);
            }
        }
    }

    /** Lo que muestra el combo para cada valor de enum: ni en la lista ni en el botón sale el código. */
    private static <T> void checkEnumItems(String where, ComboBox<T> combo) {
        var before = combo.getValue();
        for (var item : combo.getItems()) {
            if (!(item instanceof Enum<?> e)) {
                continue;
            }
            if (combo.getCellFactory() == null) {
                codes.add(where + ": lista sin etiquetas, " + e.name());
            }
            combo.setValue(item);
            combo.applyCss();
            combo.layout();
            if (combo.lookup(".list-cell") instanceof ListCell<?> cell && e.name().equals(cell.getText())) {
                codes.add(where + ": " + e.name());
            }
        }
        combo.setValue(before);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String describe(Node n) {
        return n.getClass().getSimpleName() + (n instanceof TextInputControl t ? " '" + t.getPromptText() + "'" : "");
    }

    private static String decode(Image image) throws Exception {
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        var img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        var pixels = image.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, pixels.getArgb(x, y));
            }
        }
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)));
        return new QRCodeReader().decode(bitmap).getText();
    }

    @Test
    void theTourReachesEveryRegisteredScreen() {
        assertEquals(screens.all().stream().map(s -> s.title()).collect(java.util.stream.Collectors.toCollection(TreeSet::new)),
                visited);
    }

    @Test
    void everyFieldHasItsLabelAbove() {
        assertEquals(Set.of(), unlabeled);
    }

    @Test
    void everyTextFieldShowsAnExample() {
        assertEquals(Set.of(), noExample);
    }

    @Test
    void everyScreenSaysWhatComesNext() {
        assertEquals(Set.of(), noNext);
    }

    @Test
    void everyDisabledButtonSaysWhy() {
        assertEquals(Set.of(), noWhy);
    }

    @Test
    void noUppercaseCodeIsVisible() {
        assertEquals(Set.of(), codes);
    }

    @Test
    void theWordFuncionIsNeverShown() {
        assertEquals(Set.of(), funcion);
    }

    @Test
    void aDisconnectedJudgeHasConnectionHelpInJuecesAndEnVivo() {
        assertTrue(helpOn.contains(Nav.JUECES), "Jueces: " + helpOn);
        assertTrue(helpOn.contains(RoundsScreen.ID), "En vivo: " + helpOn);
    }

    @Test
    void theJudgesQrDecodesToTheNewUriWithTheCodeOnScreen() {
        assertFalse(decodedQrs.isEmpty());
        for (var uri : decodedQrs) {
            var m = QR_URI.matcher(uri);
            assertTrue(m.matches(), uri);
            assertTrue(shownCodes.contains(m.group(1)), uri + " no está en " + shownCodes);
        }
    }
}
