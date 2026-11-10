package com.sigebox.central.ui.rounds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.TarjetaRoundRepository;
import com.sigebox.central.service.RoundService;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.TarjetaRound;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** En vivo sin "Ver resumen parcial" ni Corrección: ningún nodo muestra un total ni un ganador (principio 3). */
class RoundsScreenTest {

    private static final String F = "pelea-1";

    /** "39–37", "10 - 9": un marcador; "Gana", "Total", "ganador": un resultado. */
    private static final Pattern SCORE = Pattern.compile("\\d+\\s*[–-]\\s*\\d+");
    private static final Pattern RESULT = Pattern.compile("(?i)\\bgana|\\btotal|ganador|resumen parcial");

    @TempDir
    Path dir;

    private Database db;
    private RoundService service;

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        new PeleaRepository(db).save(new Pelea(F, "Rojo", "Azul", 3, 2, EstadoPelea.EN_CURSO, "2026-11-04"));
        var seats = new LugarJuezRepository(db);
        seats.save(new LugarJuez(F, 1, "Ana", "A", "fp", EstadoConexion.CONECTADO, "central", 1L));
        seats.save(new LugarJuez(F, 2, "María López", "B", "fp", EstadoConexion.CONECTADO, "central", 1L));
        service = new RoundService(db, new RoundService.Broadcaster() {
            @Override
            public void roundOpened(String fightId, int round) {
            }

            @Override
            public void roundClosed(String fightId, int round, boolean forced) {
            }
        }, () -> 5_000L);
        // Round 1 cerrado con las dos tarjetas y round 2 abierto con una: hay puntos guardados que no deben verse.
        service.openRound(F, 1);
        card(1, 1, 10, 9);
        card(2, 1, 9, 10);
        service.closeRound(F, 1, null);
        service.openRound(F, 2);
        card(1, 2, 10, 8);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private void card(int seat, int round, int red, int blue) {
        new TarjetaRoundRepository(db).save(new TarjetaRound(F, seat, round, red, blue, EstadoTarjeta.ENVIADA,
                OrigenTarjeta.JUEZ, 1L));
    }

    private static List<Node> all(Node root) {
        var out = new ArrayList<Node>();
        out.add(root);
        if (root instanceof Labeled l && l.getGraphic() != null) {
            out.addAll(all(l.getGraphic()));
        }
        if (root instanceof Parent p) {
            for (var c : p.getChildrenUnmodifiable()) {
                out.addAll(all(c));
            }
        }
        return out;
    }

    private static List<String> texts(Node root) {
        return all(root).stream().filter(n -> n instanceof Labeled).map(n -> ((Labeled) n).getText())
                .filter(t -> t != null && !t.isBlank()).toList();
    }

    @Test
    void enVivoHasNoPartialSummaryNorCorrection() throws Exception {
        var t = FxTest.onFx(() -> texts(RoundsScreen.build(service, () -> F)));
        assertTrue(t.contains("Round 2 de 3"), t.toString());
        assertFalse(t.stream().anyMatch(s -> s.contains("resumen") || s.contains("Resumen")), t.toString());
        assertFalse(t.stream().anyMatch(s -> s.contains("Corregir") || s.contains("Capturar")), t.toString());
    }

    @Test
    void noNodeShowsATotalOrAWinner() throws Exception {
        var nodes = FxTest.onFx(() -> all(RoundsScreen.build(service, () -> F)));
        for (var n : nodes) {
            assertFalse(n.getStyleClass().contains("numeral"), "un número de esquina en En vivo: " + n);
            if (n instanceof Labeled l && l.getText() != null) {
                assertFalse(SCORE.matcher(l.getText()).find(), "marcador a la vista: " + l.getText());
                assertFalse(RESULT.matcher(l.getText()).find(), "resultado a la vista: " + l.getText());
            }
        }
    }

    @Test
    void thereIsOneActionCardWithTheRoundButtonOnly() throws Exception {
        var buttons = FxTest.onFx(() -> all(RoundsScreen.build(service, () -> F)).stream()
                .filter(n -> n instanceof javafx.scene.control.Button).map(n -> ((Labeled) n).getText()).toList());
        assertEquals(List.of("Cerrar round 2"), buttons);
    }

    private static List<Node> withClass(List<Node> nodes, String styleClass) {
        return nodes.stream().filter(n -> n.getStyleClass().contains(styleClass)).toList();
    }

    @Test
    void ringShowsCardsReceivedOfJudges() throws Exception {
        var rings = FxTest.onFx(() -> withClass(all(RoundsScreen.build(service, () -> F)), "cards-ring").stream()
                .map(RoundsScreenTest::texts).toList());
        assertEquals(1, rings.size(), "un anillo: " + rings);
        assertTrue(rings.getFirst().contains("1/2"), rings.toString());
        assertTrue(rings.getFirst().contains("tarjetas"), rings.toString());
    }

    @Test
    void oneSegmentPerRoundClosedCurrentFuture() throws Exception {
        var segs = FxTest.onFx(() -> withClass(all(RoundsScreen.build(service, () -> F)), "round-segment").stream()
                .map(n -> n.getStyleClass().contains("done") ? "done"
                        : n.getStyleClass().contains("current") ? "current"
                        : n.getStyleClass().contains("future") ? "future" : "?")
                .toList());
        assertEquals(List.of("done", "current", "future"), segs);
    }

    @Test
    void oneJudgeCardPerSeatWithInitialsAndRoundState() throws Exception {
        var cards = FxTest.onFx(() -> withClass(all(RoundsScreen.build(service, () -> F)), "judge-card").stream()
                .map(RoundsScreenTest::texts).toList());
        assertEquals(2, cards.size(), cards.toString());
        assertTrue(cards.get(0).containsAll(List.of("A", "Ana", "Juez 1", "Enviada")), cards.toString());
        assertTrue(cards.get(1).containsAll(List.of("ML", "María López", "Juez 2", "Calificando…")),
                cards.toString());
    }

    @Test
    void accionesNoDejaEspacioVacioBajoSusBotones() throws Exception {
        var blanks = FxTest.onFx(() -> {
            var nodes = all(RoundsScreen.build(service, () -> F));
            var card = nodes.stream().filter(n -> n.getStyleClass().contains("card") && n instanceof Parent p
                    && p.getChildrenUnmodifiable().stream().anyMatch(c -> c instanceof Labeled l
                            && "Acciones".equals(l.getText()))).map(n -> (Parent) n).findFirst().orElseThrow();
            return card.getChildrenUnmodifiable().stream().filter(c -> c.isManaged() && c instanceof Labeled l
                    && !(c instanceof javafx.scene.control.Button) && (l.getText() == null || l.getText().isBlank()))
                    .count();
        });
        assertEquals(0L, blanks, "un texto vacío en Acciones deja un hueco");
    }
}
