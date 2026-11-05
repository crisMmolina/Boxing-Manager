package com.sigebox.central.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.export.ExportService;
import com.sigebox.central.server.JudgeHub;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.server.ServerEvents;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.ResultServiceTest;
import com.sigebox.central.service.RoundService;
import com.sigebox.central.service.TerminationService;
import com.sigebox.central.ui.funcion.FuncionFlow;
import com.sigebox.central.ui.funcion.FuncionQuery;
import com.sigebox.central.ui.funcion.FuncionScreen;
import com.sigebox.central.ui.inicio.CarteleraScreen;
import com.sigebox.central.ui.inicio.InicioQuery;
import com.sigebox.central.ui.inicio.InicioScreen;
import com.sigebox.central.ui.log.LogQuery;
import com.sigebox.central.ui.log.LogScreen;
import com.sigebox.central.ui.result.ResultStepScreen;
import com.sigebox.central.ui.rounds.RoundsScreen;
import com.sigebox.central.ui.setup.SetupScreen;
import com.sigebox.central.ui.termination.TerminationScreen;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.central.ui.theme.Skeleton;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.Pelea;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Cada sección muestra sola lo que cambia fuera de ella, sin botón "Actualizar" y con skeleton mientras carga. */
class LiveRefreshTest {

    /** El segundo del temporizador más lo que tarda la lectura en llegar a la pantalla. */
    private static final long WITHIN_NANOS = 1_500_000_000L;

    private static final String NO_FIGHT = "No hay pelea en curso.";

    @TempDir
    Path dir;

    private Database db;
    private FightService fights;
    private FuncionService funciones;
    private JudgeHub hub;
    private final Screens<Node> screens = new Screens<>();
    private final List<Scene> scenes = new ArrayList<>();

    @BeforeAll
    static void fx() throws Exception {
        FxTest.start();
    }

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        var tokens = new SeatTokens();
        fights = new FightService(db, tokens, () -> LocalDate.of(2026, 11, 3));
        funciones = new FuncionService(db, fights);
        hub = new JudgeHub(db, tokens, ServerEvents.NONE);
    }

    @AfterEach
    void tearDown() throws Exception {
        FxTest.onFx(() -> {
            scenes.forEach(LiveRefreshTest::detach);
            return null;
        });
        hub.close();
        db.close();
    }

    private static void detach(Scene scene) {
        var old = (Group) scene.getRoot();
        scene.setRoot(new Group());
        old.getChildren().clear();
    }

    private Node open(String id) throws Exception {
        return attach(FxTest.onFx(() -> screens.show(id)));
    }

    private Node attach(Node root) throws Exception {
        scenes.add(FxTest.onFx(() -> new Scene(new Group((Parent) root))));
        return root;
    }

    private static List<Node> all(Node node) {
        var out = new ArrayList<Node>();
        out.add(node);
        if (node instanceof Labeled l && l.getGraphic() != null) {
            out.addAll(all(l.getGraphic()));
        }
        if (node instanceof Parent p) {
            p.getChildrenUnmodifiable().forEach(c -> out.addAll(all(c)));
        }
        return out;
    }

    private static List<String> texts(Node root) {
        var out = new ArrayList<String>();
        for (var n : all(root)) {
            if (n instanceof Labeled l && l.getText() != null) {
                out.add(l.getText());
            } else if (n instanceof TextInputControl t && t.getText() != null) {
                out.add(t.getText());
            }
        }
        return out;
    }

    private static boolean shows(Node root, String text) throws Exception {
        return FxTest.onFx(() -> texts(root).stream().anyMatch(t -> t.contains(text)));
    }

    private static boolean loading(Node root) throws Exception {
        return FxTest.onFx(() -> all(root).stream().anyMatch(Skeleton.class::isInstance));
    }

    private interface Check {
        boolean holds() throws Exception;
    }

    private static boolean soon(Check check) throws Exception {
        long deadline = System.nanoTime() + WITHIN_NANOS;
        while (System.nanoTime() < deadline) {
            if (check.holds()) {
                return true;
            }
            Thread.sleep(20);
        }
        return check.holds();
    }

    private static void assertAppears(Node root, String text) throws Exception {
        assertTrue(soon(() -> shows(root, text)), () -> text + " no apareció: " + textsOf(root));
    }

    private static String textsOf(Node root) {
        try {
            return FxTest.onFx(() -> texts(root)).toString();
        } catch (Exception e) {
            return e.toString();
        }
    }

    private static List<Esquinas> pelea() {
        return List.of(new Esquinas("Roja 1", "Azul 1"));
    }

    private String funcion() {
        return funciones.crear("Primero", LocalDate.of(2026, 10, 1), 4, 3, pelea()).id();
    }

    private static ResultService results(Database db) {
        return new ResultService(db, new ResultService.Broadcaster() {
            @Override
            public void fightEnded(String fightId, MotivoTerminacion reason) {
            }

            @Override
            public void fightClosed(String fightId) {
            }
        });
    }

    private static RoundService rounds(Database db) {
        return new RoundService(db, new RoundService.Broadcaster() {
            @Override
            public void roundOpened(String fightId, int round) {
            }

            @Override
            public void roundClosed(String fightId, int round, boolean forced) {
            }
        }, () -> 5_000L);
    }

    private static TerminationService termination(Database db) {
        return new TerminationService(db, (fight, reason) -> { }, () -> 5_000L);
    }

    // Criterio 1: cada pantalla muestra un cambio externo sin que nadie pulse nada.

    @Test
    void inicioShowsANewEvent() throws Exception {
        InicioScreen.register(screens, new InicioQuery(db, funciones), funciones, id -> { });
        var root = open(InicioScreen.ID);
        assertTrue(soon(() -> !loading(root)), () -> textsOf(root));

        funciones.crear("Velada nueva", LocalDate.of(2026, 11, 3), 4, 3, pelea());

        assertAppears(root, "Velada nueva");
    }

    @Test
    void eventoShowsANewFight() throws Exception {
        var id = funcion();
        FuncionScreen.register(screens, new FuncionFlow(funciones, new FuncionQuery(db)), () -> { });
        var root = open(FuncionScreen.ID);
        assertAppears(root, "Roja 1");

        funciones.agregar(id, new Esquinas("Roja nueva", "Azul nueva"));

        assertAppears(root, "Roja nueva");
    }

    @Test
    void juecesShowsAFightCreatedElsewhere() throws Exception {
        new SetupScreen(fights, hub, 8765, () -> { }).register(screens, "jueces", "Jueces");
        var root = open("jueces");
        assertAppears(root, "Crea la pelea");

        fights.create("Rojo", "Azul", 3, 1);

        assertAppears(root, "Juez 1");
    }

    @Test
    void enVivoShowsARoundOpenedElsewhere() throws Exception {
        var fight = "pelea-1";
        new PeleaRepository(db).save(new Pelea(fight, "Rojo", "Azul", 3, 1, EstadoPelea.EN_CURSO, "2026-11-04"));
        new LugarJuezRepository(db).save(new LugarJuez(fight, 1, "Ana", "A", "fp", EstadoConexion.CONECTADO,
                "central", 1L));
        var service = rounds(db);
        RoundsScreen.register(screens, service, () -> fight, () -> { });
        var root = open(RoundsScreen.ID);
        assertAppears(root, "Por abrir");

        service.openRound(fight, 1);

        assertAppears(root, "Round abierto");
    }

    @Test
    void resultadoShowsAFightThatEnded() throws Exception {
        var ref = ResultServiceTest.Reference.load();
        ref.into(db, EstadoPelea.EN_CURSO);
        var fight = ref.pelea().id();
        ResultStepScreen.register(screens, results(db), new ExportService(), () -> fight);
        var root = open(ResultStepScreen.ID);
        assertFalse(shows(root, "Suma"), () -> textsOf(root));

        new PeleaRepository(db).updateEstado(fight, EstadoPelea.TERMINADA);

        assertAppears(root, "Suma");
    }

    @Test
    void terminacionShowsAFightThatStarted() throws Exception {
        var fight = new AtomicReference<String>();
        TerminationScreen.register(screens, termination(db), fight::get);
        var root = open(TerminationScreen.ID);
        assertAppears(root, NO_FIGHT);

        fight.set("pelea-1");

        assertTrue(soon(() -> !shows(root, NO_FIGHT)), () -> textsOf(root));
    }

    @Test
    void bitacoraShowsANewEvent() throws Exception {
        LogScreen.register(screens, new LogQuery(db));
        var root = open(LogScreen.ID);
        assertAppears(root, "0 eventos");

        new EventLog(db).append("p-1", "SUBMIT_ROUND", "lugar 2 round 3 TARDIA");

        assertAppears(root, "1 eventos");
    }

    // Criterio 2: ninguna pantalla lleva botón "Actualizar".

    @Test
    void noScreenHasARefreshButton() throws Exception {
        var ref = ResultServiceTest.Reference.load();
        ref.into(db, EstadoPelea.EN_CURSO);
        var fight = ref.pelea().id();
        var id = funcion();
        var routes = InicioScreen.register(screens, new InicioQuery(db, funciones), funciones, x -> { });
        routes.open(id);
        FuncionScreen.register(screens, new FuncionFlow(funciones, new FuncionQuery(db)), () -> { });
        new SetupScreen(fights, hub, 8765, () -> { }).register(screens, "jueces", "Jueces");
        RoundsScreen.register(screens, rounds(db), () -> fight, () -> { });
        ResultStepScreen.register(screens, results(db), new ExportService(), () -> fight);
        TerminationScreen.register(screens, termination(db), () -> fight);
        LogScreen.register(screens, new LogQuery(db));

        assertTrue(screens.all().size() >= 8, () -> "pantallas: " + screens.all().size());
        for (var screen : screens.all()) {
            var root = open(screen.id());
            assertTrue(soon(() -> !loading(root)), () -> screen.id() + " sigue cargando");
            assertFalse(shows(root, "Actualizar"), () -> screen.id() + ": " + textsOf(root));
        }
    }

    // Criterio 3: mientras no llega la lectura, la sección muestra su skeleton.

    @Test
    void everySectionShowsItsSkeletonUntilTheDataArrives() throws Exception {
        var id = funcion();
        var held = new ArrayList<Runnable>();
        new SetupScreen(fights, hub, 8765, () -> { }, held::add).register(screens, "jueces", "Jueces");
        LogScreen.register(screens, new LogQuery(db), held::add);
        TerminationScreen.register(screens, termination(db), () -> null, held::add);
        var query = new InicioQuery(db, funciones);

        assertLoadsAfterSkeleton(FxTest.onFx(() -> new InicioScreen(query, x -> { }, () -> { }, held::add).build()),
                held, "Primero");
        assertLoadsAfterSkeleton(FxTest.onFx(() -> new CarteleraScreen(funciones, () -> id, held::add).build()),
                held, "Roja 1");
        assertLoadsAfterSkeleton(FxTest.onFx(() -> screens.show("jueces")), held, "Crea la pelea");
        assertLoadsAfterSkeleton(FxTest.onFx(() -> screens.show(LogScreen.ID)), held, "0 eventos");
        assertLoadsAfterSkeleton(FxTest.onFx(() -> screens.show(TerminationScreen.ID)), held, NO_FIGHT);
    }

    private static void assertLoadsAfterSkeleton(Node root, List<Runnable> held, String content) throws Exception {
        assertTrue(loading(root), () -> textsOf(root));
        assertFalse(shows(root, content), () -> textsOf(root));

        FxTest.onFx(() -> {
            var reads = List.copyOf(held);
            held.clear();
            reads.forEach(Runnable::run);
            return null;
        });

        assertFalse(loading(root), () -> textsOf(root));
        assertTrue(shows(root, content), () -> textsOf(root));
    }

    // El mecanismo.

    private static <T> LiveRefresh<T> watch(Node root, Supplier<T> read, List<T> drawn) throws Exception {
        return FxTest.onFx(() -> LiveRefresh.watch(root, LiveRefresh.DIRECT, read, drawn::add));
    }

    private static void flushFx() throws Exception {
        FxTest.onFx(() -> null);
    }

    @Test
    void aServerEventRefreshesRightAway() throws Exception {
        var value = new AtomicInteger(1);
        var drawn = new CopyOnWriteArrayList<Integer>();
        var root = new VBox();
        watch(root, value::get, drawn);
        attach(root);

        value.set(2);
        LiveRefresh.EVENTS.onJudgeConnected("p-1", 1); // desde este hilo, como un hilo de Jetty
        flushFx();

        assertEquals(List.of(1, 2), drawn);
    }

    @Test
    void drawsOnlyWhatChanged() throws Exception {
        var drawn = new CopyOnWriteArrayList<String>();
        var live = watch(new VBox(), () -> "igual", drawn);

        live.poke();
        live.poke();
        flushFx();

        assertEquals(List.of("igual"), drawn);
    }

    @Test
    void aFailedReadKeepsWhatIsShownAndTriesAgain() throws Exception {
        var fail = new AtomicBoolean();
        var drawn = new CopyOnWriteArrayList<String>();
        var live = watch(new VBox(), () -> {
            if (fail.get()) {
                throw new IllegalStateException("base ocupada");
            }
            return "dato " + drawn.size();
        }, drawn);

        fail.set(true);
        live.poke();
        flushFx();
        assertEquals(List.of("dato 0"), drawn);

        fail.set(false);
        live.poke();
        flushFx();
        assertEquals(List.of("dato 0", "dato 1"), drawn);
    }

    @Test
    void readsEverySecondOnlyWhileInTheScene() throws Exception {
        var reads = new AtomicInteger();
        var root = new VBox();
        watch(root, reads::incrementAndGet, new CopyOnWriteArrayList<>());
        attach(root);
        assertTrue(soon(() -> reads.get() >= 2), "el temporizador no leyó");

        FxTest.onFx(() -> {
            detach(scenes.getLast());
            return null;
        });
        int seen = reads.get();
        LiveRefresh.EVENTS.onCardReceived(null);
        Thread.sleep(1_300);
        flushFx();

        assertEquals(seen, reads.get());
    }
}
