package com.sigebox.central.ui.funcion;

import com.sigebox.central.service.FuncionService.Estado;
import com.sigebox.central.service.FuncionService.Fila;
import java.util.List;
import java.util.Optional;

/** Textos y reglas de la cartelera, sin JavaFX (DESIGN v3 §8: frase corta, sin signos de exclamación). */
public final class FuncionView {

    private FuncionView() {
    }

    public static String estado(Estado estado) {
        return switch (estado) {
            case PROGRAMADA -> "Programada";
            case EN_CURSO -> "En curso";
            case CERRADA -> "Cerrada";
            case SALTADA -> "Saltada";
        };
    }

    /** "10 rounds · 3 jueces". */
    public static String detalle(int rounds, int jueces) {
        return rounds + (rounds == 1 ? " round" : " rounds") + " · " + jueces + (jueces == 1 ? " juez" : " jueces");
    }

    /** Solo una pelea programada se puede mover, saltar, quitar o cambiar. */
    public static boolean editable(Fila fila) {
        return fila.estado() == Estado.PROGRAMADA;
    }

    /** La siguiente pelea que inicia la función: la primera programada. */
    public static Optional<Fila> siguiente(List<Fila> cartelera) {
        return cartelera.stream().filter(FuncionView::editable).findFirst();
    }

    /** Etiqueta del botón que inicia la siguiente pelea. */
    public static String siguienteTexto(Fila fila) {
        return "Siguiente pelea: " + fila.roja() + " vs " + fila.azul();
    }

    /** "3 peleas · 1 cerrada" para el encabezado de la cartelera. */
    public static String resumen(List<Fila> cartelera) {
        long cerradas = cartelera.stream().filter(f -> f.estado() == Estado.CERRADA).count();
        return cartelera.size() + (cartelera.size() == 1 ? " pelea" : " peleas") + " · " + cerradas
                + (cerradas == 1 ? " cerrada" : " cerradas");
    }
}
