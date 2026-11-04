package com.sigebox.core.model;

/**
 * Descuento de un punto a una esquina en un round (ADR 0005). Lo registra la central; un descuento anulado sigue
 * guardado, pero no cuenta en el resultado.
 *
 * @param puntos siempre {@link #PUNTOS}
 * @param motivoAnulacion {@code null} si no está anulado
 */
public record Descuento(
        long id,
        String fightId,
        int round,
        Esquina esquina,
        int puntos,
        boolean anulado,
        String motivoAnulacion) {

    /** Puntos de cada descuento. */
    public static final int PUNTOS = 1;
}
