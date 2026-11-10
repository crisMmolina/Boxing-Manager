package com.sigebox.central.ui.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.TarjetaFinalRepository;
import com.sigebox.central.export.ExportService;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.ResultServiceTest;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.central.ui.theme.PageHeader;
import com.sigebox.central.ui.theme.Theme;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.TarjetaFinal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Spec 006 · DSK-045: Resultado y su ventana con el encabezado v5; los totales entran contando (DESIGN v5 §7). */
class ResultV5Test {

    @TempDir
    Path dir;

    private Database db;
    private ResultServiceTest.Reference ref;
    private String fight;
    private ResultService service;

    @BeforeAll
    static void fx() throws Exception {
        FxTest.start();
    }

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
        Theme.setReducedMotion(null);
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

    private void closedFight() {
        ref.into(db, EstadoPelea.TERMINADA);
        for (int seat = 1; seat <= ref.pelea().jueces(); seat++) {
            new TarjetaFinalRepository(db).save(new TarjetaFinal(fight, seat, "ab".repeat(32), 1L), "firma");
        }
        service.close(fight);
    }

    @Test
    void elTotalCuentaDeCeroASuValor() throws Exception {
        Theme.setReducedMotion(false);
        var label = FxTest.onFx(() -> new Label("39"));
        var anim = FxTest.onFx(() -> CountUp.play(label, 39));

        assertEquals("0", FxTest.onFx(label::getText));
        assertEquals(400, anim.getTotalDuration().toMillis(), 1);
        FxTest.onFx(() -> {
            anim.jumpTo(anim.getTotalDuration());
            return null;
        });
        assertEquals("39", FxTest.onFx(label::getText));
        FxTest.onFx(() -> {
            anim.stop();
            return null;
        });
    }

    @Test
    void conMovimientoReducidoElTotalApareceDeUnaVez() throws Exception {
        Theme.setReducedMotion(true);
        var label = FxTest.onFx(() -> new Label("39"));

        var anim = FxTest.onFx(() -> CountUp.play(label, 39));

        assertNull(anim);
        assertEquals("39", label.getText());
    }

    @Test
    void laVentanaLlevaElEncabezadoConLaPeleaYSusTotalesCuentan() throws Exception {
        Theme.setReducedMotion(true);
        closedFight();
        var sheet = service.sheet(fight);

        var root = FxTest.onFx(() -> ResultScreen.content(new Stage(), sheet, new ExportService()));

        var header = FxTest.onFx(() -> all(root).stream().filter(n -> n instanceof PageHeader)
                .map(PageHeader.class::cast).findFirst().orElseThrow());
        assertEquals("Resultado", header.titleText());
        assertEquals(ref.pelea().esquinaRoja() + " vs " + ref.pelea().esquinaAzul(), header.contextText());
        var totals = FxTest.onFx(() -> all(root).stream()
                .filter(n -> n instanceof Label l && l.getStyleClass().contains("judge-total")).toList());
        assertFalse(totals.isEmpty(), "los totales por juez se marcan para contar");
    }

    @Test
    void laPantallaResultadoUsaElEncabezadoV5() throws Exception {
        ref.into(db, EstadoPelea.EN_CURSO);
        var root = FxTest.onFx(() -> ResultStepScreen.build(service, () -> fight, Optional::empty, () -> { },
                (node, s) -> { }));

        var header = FxTest.onFx(() -> all(root).stream().filter(n -> n instanceof PageHeader)
                .map(PageHeader.class::cast).findFirst().orElseThrow());
        assertEquals("Resultado", header.titleText());
    }
}
