package com.sigebox.core.protocol;

/**
 * JOIN (juez → central).
 *
 * <p>El teléfono pide unirse a un lugar con el token del QR (o el código de 6 dígitos) y registra su llave pública; {@code sig} firma {@link DeviceProof#joinText}.
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
        String publicKey,
        String nonce,
        String sig) implements Message {

    public static final String TYPE = "JOIN";

    public Join {
        Fields.base(v, id, type, TYPE);
        Fields.text(fightId, "fightId", 1, 64);
        Fields.range(seat, "seat", 1, 5);
        Fields.text(token, "token", 6, 128);
        Fields.text(judgeName, "judgeName", 1, 80);
        Fields.text(deviceId, "deviceId", 1, 128);
        Fields.text(publicKey, "publicKey", 60, 400);
        Fields.match(publicKey, "publicKey", Fields.BASE64);
        Fields.text(nonce, "nonce", 22, 128);
        Fields.match(nonce, "nonce", Fields.NONCE);
        Fields.text(sig, "sig", 40, 200);
        Fields.match(sig, "sig", Fields.BASE64);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public Join(
            String fightId,
            Integer seat,
            String token,
            String judgeName,
            String deviceId,
            String publicKey,
            String nonce,
            String sig) {
        this(VERSION, Fields.newId(), TYPE, fightId, seat, token, judgeName, deviceId, publicKey, nonce, sig);
    }
}
