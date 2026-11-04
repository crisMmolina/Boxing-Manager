package com.sigebox.central.ui.inicio;

import com.sigebox.central.service.FuncionService;
import com.sigebox.central.ui.funcion.FuncionView;
import com.sigebox.central.ui.theme.Ui;
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

    public CarteleraScreen(FuncionService funciones, Supplier<String> selected) {
        this.funciones = funciones;
        this.selected = selected;
    }

    public Node build() {
        var id = selected.get();
        if (id == null) {
            return new VBox(24, Ui.screenTitle("Cartelera"), Ui.body("Elige un evento en Inicio."));
        }
        var f = funciones.funcion(id);
        var rows = funciones.cartelera(id);
        var list = Ui.card(Ui.cardTitle(FuncionView.resumen(rows)));
        for (var fila : rows) {
            var line = new HBox(12, Ui.body("Pelea " + fila.orden() + " · " + fila.roja() + " vs " + fila.azul()),
                    Ui.grow(), Ui.support(FuncionView.estado(fila.estado())));
            line.setAlignment(Pos.CENTER_LEFT);
            list.getChildren().add(line);
        }
        var root = new VBox(24, Ui.screenTitle(InicioQuery.nombre(f)),
                Ui.support(InicioQuery.fecha(f.fecha()) + " · " + FuncionView.detalle(f.baseRounds(), f.baseJueces())
                        + ". Evento anterior: solo se consulta."),
                list);
        root.setMaxWidth(900);
        return root;
    }
}
