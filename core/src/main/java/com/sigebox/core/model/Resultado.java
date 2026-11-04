package com.sigebox.core.model;

import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * Total y fallo de un juez. Los puntos ya tienen restados los descuentos.
     *
     * @param rounds puntos por round ya descontados; vacío si el resultado se calculó sin desglose
     * @param descuentos descuentos que sí restaron en esta tarjeta: uno sobre un 0 no resta y no aparece
     */
    public record TotalJuez(
            int juez, String nombre, int rojo, int azul, Ganador ganador, List<RoundJuez> rounds,
            List<Descuento> descuentos) {

        public TotalJuez {
            rounds = copia(rounds);
            descuentos = copia(descuentos);
        }

        /** Total sin desglose. */
        public TotalJuez(int juez, String nombre, int rojo, int azul, Ganador ganador) {
            this(juez, nombre, rojo, azul, ganador, null, null);
        }

        // null: tarjetas guardadas o leídas de JSON antes de que existiera el desglose.
        private static <T> List<T> copia(List<T> lista) {
            return lista == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(lista));
        }
    }

    /** Puntos de un juez en un round, ya descontados. */
    public record RoundJuez(int round, int rojo, int azul) {}

    /** Tarjetas vigentes por estado. */
    public record Conteo(int enviadas, int tardias, int capturadas, int corregidas) {}
}
