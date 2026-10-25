package com.sigebox.core.model;

/**
 * Un round de una pelea. Las horas son ms desde epoch; {@code null} si aún no ocurren.
 *
 * @param cierreForzado se cerró con jueces faltantes
 */
public record Round(
        String peleaId, int numero, EstadoRound estado, Long abiertoEnMs, Long cerradoEnMs, boolean cierreForzado) {}
