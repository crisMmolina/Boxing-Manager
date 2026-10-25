package com.sigebox.core.model;

/**
 * Un cambio de una tarjeta (RJ-05, RO-09).
 *
 * @param rojoAnterior {@code null} en la primera versión
 * @param azulAnterior {@code null} en la primera versión
 * @param motivo obligatorio cuando {@code quien} es el organizador
 */
public record VersionTarjeta(
        String peleaId,
        int juez,
        int round,
        Integer rojoAnterior,
        Integer azulAnterior,
        int rojoNuevo,
        int azulNuevo,
        OrigenTarjeta quien,
        String motivo,
        long horaMs) {}
