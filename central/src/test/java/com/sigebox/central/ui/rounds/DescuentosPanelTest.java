package com.sigebox.central.ui.rounds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.server.ServerEvents;
import com.sigebox.central.service.DescuentoService;
import com.sigebox.central.service.RoundService;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.Pelea;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** La tarjeta de descuentos de En vivo: confirma, lista cada descuento y nunca suma (principio 3). */
class DescuentosPanelTest {

    private static final String F = "pelea-1";
    private static final Pattern SCORE = Pattern.compile("\\d+\\s*[–-]\\s*\\d+");
    private static final Pattern RESULT = Pattern.compile("(?i)\\bgana|\\btotal|ganador|resumen parcial");

    @TempDir
    Path dir;

    private Database db;
    private RoundService rounds;
    private DescuentoService service;

    /** Respuestas del operador falso. */
    private Optional<Integer> round = Optional.empty();
    private Optional<String> motivo = Optional.empty();
    private int defaultRound;
    private List<Integer> opciones;

    private final DescuentosPanel.Ask ask = new DescuentosPanel.Ask() {
        @Override
        public Optional<Integer> round(String pregunta, List<Integer> rounds, int actual) {
            defaultRound = actual;
            opciones = rounds;
            return round;
        }

        @Override
        public Optional<String> motivo(String pregunta) {
            return motivo;
        }
    };

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        pelea(EstadoPelea.EN_CURSO);
        rounds = new RoundService(db, new RoundService.Broadcaster() {
            @Override
            public void roundOpened(String fightId, int round) {
            }

            @Override
            public void roundClosed(String fightId, int round, boolean forced) {
            }
        }, () -> 5_000L);
        service = new DescuentoService(db, ServerEvents.NONE, () -> 5_000L);
        rounds.openRound(F, 1);
        rounds.closeRound(F, 1, RoundService.CONFIRM_WORD);
        rounds.openRound(F, 2);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private void pelea(EstadoPelea estado) {
        new PeleaRepository(db).save(new Pelea(F, "Rojo", "Azul", 3, 2, estado, "2026-11-04"));
    }

    private Node screen() throws Exception {
        return FxTest.onFx(() -> RoundsScreen.build(rounds, service, ask, () -> F, () -> {}));
    }

    private static Button button(Node root, String id) {
        return (Button) all(root).stream().filter(n -> id.equals(n.getId())).findFirst().orElseThrow();
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
        return all(root).stream().filter(Labeled.class::isInstance).map(n -> ((Labeled) n).getText())
                .filter(t -> t != null && !t.isEmpty()).toList();
    }

    private static Optional<Button> anular(Node root) {
        return all(root).stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> "Anular".equals(b.getText())).findFirst();
    }

    @Test
    void elBotonProponeElRoundActualYGuardaAlConfirmar() throws Exception {
        var root = screen();
        round = Optional.of(2);

        FxTest.onFx(() -> {
            button(root, DescuentosPanel.ROJA_ID).fire();
            return null;
        });

        assertEquals(2, defaultRound);
        assertEquals(List.of(1, 2, 3), opciones);
        var d = service.listar(F).get(0);
        assertEquals(2, d.round());
        assertEquals(Esquina.ROJA, d.esquina());
        assertTrue(FxTest.onFx(() -> texts(root)).contains("Round 2 · −1 roja"));
    }

    @Test
    void sinConfirmacionNoSeGuardaNada() throws Exception {
        var root = screen();

        FxTest.onFx(() -> {
            button(root, DescuentosPanel.AZUL_ID).fire();
            return null;
        });

        assertTrue(service.listar(F).isEmpty());
    }

    @Test
    void tresEnElMismoRoundSeListanPorSeparado() throws Exception {
        var root = screen();
        round = Optional.of(1);

        FxTest.onFx(() -> {
            for (int i = 0; i < 3; i++) {
                button(root, DescuentosPanel.AZUL_ID).fire();
            }
            return null;
        });

        var rows = FxTest.onFx(() -> texts(root)).stream().filter("Round 1 · −1 azul"::equals).toList();
        assertEquals(3, rows.size());
    }

    @Test
    void anularPideMotivoYElRenglonSigueVisible() throws Exception {
        service.poner(F, 2, Esquina.ROJA);
        var root = screen();
        motivo = Optional.of("error");

        FxTest.onFx(() -> {
            anular(root).orElseThrow().fire();
            return null;
        });

        assertTrue(service.listar(F).get(0).anulado());
        assertTrue(FxTest.onFx(() -> texts(root)).contains("Round 2 · −1 roja · anulado: error"));
        assertTrue(FxTest.onFx(() -> anular(root)).isEmpty());
    }

    @Test
    void cancelarElMotivoNoAnula() throws Exception {
        service.poner(F, 2, Esquina.ROJA);
        var root = screen();

        FxTest.onFx(() -> {
            anular(root).orElseThrow().fire();
            return null;
        });

        assertFalse(service.listar(F).get(0).anulado());
    }

    @Test
    void motivoVacioMuestraElErrorSinAnular() throws Exception {
        service.poner(F, 2, Esquina.ROJA);
        var root = screen();
        motivo = Optional.of("  ");

        FxTest.onFx(() -> {
            anular(root).orElseThrow().fire();
            return null;
        });

        assertFalse(service.listar(F).get(0).anulado());
        assertTrue(FxTest.onFx(() -> texts(root)).contains("anular exige un motivo"));
    }

    @Test
    void conLaPeleaTerminadaLosBotonesSeApagan() throws Exception {
        service.poner(F, 2, Esquina.ROJA);
        pelea(EstadoPelea.TERMINADA);

        var root = screen();

        assertTrue(FxTest.onFx(() -> button(root, DescuentosPanel.ROJA_ID).isDisable()));
        assertTrue(FxTest.onFx(() -> button(root, DescuentosPanel.AZUL_ID).isDisable()));
        assertTrue(FxTest.onFx(() -> anular(root)).isEmpty());
        assertTrue(FxTest.onFx(() -> texts(root)).contains("Round 2 · −1 roja"));
    }

    @Test
    void ningunTextoMuestraTotalNiGanador() throws Exception {
        service.poner(F, 1, Esquina.ROJA);
        service.poner(F, 1, Esquina.ROJA);
        long id = service.poner(F, 2, Esquina.AZUL);
        service.anular(id, "error");

        var root = screen();

        for (var t : FxTest.onFx(() -> texts(root))) {
            assertFalse(SCORE.matcher(t).find(), t);
            assertFalse(RESULT.matcher(t).find(), t);
        }
    }
}
