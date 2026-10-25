package com.sigebox.core.model;

/**
 * El lugar de un juez en una pelea.
 *
 * @param numero 1–5
 * @param aprobadoEnMs hora de aprobación en ms desde epoch; {@code null} si no está aprobado
 */
public record LugarJuez(
        String peleaId,
        int numero,
        String nombre,
        String dispositivo,
        String huellaCredencial,
        EstadoConexion conexion,
        String aprobadoPor,
        Long aprobadoEnMs) {}
