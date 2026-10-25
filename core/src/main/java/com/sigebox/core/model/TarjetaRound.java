package com.sigebox.core.model;

/**
 * La calificación vigente de un juez en un round.
 *
 * @param rojo 0–10
 * @param azul 0–10
 * @param horaMs ms desde epoch
 */
public record TarjetaRound(
        String peleaId,
        int juez,
        int round,
        int rojo,
        int azul,
        EstadoTarjeta estado,
        OrigenTarjeta origen,
        long horaMs) {}
