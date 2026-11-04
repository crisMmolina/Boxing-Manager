package com.sigebox.core.protocol;

import com.sigebox.core.model.EstadoTarjeta;

/**
 * ACK (central → juez).
 *
 * <p>Confirma un mensaje después de persistirlo. {@code status} cuando {@code ok}; {@code error} solo cuando {@code ok} es falso.
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
        Error error) implements Message {

    public static final String TYPE = "ACK";

    /** Código de rechazo; nunca texto del mensaje recibido. */
    public enum Error {

        MALFORMED,
        NOT_APPROVED,
        BAD_SIGNATURE,
        WRONG_SESSION,
        INVALID_SCORE,
        ROUND_NOT_OPEN,
        FIGHT_NOT_ACTIVE,
        ALREADY_FINAL
    }

    public Ack {
        Fields.base(v, id, type, TYPE);
        Fields.match(ackId, "ackId", Fields.UUID_PATTERN);
        Fields.require(ok, "ok");
        if (ok && error != null || !ok && status != null) {
            throw new IllegalArgumentException("status solo con ok; error solo sin ok");
        }
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public Ack(String ackId, Boolean ok, EstadoTarjeta status, Error error) {
        this(VERSION, Fields.newId(), TYPE, ackId, ok, status, error);
    }
}
