package com.sigebox.central.export;

import com.sigebox.core.model.Descuento;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Resultado;
import com.sigebox.core.model.Terminacion;
import java.util.Comparator;
import java.util.List;

/**
 * Todo lo que muestra la pantalla de resultado y lo que se exporta: la frase del resultado, la
 * tarjeta de cada juez por round y sus totales, ya con los descuentos (calculados por {@code core}).
 *
 * @param terminacion null si la pelea llegó al último round
 */
public record ResultSheet(Pelea pelea, Resultado resultado, String headline, List<JudgeRow> rows,
        Terminacion terminacion) {

    /** Un round de la tarjeta de un juez; {@code null} en los valores si no cuenta o no llegó. */
    public record Cell(int round, Integer red, Integer blue, String status) {}

    public record JudgeRow(int seat, String name, List<Cell> cells, int totalRed, int totalBlue, String winner) {}

    public ResultSheet {
        rows = List.copyOf(rows);
    }

    /** Descuentos que sí restaron en la tarjeta de ese juez. */
    public List<Descuento> descuentos(int seat) {
        return resultado.totales().stream().filter(t -> t.juez() == seat)
                .flatMap(t -> t.descuentos().stream()).toList();
    }

    /** Descuentos que restaron en alguna tarjeta, por round. */
    public List<Descuento> descuentos() {
        return resultado.totales().stream().flatMap(t -> t.descuentos().stream()).distinct()
                .sorted(Comparator.comparingInt(Descuento::round).thenComparingLong(Descuento::id)).toList();
    }
}
