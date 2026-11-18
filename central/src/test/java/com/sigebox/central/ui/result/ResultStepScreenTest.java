package com.sigebox.central.ui.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.DescuentoRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.TarjetaFinalRepository;
import com.sigebox.central.export.ResultSheet;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.ResultServiceTest;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.TarjetaFinal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** El paso Resultado: puntos y descuentos sin ganador, se actualiza solo y cierra con las tarjetas finales. */
class ResultStepScreenTest {

    private static final Pattern WINNER = Pattern.compile("(?i)\\bgana|unánime|dividida|mayoría|\\bsin decisión");

    @TempDir
    Path dir;

    private Database db;
    private ResultServiceTest.Reference ref;
    private String fight;
    private ResultService service;
    private final List<ResultSheet> shown = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        db = Database.open(dir.resolve("sigebox.db"));
        ref = ResultServiceTest.Reference.load();
        fight = ref.pelea().id();
        service = new ResultService(db, new ResultService.Broadcaster() {
            @Override
            public void fightEnded(String fightId, MotivoTerminacion reason) {
            }

            @Override
            public void fightClosed(String fightId) {
            }
        });
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private Node screen() {
        return ResultStepScreen.build(service, () -> fight, Optional::empty, () -> { },
                (node, sheet) -> shown.add(sheet));
    }

    private void finalCards() {
        for (int seat = 1; seat <= ref.pelea().jueces(); seat++) {
            new TarjetaFinalRepository(db).save(new TarjetaFinal(fight, seat, "ab".repeat(32), 1L), "firma");
        }
    }

    private static void walk(Node node, List<Node> out) {
        out.add(node);
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> walk(child, out));
        }
    }

    private static List<String> texts(Node root) {
        var nodes = new ArrayList<Node>();
        walk(root, nodes);
        return nodes.stream().filter(Labeled.class::isInstance).map(n -> ((Labeled) n).getText()).toList();
    }

    private static Optional<Button> closeButton(Node root) {
        return Optional.ofNullable((Button) root.lookup("#cerrar-pelea"));
    }

    private static void assertNoWinner(List<String> texts) {
        texts.forEach(t -> assertFalse(WINNER.matcher(t).find(), t));
    }

    @Test
    void liveFightShowsNoPoints() throws Exception {
        ref.into(db, EstadoPelea.EN_CURSO);
        var texts = FxTest.onFx(() -> texts(screen()));

        assertTrue(texts.contains("Pelea en curso"), texts.toString());
        assertFalse(texts.contains("Suma"), texts.toString());
        assertFalse(texts.contains("39"), texts.toString());
        assertFalse(texts.contains("Cerrar pelea"), texts.toString());
    }

    @Test
    void showsRoundsDiscountsAndSumsPerJudgeWithoutWinner() throws Exception {
        ref.into(db, EstadoPelea.TERMINADA);
        new DescuentoRepository(db).save(fight, 2, Esquina.ROJA, 1L);

        var texts = FxTest.onFx(() -> texts(screen()));

        assertTrue(texts.containsAll(List.of("R1", "R4", "Descuentos", "Suma", "R2 Roja −1", "38", "36")),
                texts.toString());
        assertFalse(texts.contains("Terminar pelea"), texts.toString());
        assertFalse(texts.contains("Actualizar"), texts.toString());
        assertNoWinner(texts);
    }

    @Test
    void closeOpensWithTheFinalCardsAndStoresTheResult() throws Exception {
        ref.into(db, EstadoPelea.TERMINADA);
        var root = FxTest.onFx(this::screen);
        assertTrue(FxTest.onFx(() -> closeButton(root).orElseThrow().isDisable()));

        finalCards();
        var ready = FxTest.onFx(this::screen);
        assertFalse(FxTest.onFx(() -> closeButton(ready).orElseThrow().isDisable()));
        FxTest.onFx(() -> {
            closeButton(ready).orElseThrow().fire();
            return null;
        });

        assertTrue(service.stored(fight).isPresent());
        assertEquals(1, shown.size());
        var texts = FxTest.onFx(() -> texts(ready));
        assertTrue(texts.contains("Pelea cerrada"), texts.toString());
        assertTrue(FxTest.onFx(() -> closeButton(ready).isEmpty()));
        assertNoWinner(texts);
    }

    @Test
    void updatesByItselfWithinASecond() throws Exception {
        ref.into(db, EstadoPelea.EN_CURSO);
        var root = FxTest.onFx(this::screen);
        var scene = FxTest.onFx(() -> new Scene(new Group((Parent) root)));

        new PeleaRepository(db).updateEstado(fight, EstadoPelea.TERMINADA);
        assertTrue(appears(root, "Suma"));

        new DescuentoRepository(db).save(fight, 2, Esquina.ROJA, 1L);
        assertTrue(appears(root, "R2 Roja −1"));

        finalCards();
        assertTrue(appears(root, "Listo para cerrar"));

        FxTest.onFx(() -> {
            scene.setRoot(new Group());
            ((Group) root.getParent()).getChildren().clear();
            return null;
        });
    }

    private static boolean appears(Node root, String text) throws Exception {
        long deadline = System.nanoTime() + 1_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (FxTest.onFx(() -> texts(root)).contains(text)) {
                return true;
            }
            java.util.concurrent.locks.LockSupport.parkNanos(20_000_000L);
        }
        return false;
    }

    @Test
    void finalScreenHasDiscountedPointsAndNoWinner() throws Exception {
        ref.into(db, EstadoPelea.TERMINADA);
        new DescuentoRepository(db).save(fight, 2, Esquina.ROJA, 1L);
        finalCards();
        service.close(fight);

        var texts = FxTest.onFx(() -> texts(ResultScreen.body(service.sheet(fight))));

        assertTrue(texts.contains("R2 Roja −1"), texts.toString());
        assertTrue(texts.contains("38"), texts.toString());
        assertNoWinner(texts);
    }

    /** DSK-053: una pelea cerrada vuelve a abrir su resultado (Exportar PDF, CSV y Excel). */
    @Test
    void peleaCerradaMuestraVerResultadoQueAbreLaVentana() throws Exception {
        ref.into(db, EstadoPelea.TERMINADA);
        finalCards();
        service.close(fight);

        var root = FxTest.onFx(this::screen);
        var texts = FxTest.onFx(() -> texts(root));
        assertTrue(texts.contains("Ver resultado"), texts.toString());
        FxTest.onFx(() -> {
            ((Button) root.lookup("#ver-resultado")).fire();
            return null;
        });

        assertEquals(1, shown.size());
        assertEquals(fight, shown.get(0).pelea().id());
        assertNoWinner(texts);
    }
}
