package com.sigebox.central.ui.inicio;

import com.sigebox.central.service.FuncionService;
import com.sigebox.central.ui.LiveRefresh;
import com.sigebox.central.ui.funcion.FuncionView;
import com.sigebox.central.ui.theme.Skeleton;
import com.sigebox.central.ui.theme.Ui;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Cartelera de un evento anterior, solo para consultar: peleas en orden con su estado. El evento en curso se
 * edita en el paso Evento.
 */
public final class CarteleraScreen {

    public static final String ID = "cartelera";

    private final FuncionService funciones;
    private final Supplier<String> selected;
    private final Executor reads;

    private record Snapshot(FuncionService.Funcion funcion, List<FuncionService.Fila> rows) {
    }

    public CarteleraScreen(FuncionService funciones, Supplier<String> selected) {
        this(funciones, selected, LiveRefresh.DIRECT);
    }

    /** Con las lecturas en {@code reads}: mientras no llegan, la pantalla muestra su skeleton. */
    public CarteleraScreen(FuncionService funciones, Supplier<String> selected, Executor reads) {
        this.funciones = funciones;
        this.selected = selected;
        this.reads = reads;
    }

    public Node build() {
        var id = selected.get();
        if (id == null) {
            return new VBox(24, Ui.screenTitle("Cartelera"), Ui.body("Elige un evento en Inicio."));
        }
        var root = new VBox(24, Ui.screenTitle("Cartelera"), Skeleton.lines(3));
        root.setMaxWidth(900);
        LiveRefresh.watch(root, reads, () -> new Snapshot(funciones.funcion(id), funciones.cartelera(id)),
                s -> LiveRefresh.swap(root, view(s)));
        return root;
    }

    private static List<Node> view(Snapshot s) {
        var f = s.funcion();
        var list = Ui.card(Ui.cardTitle(FuncionView.resumen(s.rows())));
        for (var fila : s.rows()) {
            var line = new HBox(12, Ui.body("Pelea " + fila.orden() + " · " + fila.roja() + " vs " + fila.azul()),
                    Ui.grow(), Ui.support(FuncionView.estado(fila.estado())));
            line.setAlignment(Pos.CENTER_LEFT);
            list.getChildren().add(line);
        }
        return List.of(Ui.screenTitle(InicioQuery.nombre(f)),
                Ui.support(InicioQuery.fecha(f.fecha()) + " · " + FuncionView.detalle(f.baseRounds(), f.baseJueces())
                        + ". Evento anterior: solo se consulta."),
                list);
    }
}
