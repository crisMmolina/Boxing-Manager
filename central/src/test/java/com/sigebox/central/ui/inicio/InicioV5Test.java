package com.sigebox.central.ui.inicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.central.ui.theme.Stepper;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Spec 006 · DSK-043: Nuevo evento con steppers (ningún {@code Spinner}) y los mismos campos con etiqueta. Reemplaza a
 * {@code InicioTest.nuevoEventoFormCreatesTheEventWithLabeledFields} (decisión DSK-043).
 */
class InicioV5Test {

    @TempDir
    Path dir;

    private Database db;
    private FuncionService funciones;

    @BeforeAll
    static void fx() throws Exception {
        FxTest.start();
    }

    @BeforeEach
    void open() {
        db = Database.open(dir.resolve("sigebox.db"));
        funciones = new FuncionService(db, new FightService(db, new SeatTokens(), () -> LocalDate.of(2026, 11, 3)));
    }

    @AfterEach
    void close() {
        db.close();
    }

    private static List<Node> all(Node root) {
        var out = new ArrayList<Node>();
        out.add(root);
        if (root instanceof Parent p) {
            for (var c : p.getChildrenUnmodifiable()) {
                out.addAll(all(c));
            }
        }
        return out;
    }

    @Test
    void nuevoEventoCreaElEventoConCamposEtiquetadosYSteppers() throws Exception {
        var created = new ArrayList<String>();
        var form = new NuevoEventoScreen(funciones, created::add);
        FxTest.onFx(() -> {
            var root = form.build();
            assertTrue(all(root).stream().noneMatch(n -> n instanceof Spinner<?>), "ningún Spinner");
            var labels = all(root).stream().filter(n -> n instanceof Label l && l.getLabelFor() != null)
                    .map(n -> (Label) n).toList();
            assertEquals(List.of("Nombre (opcional)", "Fecha", "Rounds por pelea", "Jueces", "Número de peleas"),
                    labels.stream().map(Label::getText).toList());
            var name = (TextField) labels.get(0).getLabelFor();
            assertTrue(name.getPromptText() == null || name.getPromptText().isEmpty(), "sin texto de ejemplo");
            name.setText("Noche de box");
            ((DatePicker) labels.get(1).getLabelFor()).setValue(LocalDate.of(2026, 11, 3));
            var fights = (Stepper) labels.get(4).getLabelFor();
            fights.setValue(4);
            var crear = all(root).stream().filter(n -> n instanceof Button b && "Crear evento".equals(b.getText()))
                    .map(n -> (Button) n).findFirst().orElseThrow();
            assertTrue(crear.getStyleClass().contains("primary"));
            crear.fire();
            return null;
        });
        assertEquals(1, created.size());
        var f = funciones.funcion(created.get(0));
        assertEquals("Noche de box", f.nombre());
        assertEquals("2026-11-03", f.fecha());
        assertEquals(4, funciones.cartelera(f.id()).size());
    }

    @Test
    void nuevoEventoSinNombreSigueCreandoElEvento() throws Exception {
        var created = new ArrayList<String>();
        var form = new NuevoEventoScreen(funciones, created::add);
        FxTest.onFx(() -> {
            var root = form.build();
            var name = all(root).stream().filter(n -> n instanceof Label l && "Nombre (opcional)".equals(l.getText()))
                    .map(n -> (TextField) ((Label) n).getLabelFor()).findFirst().orElseThrow();
            assertTrue(name.getPromptText() == null || name.getPromptText().isEmpty(), "sin texto de ejemplo");
            assertTrue(name.getText().isEmpty());
            all(root).stream().filter(n -> n instanceof Button b && "Crear evento".equals(b.getText()))
                    .map(n -> (Button) n).findFirst().orElseThrow().fire();
            return null;
        });
        assertEquals(1, created.size());
        assertEquals(1, funciones.cartelera(created.get(0)).size());
    }
}
