package com.sigebox.central.ui.rounds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.RoundService;
import com.sigebox.central.service.TerminationService;
import com.sigebox.central.ui.termination.TerminarEnVivo;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.Pelea;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Spec 005 §En vivo y terminación (DSK-036): "Abrir round" solo con todos los jueces conectados, y "Terminar pelea"
 * termina y cierra la pelea desde En vivo.
 */
class RoundsGateTest {

    private static final String F = "pelea-1";

    @TempDir
    Path dir;

    private Database db;
    private PeleaRepository peleas;
    private LugarJuezRepository lugares;
    private RoundService rounds;
    private final List<String> cerradas = new ArrayList<>();

    @BeforeAll
    static void fx() throws Exception {
        FxTest.start();
    }

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        peleas = new PeleaRepository(db);
        lugares = new LugarJuezRepository(db);
        peleas.save(new Pelea(F, "Rojo", "Azul", 3, 2, EstadoPelea.CREADA, "2026-11-09"));
        lugar(1, "Ana", EstadoConexion.CONECTADO);
        rounds = new RoundService(db, new RoundService.Broadcaster() {
            @Override
            public void roundOpened(String fightId, int round) {
            }

            @Override
            public void roundClosed(String fightId, int round, boolean forced) {
            }
        }, () -> 5_000L);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private void lugar(int seat, String name, EstadoConexion conexion) {
        lugares.save(new LugarJuez(F, seat, name, "D-" + seat, "fp", conexion, "central", 1L));
    }

    private Node build(Supplier<String> fightId, TerminarEnVivo terminar) throws Exception {
        return FxTest.onFx(() -> RoundsScreen.build(rounds, null, null, fightId, () -> { }, terminar));
    }

    private static Button button(Node root, String id) {
        return (Button) root.lookup("#" + id);
    }

    private static String why(Node root) throws Exception {
        return FxTest.onFx(() -> ((Label) root.lookup(".why")).getText());
    }

    @Test
    void conUnLugarSinJuezNoSeAbreElRoundYDiceCualFaltaYComoSeguir() throws Exception {
        var root = build(() -> F, null);
        var open = button(root, RoundsScreen.PRIMARY_ID);

        assertEquals("Abrir round 1", FxTest.onFx(open::getText));
        assertTrue(FxTest.onFx(open::isDisabled));
        assertTrue(why(root).contains("Juez 2") && why(root).contains("baja los jueces en Evento"), why(root));
    }

    @Test
    void conUnJuezAprobadoSinConexionDiceQueSeReconecte() throws Exception {
        lugar(2, "María", EstadoConexion.DESCONECTADO);
        var root = build(() -> F, null);

        assertTrue(FxTest.onFx(() -> button(root, RoundsScreen.PRIMARY_ID).isDisabled()));
        assertTrue(why(root).contains("Juez 2 · María") && why(root).contains("abra SIGEBOX"), why(root));
    }

    @Test
    void conTodosConectadosSeAbreYUnJuezQueSeCaeNoImpideCerrar() throws Exception {
        lugar(2, "María", EstadoConexion.CONECTADO);
        var root = build(() -> F, null);
        var primary = button(root, RoundsScreen.PRIMARY_ID);
        assertFalse(FxTest.onFx(primary::isDisabled));

        FxTest.onFx(() -> {
            primary.fire();
            return null;
        });
        lugar(2, "María", EstadoConexion.DESCONECTADO);
        var again = build(() -> F, null);

        assertEquals("Cerrar round 1", FxTest.onFx(() -> button(again, RoundsScreen.PRIMARY_ID).getText()));
        assertFalse(FxTest.onFx(() -> button(again, RoundsScreen.PRIMARY_ID).isDisabled()));
    }

    @Test
    void terminarPeleaPideSoloElMotivoCierraLaPeleaYLlevaAResultado() throws Exception {
        lugar(2, "María", EstadoConexion.CONECTADO);
        rounds.openRound(F, 1);
        var terminar = new TerminarEnVivo(new TerminationService(db, (f, m) -> { }, () -> 65_000L),
                new ResultService(db, new ResultService.Broadcaster() {
                    @Override
                    public void fightEnded(String fightId, MotivoTerminacion reason) {
                    }

                    @Override
                    public void fightClosed(String fightId) {
                    }
                }),
                (owner, punto) -> {
                    assertEquals(1, punto.round()); // la central pone el round, no se escribe
                    return Optional.of(new TerminarEnVivo.Eleccion(MotivoTerminacion.KO, null, Esquina.AZUL));
                }, cerradas::add);
        Supplier<String> active = () -> peleas.findActive().map(Pelea::id).orElse(null);
        var root = build(active, terminar);
        var end = button(root, RoundsScreen.END_ID);
        assertFalse(FxTest.onFx(end::isDisabled));

        FxTest.onFx(() -> {
            end.fire();
            return null;
        });

        assertEquals(EstadoPelea.CERRADA, peleas.find(F).orElseThrow().estado());
        assertEquals(List.of(F), cerradas);
        assertTrue(FxTest.onFx(end::isDisabled), "En vivo ya no tiene pelea que terminar");
    }
}
