package com.sigebox.core.protocol;

/**
 * JOIN (juez → central).
 *
 * <p>El teléfono pide unirse a un lugar con el token del QR.
 */
public record Join(
        int v,
        String id,
        String type,
        String fightId,
        Integer seat,
        String token,
        String judgeName,
        String deviceId,
        String credentialFingerprint) implements Message {

    public static final String TYPE = "JOIN";

    public Join {
        Fields.base(v, id, type, TYPE);
        Fields.require(fightId, "fightId");
        Fields.require(seat, "seat");
        Fields.require(token, "token");
        Fields.require(judgeName, "judgeName");
        Fields.require(deviceId, "deviceId");
        Fields.require(credentialFingerprint, "credentialFingerprint");
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public Join(
            String fightId,
            Integer seat,
            String token,
            String judgeName,
            String deviceId,
            String credentialFingerprint) {
        this(VERSION, Fields.newId(), TYPE, fightId, seat, token, judgeName, deviceId, credentialFingerprint);
    }
}
