package com.sigebox.core.model;

/**
 * Una pelea (IDEA §6).
 *
 * @param rounds 1–12
 * @param jueces 1–5
 * @param fecha fecha ISO-8601 ({@code 2026-10-24})
 */
public record Pelea(
        String id, String esquinaRoja, String esquinaAzul, int rounds, int jueces, EstadoPelea estado, String fecha) {}
