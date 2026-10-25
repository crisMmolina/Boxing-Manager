package com.sigebox.core.protocol;

import com.sigebox.core.model.EstadoTarjeta;

/**
 * ACK (central → juez).
 *
 * <p>Confirma un mensaje después de persistirlo. {@code error} solo cuando {@code ok} es falso.
 *
 * <p>Opcionales ({@code null} se omite): {@code status}, {@code error}.
 */
public record Ack(
        int v,
        String id,
        String type,
        String ackId,
        Boolean ok,
        EstadoTarjeta status,
        String error) implements Message {

    public static final String TYPE = "ACK";

    public Ack {
        Fields.base(v, id, type, TYPE);
        Fields.require(ackId, "ackId");
        Fields.require(ok, "ok");
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public Ack(String ackId, Boolean ok, EstadoTarjeta status, String error) {
        this(VERSION, Fields.newId(), TYPE, ackId, ok, status, error);
    }
}
