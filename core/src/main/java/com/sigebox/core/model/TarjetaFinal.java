package com.sigebox.core.model;

/**
 * La tarjeta final firmada por un juez.
 *
 * @param huella SHA-256 en hex de {@link com.sigebox.core.protocol.FinalCardHash}
 */
public record TarjetaFinal(String peleaId, int juez, String huella, long horaMs) {}
