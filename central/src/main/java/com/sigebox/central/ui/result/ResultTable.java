package com.sigebox.central.ui.result;

import com.sigebox.central.export.ResultSheet;
import com.sigebox.central.ui.Labels;
import com.sigebox.central.ui.theme.Ui;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/**
 * Tabla de puntos de una pelea: una columna por juez con cada round, sus descuentos y la suma de cada
 * esquina. Todo viene ya calculado por {@code core}; aquí no se decide nada ni se nombra ganador.
 */
final class ResultTable {

    private ResultTable() {
    }

    static GridPane of(ResultSheet sheet) {
        var grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);
        int rounds = sheet.pelea().rounds();
        grid.add(Ui.support("Round"), 0, 0);
        for (int r = 1; r <= rounds; r++) {
            grid.add(Ui.body("R" + r), 0, r);
        }
        grid.add(Ui.body("Descuentos"), 0, rounds + 1);
        grid.add(Ui.body("Suma"), 0, rounds + 2);

        int x = 1;
        for (var row : sheet.rows()) {
            grid.add(Ui.support("Juez " + row.seat() + (row.name() == null ? "" : " · " + row.name())), x, 0);
            for (var c : row.cells()) {
                grid.add(c.red() == null ? Ui.support("—") : score(c.red(), c.blue()), x, c.round());
            }
            grid.add(Ui.support(descuentos(sheet, row.seat())), x, rounds + 1);
            grid.add(score(row.totalRed(), row.totalBlue()), x, rounds + 2);
            x++;
        }
        return grid;
    }

    private static Node score(int red, int blue) {
        return new HBox(4, Ui.numeral(String.valueOf(red), "red"), Ui.numeral("–", "neutral"),
                Ui.numeral(String.valueOf(blue), "blue"));
    }

    /** "R2 Roja −1", uno por línea. */
    private static String descuentos(ResultSheet sheet, int seat) {
        var lines = sheet.descuentos(seat).stream()
                .map(d -> "R" + d.round() + " " + Labels.of(d.esquina()) + " −" + d.puntos())
                .collect(Collectors.joining("\n"));
        return lines.isEmpty() ? "—" : lines;
    }
}
