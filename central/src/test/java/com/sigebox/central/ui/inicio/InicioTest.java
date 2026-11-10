package com.sigebox.central.ui.inicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.ui.Screens;
import com.sigebox.central.ui.funcion.FuncionScreen;
import com.sigebox.central.ui.result.ResultStepScreen;
import com.sigebox.central.ui.rounds.RoundsScreen;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.core.model.EstadoPelea;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Inicio: historial de eventos, "Nuevo evento" y a dónde lleva abrir uno (spec 004 §Arranque e inicio). */
class InicioTest {

    @TempDir
    Path dir;

    private Database db;
    private FuncionService funciones;

    @BeforeEach
    void open() {
        db = Database.open(dir.resolve("sigebox.db"));
        funciones = new FuncionService(db, new FightService(db, new SeatTokens(), () -> LocalDate.of(2026, 11, 3)));
    }

    @AfterEach
    void close() {
        db.close();
    }

    private static List<Esquinas> peleas(int n) {
        var out = new ArrayList<Esquinas>();
        for (int i = 0; i < n; i++) {
            out.add(new Esquinas("", ""));
        }
        return out;
    }

    private static List<Node> all(Node root) {
        var out = new ArrayList<Node>();
        out.add(root);
        if (root instanceof javafx.scene.control.Labeled l && l.getGraphic() != null) {
            out.addAll(all(l.getGraphic())); // sin escena el botón aún no tiene skin con su gráfico
        }
        if (root instanceof Parent p) {
            for (var c : p.getChildrenUnmodifiable()) {
                out.addAll(all(c));
            }
        }
        return out;
    }

    private static List<String> texts(Node root) {
        return all(root).stream().filter(n -> n instanceof Label).map(n -> ((Label) n).getText()).toList();
    }

    @Test
    void listsThreeEventsFromNewestToOldestWithTheirSummary() {
        // Se crean en un orden distinto al de sus fechas: manda el orden de creación, igual que el paso Evento.
        var a = funciones.crear("Noche de box Iztapalapa", LocalDate.of(2026, 11, 3), 10, 3, peleas(3));
        var b = funciones.crear("", LocalDate.of(2026, 10, 27), 4, 3, peleas(2));
        var c = funciones.crear("Velada de mayo", LocalDate.of(2026, 5, 1), 6, 3, peleas(1));
        var p = funciones.siguiente(b.id());
        new PeleaRepository(db).updateEstado(p.id(), EstadoPelea.CERRADA);

        var rows = new InicioQuery(db, funciones).eventos();
        assertEquals(List.of(c.id(), b.id(), a.id()), rows.stream().map(InicioQuery.Evento::id).toList());
        assertEquals("Velada de mayo", rows.get(0).nombre());
        assertEquals("Evento 27 oct 2026", rows.get(1).nombre());
        assertEquals("27 oct 2026", rows.get(1).fecha());
        assertEquals("2 peleas · 1 cerrada", rows.get(1).resumen());
        assertEquals("3 peleas · 0 cerradas", rows.get(2).resumen());
        assertTrue(rows.get(0).ultimo());
        assertFalse(rows.get(1).ultimo());
    }

    @Test
    void openingAnEventGoesToItsActiveFightOrItsCard() {
        var viejo = funciones.crear("Viejo", LocalDate.of(2026, 10, 1), 4, 3, peleas(1));
        var nuevo = funciones.crear("Nuevo", LocalDate.of(2026, 11, 3), 4, 3, peleas(2));
        var q = new InicioQuery(db, funciones);
        assertEquals(InicioQuery.Destino.EVENTO, q.destino(nuevo.id()), "el evento en curso abre su cartelera");
        assertEquals(InicioQuery.Destino.CARTELERA, q.destino(viejo.id()), "uno anterior se consulta solo");
        funciones.siguiente(nuevo.id());
        assertEquals(InicioQuery.Destino.EN_VIVO, q.destino(nuevo.id()), "con una pelea activa lleva a En vivo");
    }

    @Test
    void theBarHasTheFourStepsAndInicioWithoutRepeatedIds() {
        assertEquals(List.of("Evento", "Jueces", "En vivo", "Resultado"),
                Nav.STEPS.stream().map(i -> i.label()).toList());
        assertEquals(FuncionScreen.ID, Nav.STEPS.get(0).id());
        assertEquals(RoundsScreen.ID, Nav.STEPS.get(2).id());
        assertEquals(ResultStepScreen.ID, Nav.STEPS.get(3).id());
        assertEquals(InicioScreen.ID, Nav.LINKS.get(0).id());
        assertEquals("Inicio", Nav.LINKS.get(0).label());
        var ids = new HashSet<String>();
        Nav.STEPS.forEach(i -> assertTrue(ids.add(i.id()), i.id()));
        Nav.LINKS.forEach(i -> assertTrue(ids.add(i.id()), "id repetido en la barra: " + i.id()));
        assertTrue(Nav.LINKS.stream().noneMatch(i -> i.label().equals("Función")));
    }

    @Test
    void screenShowsTheEventsAndNuevoEventoAsThePrimaryAction() throws Exception {
        funciones.crear("Primero", LocalDate.of(2026, 10, 1), 4, 3, peleas(1));
        funciones.crear("Segundo", LocalDate.of(2026, 10, 2), 4, 3, peleas(1));
        funciones.crear("Tercero", LocalDate.of(2026, 10, 3), 4, 3, peleas(1));
        var opened = new ArrayList<String>();
        var screen = new InicioScreen(new InicioQuery(db, funciones), id -> opened.add(id), () -> opened.add("nuevo"));
        FxTest.onFx(() -> {
            var root = screen.build();
            var t = texts(root);
            assertTrue(t.contains("Eventos"));
            assertTrue(t.indexOf("Tercero") < t.indexOf("Segundo") && t.indexOf("Segundo") < t.indexOf("Primero"), t.toString());
            var primary = all(root).stream().filter(n -> n instanceof Button b && b.getStyleClass().contains("primary"))
                    .map(n -> (Button) n).toList();
            assertEquals(1, primary.size(), "una sola acción principal");
            assertEquals("Nuevo evento", primary.get(0).getText());
            var rows = all(root).stream().filter(n -> n.getStyleClass().contains("event-row")).toList();
            assertEquals(3, rows.size());
            assertTrue(rows.get(0).isFocusTraversable(), "las filas se recorren con teclado");
            ((Button) rows.get(0)).fire();
            primary.get(0).fire();
            return null;
        });
        assertEquals(2, opened.size());
        assertEquals("nuevo", opened.get(1));
    }

    @Test
    void emptyHistorySaysSoAndStillOffersNuevoEvento() throws Exception {
        var screen = new InicioScreen(new InicioQuery(db, funciones), id -> { }, () -> { });
        FxTest.onFx(() -> {
            var root = screen.build();
            assertTrue(texts(root).contains("Aún no hay eventos."));
            assertTrue(all(root).stream().anyMatch(n -> n instanceof Button b && "Nuevo evento".equals(b.getText())));
            return null;
        });
    }

    @Test
    void anOlderEventShowsItsCardReadOnly() throws Exception {
        var viejo = funciones.crear("Viejo", LocalDate.of(2026, 10, 1), 4, 3, List.of(new Esquinas("Ana", "Bea")));
        funciones.crear("Nuevo", LocalDate.of(2026, 11, 3), 4, 3, peleas(1));
        var view = new CarteleraScreen(funciones, () -> viejo.id());
        FxTest.onFx(() -> {
            var root = view.build();
            var t = texts(root);
            assertTrue(t.contains("Viejo"), t.toString());
            assertTrue(t.stream().anyMatch(s -> s.contains("Ana") && s.contains("Bea")), t.toString());
            assertTrue(all(root).stream().noneMatch(n -> n instanceof TextField), "solo consulta");
            return null;
        });
    }

    @Test
    void registersInicioAndRoutesEachEventToItsScreen() throws Exception {
        var viejo = funciones.crear("Viejo", LocalDate.of(2026, 10, 1), 4, 3, peleas(1));
        var nuevo = funciones.crear("Nuevo", LocalDate.of(2026, 11, 3), 4, 3, peleas(1));
        var screens = new Screens<Node>();
        var went = new ArrayList<String>();
        var routes = InicioScreen.register(screens, new InicioQuery(db, funciones), funciones, went::add);
        var ids = screens.all().stream().map(Screens.Screen::id).toList();
        assertEquals(List.of(InicioScreen.ID, NuevoEventoScreen.ID, CarteleraScreen.ID), ids);
        routes.open(nuevo.id());
        routes.open(viejo.id());
        funciones.siguiente(nuevo.id());
        routes.open(nuevo.id());
        routes.nuevo();
        assertEquals(List.of(FuncionScreen.ID, CarteleraScreen.ID, RoundsScreen.ID, NuevoEventoScreen.ID), went);
        assertEquals(viejo.id(), routes.selected());
        assertNotNull(screens.all().get(0).factory());
    }
}
