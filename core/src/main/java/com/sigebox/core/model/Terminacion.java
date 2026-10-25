package com.sigebox.core.model;

/**
 * Terminación extraordinaria (RO-08).
 *
 * @param textoOtro razón escrita, solo con {@link MotivoTerminacion#OTRO}
 * @param ganadora {@code null} si el motivo no da ganador (sin decisión)
 * @param tiempo tiempo del round en {@code m:ss}
 */
public record Terminacion(
        String peleaId,
        MotivoTerminacion motivo,
        String textoOtro,
        Esquina ganadora,
        int round,
        String tiempo,
        String observaciones) {}
