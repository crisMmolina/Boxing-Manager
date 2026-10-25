package com.sigebox.core.protocol;

/**
 * HELLO (juez → central).
 *
 * <p>Un juez ya aprobado se reconecta; la central responde con {@code STATE}.
 */
public record Hello(
        int v,
        String id,
        String type,
        String fightId,
        Integer seat,
        String deviceId,
        String credentialFingerprint) implements Message {

    public static final String TYPE = "HELLO";

    public Hello {
        Fields.base(v, id, type, TYPE);
        Fields.require(fightId, "fightId");
        Fields.require(seat, "seat");
        Fields.require(deviceId, "deviceId");
        Fields.require(credentialFingerprint, "credentialFingerprint");
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public Hello(String fightId, Integer seat, String deviceId, String credentialFingerprint) {
        this(VERSION, Fields.newId(), TYPE, fightId, seat, deviceId, credentialFingerprint);
    }
}
