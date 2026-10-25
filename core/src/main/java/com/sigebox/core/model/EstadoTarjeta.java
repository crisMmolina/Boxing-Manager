package com.sigebox.core.model;

/** Estado de la tarjeta de un round. {@code CORREGIDA} gana sobre el estado de la versión anterior. */
public enum EstadoTarjeta {
    ENVIADA, TARDIA, CAPTURADA, CORREGIDA
}
