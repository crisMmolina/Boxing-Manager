package com.sigebox.central.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.export.ExportService;
import com.sigebox.central.db.PeleaRepository;
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
import com.sigebox.central.ui.termination.TerminarEnVivo;
import com.sigebox.central.ui.terms.TermsGate;
import com.sigebox.central.ui.terms.TermsScreen;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.central.service.ResultService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Spec 005 §Una sola línea (DSK-039): recorre las pantallas registradas como en {@code App} y falla si un título no
 * coincide con su sección de la barra, si un botón está en mayúsculas o no empieza con mayúscula, si un "Qué sigue"
 * no es una instrucción que termina en punto, o si una pregunta no abre con "¿" (también en los diálogos, que se
 * revisan en el código porque no están en la escena).
 */
class ConsistencyTest {

    /** "Qué sigue: " + una instrucción que empieza con mayúscula y termina en punto. */
    private static final Pattern NEXT = Pattern.compile("Qué sigue: [A-ZÁÉÍÓÚÑ].*\\.");
    /** Literal de texto en el código. */
    private static final Pattern LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
    /** SQL con parámetros: no es texto de la pantalla. */
    private static final Pattern SQL = Pattern.compile("(=|LIMIT|IN \\() ?\\?");

    private static final Set<String> titles = new TreeSet<>();
    private static final Set<String> buttons = new TreeSet<>();
    private static final Set<String> nexts = new TreeSet<>();
    private static final Set<String> questions = new TreeSet<>();
    private static final Map<String, String> sections = new HashMap<>();

    private static Path dir;
    private static JudgeSimRig rig;
    private static Screens<Node> screens;
    private static FuncionFlow flow;

    @BeforeAll
    static void tour() throws Exception {
        FxTest.start();
        Stream.concat(Nav.STEPS.stream(), Nav.LINKS.stream()).forEach(i -> sections.put(i.id(), i.label()));
        dir = Files.createTempDirectory("consistencia");
        rig = new JudgeSimRig(dir.resolve("c.db"));
        flow = new FuncionFlow(rig.funciones, new FuncionQuery(rig.db));
        register();
        visit("sin datos");
        rig.funciones.crear(2, 1, List.of(new Esquinas("Ana", "Bea"), new Esquinas("Carla", "Dora")));
        visit("evento creado");
        flow.start();
        visit("pelea sin jueces");
    }

    @AfterAll
    static void close() throws Exception {
        rig.close();
    }

    private static void register() {
        var db = rig.db;
        Supplier<String> fightId = () -> new PeleaRepository(db).findActive().map(p -> p.id()).orElse(null);
        screens = new Screens<>();
        TermsScreen.register(screens, new TermsGate(db), () -> { });
        InicioScreen.register(screens, new InicioQuery(db, rig.funciones), rig.funciones, id -> { });
        var setup = new SetupScreen(rig.fights, rig.hub, rig.server.port(), () -> { });
        setup.alEvento(() -> { });
        setup.register(screens, Nav.JUECES, "Jueces");
        var results = ResultService.forHub(db, rig.hub);
        RoundsScreen.register(screens, rig.rounds, fightId, () -> { },
                new TerminarEnVivo(TerminationService.forHub(db, rig.hub), results, (o, p) -> Optional.empty(), id -> { }));
        FuncionScreen.register(screens, flow, () -> { });
        ResultStepScreen.register(screens, results, new ExportService(), fightId, flow::peek, () -> { });
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

    private static void inspect(String id, String where, Parent root) {
        var nodes = new ArrayList<Node>();
        walk(root, nodes);
        for (var n : nodes) {
            // Fuera del evento (Inicio) la barra no muestra secciones: el título no tiene con qué coincidir.
            if (n instanceof Label l && l.getStyleClass().contains("screen-title") && sections.containsKey(id)
                    && Nav.inEvent(id) && !sections.get(id).equals(l.getText())) {
                titles.add(where + ": \"" + l.getText() + "\" en la sección \"" + sections.get(id) + "\"");
            }
            if (n instanceof Label l && l.getStyleClass().contains("next-step") && !NEXT.matcher(l.getText()).matches()) {
                nexts.add(where + ": \"" + l.getText() + "\"");
            }
            if (n instanceof ButtonBase b && !badButton(b.getText()).isEmpty()) {
                buttons.add(where + ": \"" + b.getText() + "\" " + badButton(b.getText()));
            }
            if (n instanceof Labeled l && !question(l.getText())) {
                questions.add(where + ": \"" + l.getText() + "\"");
            }
        }
    }

    private static void walk(Node node, List<Node> out) {
        out.add(node);
        if (node instanceof Parent p) {
            p.getChildrenUnmodifiable().forEach(c -> walk(c, out));
        }
    }

    /** Vacío si el botón está bien: primera letra en mayúscula y no todo en mayúsculas. */
    static String badButton(String text) {
        if (text == null) {
            return "";
        }
        var letters = text.codePoints().filter(Character::isLetter).toArray();
        if (letters.length == 0) {
            return "";
        }
        if (!Character.isUpperCase(letters[0])) {
            return "(empieza con minúscula)";
        }
        if (letters.length > 1 && java.util.Arrays.stream(letters).allMatch(Character::isUpperCase)) {
            return "(todo en mayúsculas)";
        }
        return "";
    }

    /** Una pregunta abre con "¿"; el "?" de ayuda ("?" o "? Cómo reconectar") no es pregunta. */
    static boolean question(String text) {
        if (text == null || !text.contains("?") || text.equals("?") || text.startsWith("? ")) {
            return true;
        }
        return text.contains("¿");
    }

    @Test
    void cadaTituloEsElNombreDeSuSeccionEnLaBarra() {
        assertEquals(Set.of(), titles);
    }

    @Test
    void ningunBotonVaEnMayusculasNiEmpiezaConMinuscula() {
        assertEquals(Set.of(), buttons);
    }

    @Test
    void cadaQueSigueEsUnaInstruccionQueTerminaEnPunto() {
        assertEquals(Set.of(), nexts);
    }

    @Test
    void lasPreguntasDeLasPantallasAbrenConSignoDeApertura() {
        assertEquals(Set.of(), questions);
    }

    /** Los diálogos (Alert, TextInputDialog) no están en la escena: se revisan sus textos en el código de ui/. */
    @Test
    void lasPreguntasDeLosDialogosAbrenConSignoDeApertura() throws Exception {
        var bad = new TreeSet<String>();
        try (var files = Files.walk(Path.of("src/main/java/com/sigebox/central/ui"))) {
            for (var file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                var lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    var line = lines.get(i).strip();
                    if (line.startsWith("*") || line.startsWith("/") || line.startsWith("//")) {
                        continue;
                    }
                    var m = LITERAL.matcher(line);
                    while (m.find()) {
                        var text = m.group(1);
                        // SQL y expresiones regulares (con "\\") no son textos de la pantalla.
                        if (!SQL.matcher(text).find() && !text.contains("\\") && !question(text)) {
                            bad.add(file.getFileName() + ":" + (i + 1) + " \"" + text + "\"");
                        }
                    }
                }
            }
        }
        assertEquals(Set.of(), bad);
    }

    @Test
    void lasReglasReconocenLoQueEstaMal() {
        assertEquals("(todo en mayúsculas)", badButton("ABRIR ROUND"));
        assertEquals("(empieza con minúscula)", badButton("abrir round"));
        assertEquals("", badButton("Ir a Evento"));
        assertEquals("", badButton("?"));
        assertTrue(!question("Cerrar el round igual?"));
        assertTrue(question("¿Cerrar el round igual?"));
        assertTrue(question("? Cómo reconectar"));
        assertTrue(!NEXT.matcher("Qué sigue: Un momento, leyendo la pelea").matches());
    }

    @Test
    void sinPeleaJuecesOfreceIrAEvento() throws Exception {
        var empty = new JudgeSimRig(Files.createTempDirectory("sin-pelea").resolve("e.db"));
        try {
            var went = new ArrayList<String>();
            var setup = new SetupScreen(empty.fights, empty.hub, empty.server.port(), () -> { }, LiveRefresh.DIRECT);
            setup.alEvento(() -> went.add("evento"));
            var s = new Screens<Node>();
            setup.register(s, Nav.JUECES, "Jueces");
            FxTest.onFx(() -> {
                var root = new StackPane(s.all().get(0).factory().get());
                new Scene(root, 1280, 800);
                var nodes = new ArrayList<Node>();
                walk(root, nodes);
                nodes.stream().filter(n -> n instanceof ButtonBase b && "Ir a Evento".equals(b.getText()))
                        .map(ButtonBase.class::cast).findFirst().orElseThrow().fire();
                root.getScene().setRoot(new StackPane());
                return null;
            });
            assertEquals(List.of("evento"), went);
        } finally {
            empty.close();
        }
    }
}
