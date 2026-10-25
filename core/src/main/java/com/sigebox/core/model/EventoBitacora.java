package com.sigebox.core.model;

/** Un evento de la bitácora (RO-13). */
public record EventoBitacora(String peleaId, String tipo, String detalle, long horaMs) {}
