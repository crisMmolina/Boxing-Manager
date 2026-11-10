package com.sigebox.central.ui.funcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.service.FuncionService.Fila;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.theme.ActionMenu;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.central.ui.theme.PageHeader;
import com.sigebox.central.ui.theme.Stepper;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Spec 006 · DSK-043: Evento v5. Encabezado con la acción principal, base con steppers (ningún {@code Spinner}) y
 * cartelera en lista con menú ⋯. Reemplaza a {@code FuncionAddRowTest.laBaseDelEventoTieneRoundsYJuecesQueSeCambian} y a
 * {@code FuncionPreparadaRowTest.bajarLosJuecesDelEventoBajaLaPeleaEnJueces} (decisión DSK-043).
 */
class FuncionV5Test {

    @TempDir
    Path dir;

    private Database db;
    private FuncionService funciones;
    private final List<Scene> scenes = new ArrayList<>();

    @BeforeAll
    static void fx() throws Exception {
        FxTest.start();
    }

    @BeforeEach
    void setUp() {
        db = Database.open(dir.resolve("sigebox.db"));
        funciones = new FuncionService(db, new FightService(db, new SeatTokens(), () -> LocalDate.of(2026, 11, 9)));
    }

    @AfterEach
    void tearDown() throws Exception {
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

    private String evento() {
        return funciones.crear(10, 3, List.of(new Esquinas("Ana", "Bea"), new Esquinas("Caro", "Dani"),
                new Esquinas("Eli", "Fer"))).id();
    }

    private Node open() throws Exception {
        var screens = new Screens<Node>();
        FuncionScreen.register(screens, new FuncionFlow(funciones, new FuncionQuery(db)), () -> { });
        return FxTest.onFx(() -> {
            var node = screens.show(FuncionScreen.ID);
            scenes.add(new Scene(new StackPane(node), 1280, 800));
            return node;
        });
    }

    private static Optional<Stepper> stepper(Node root, String name) {
        return all(root).stream().filter(n -> n instanceof Stepper s && s.getAccessibleText().startsWith(name + ":"))
                .map(Stepper.class::cast).findFirst();
    }

    private static Optional<ActionMenu> menu(Node root, int orden) {
        return all(root).stream().filter(n -> n instanceof ActionMenu m
                && ("Más acciones de la pelea " + orden).equals(m.getAccessibleText()))
                .map(ActionMenu.class::cast).findFirst();
    }

    private static void fire(ActionMenu menu, String text) {
        menu.getItems().stream().filter(i -> text.equals(i.getText())).findFirst().orElseThrow().fire();
    }

    @Test
    void ningunSpinnerYLaBaseSeCambiaConSteppers() throws Exception {
        var id = evento();
        var root = open();

        assertTrue(FxTest.onFx(() -> all(root).stream().noneMatch(n -> n instanceof Spinner<?>)), "ningún Spinner");
        var judges = FxTest.onFx(() -> stepper(root, "Jueces del evento").orElseThrow());
        FxTest.onFx(() -> {
            judges.decrement();
            return null;
        });

        assertEquals(2, funciones.funcion(id).baseJueces());
        assertEquals(List.of(2, 2, 2), funciones.cartelera(id).stream().map(Fila::jueces).toList());
    }

    @Test
    void bajarLosJuecesDelEventoBajaLaPeleaQueEsperaEnJueces() throws Exception {
        var id = evento();
        var pelea = funciones.siguiente(id);
        var root = open();
        var judges = FxTest.onFx(() -> stepper(root, "Jueces del evento").orElseThrow());

        FxTest.onFx(() -> {
            judges.decrement();
            return null;
        });

        assertEquals(2, new PeleaRepository(db).find(pelea.id()).orElseThrow().jueces());
    }

    @Test
    void cadaFilaTieneSuMenuConTodasLasAccionesYNingunBotonSuelto() throws Exception {
        evento();
        var root = open();

        var items = FxTest.onFx(() -> menu(root, 2).orElseThrow().getItems().stream()
                .map(MenuItem::getText).filter(t -> t != null).toList());
        assertEquals(List.of("Cambiar rounds y jueces", "Subir", "Bajar", "Saltar pelea", "Quitar de la cartelera"),
                items);
        assertTrue(FxTest.onFx(() -> all(root).stream().noneMatch(n -> n instanceof Button b
                && List.of("Subir", "Bajar", "Saltar", "Quitar").contains(b.getText()))), "sin botones sueltos");
    }

    @Test
    void elMenuMueveYQuitaPeleas() throws Exception {
        var id = evento();
        var root = open();

        FxTest.onFx(() -> {
            fire(menu(root, 3).orElseThrow(), "Subir");
            return null;
        });
        assertEquals(List.of("Ana", "Eli", "Caro"), funciones.cartelera(id).stream().map(Fila::roja).toList());

        var again = open();
        FxTest.onFx(() -> {
            fire(menu(again, 1).orElseThrow(), "Quitar de la cartelera");
            return null;
        });
        assertEquals(List.of("Eli", "Caro"), funciones.cartelera(id).stream().map(Fila::roja).toList());
    }

    @Test
    void elEncabezadoLlevaElEventoYLaSiguientePeleaComoAccionPrincipal() throws Exception {
        evento();
        var root = open();

        var header = FxTest.onFx(() -> all(root).stream().filter(n -> n instanceof PageHeader)
                .map(PageHeader.class::cast).findFirst().orElseThrow());
        assertEquals("Evento", header.titleText());
        assertTrue(header.contextText().contains("2026-11-09"), header.contextText());
        var action = (Button) header.actionNode();
        assertEquals("Siguiente pelea: Ana vs Bea", action.getText());
        assertTrue(action.getStyleClass().contains("primary"));
    }
}
