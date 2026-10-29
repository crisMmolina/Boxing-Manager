package com.sigebox.central.ui.funcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.service.FuncionService.Estado;
import com.sigebox.central.service.FuncionService.Fila;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Textos y reglas de la cartelera (DSK-021), sin JavaFX. */
class FuncionViewTest {

    private static Fila fila(int orden, Estado estado) {
        return new Fila(orden, "Roja " + orden, "Azul " + orden, 10, 3, false, estado, null);
    }

    @Test
    void everyStateHasItsName() {
        assertEquals("Programada", FuncionView.estado(Estado.PROGRAMADA));
        assertEquals("En curso", FuncionView.estado(Estado.EN_CURSO));
        assertEquals("Cerrada", FuncionView.estado(Estado.CERRADA));
        assertEquals("Saltada", FuncionView.estado(Estado.SALTADA));
    }

    @Test
    void onlyAScheduledFightCanBeEdited() {
        assertTrue(FuncionView.editable(fila(1, Estado.PROGRAMADA)));
        for (var e : List.of(Estado.EN_CURSO, Estado.CERRADA, Estado.SALTADA)) {
            assertFalse(FuncionView.editable(fila(1, e)), e.name());
        }
    }

    @Test
    void nextIsTheFirstScheduledFightAndSkipsTheSkippedOnes() {
        var cartelera = List.of(fila(1, Estado.CERRADA), fila(2, Estado.SALTADA), fila(3, Estado.PROGRAMADA),
                fila(4, Estado.PROGRAMADA));
        var next = FuncionView.siguiente(cartelera).orElseThrow();
        assertEquals(3, next.orden());
        assertEquals("Siguiente pelea: Roja 3 vs Azul 3", FuncionView.siguienteTexto(next));
        assertTrue(FuncionView.siguiente(List.of(fila(1, Estado.CERRADA), fila(2, Estado.SALTADA))).isEmpty());
    }

    @Test
    void detailAndSummaryUseSingularAndPlural() {
        assertEquals("10 rounds · 3 jueces", FuncionView.detalle(10, 3));
        assertEquals("1 round · 1 juez", FuncionView.detalle(1, 1));
        assertEquals("3 peleas · 1 cerrada",
                FuncionView.resumen(List.of(fila(1, Estado.CERRADA), fila(2, Estado.EN_CURSO),
                        fila(3, Estado.PROGRAMADA))));
        assertEquals("1 pelea · 0 cerradas", FuncionView.resumen(List.of(fila(1, Estado.PROGRAMADA))));
    }
}
