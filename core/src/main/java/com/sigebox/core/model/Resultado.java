package com.sigebox.core.model;

import java.util.List;

/**
 * Resultado calculado por {@code core}.
 *
 * @param tipo tipo de decisión por tarjetas; {@code null} si la pelea terminó por motivo
 * @param motivo motivo de terminación; {@code null} si se decidió por tarjetas
 */
public record Resultado(
        String peleaId, List<TotalJuez> totales, Ganador ganador, TipoDecision tipo, MotivoTerminacion motivo,
        Conteo conteo) {

    /** Total y fallo de un juez. */
    public record TotalJuez(int juez, String nombre, int rojo, int azul, Ganador ganador) {}

    /** Tarjetas vigentes por estado. */
    public record Conteo(int enviadas, int tardias, int capturadas, int corregidas) {}
}
