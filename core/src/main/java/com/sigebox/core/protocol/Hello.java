package com.sigebox.core.protocol;

/**
 * HELLO (juez → central).
 *
 * <p>Un juez ya aprobado se reconecta; {@code sig} firma {@link DeviceProof#helloText} con la llave registrada en {@code JOIN}. La central responde con {@code STATE}.
 */
public record Hello(
        int v,
        String id,
        String type,
        String fightId,
        Integer seat,
        String deviceId,
        String nonce,
        String sig) implements Message {

    public static final String TYPE = "HELLO";

    public Hello {
        Fields.base(v, id, type, TYPE);
        Fields.text(fightId, "fightId", 1, 64);
        Fields.range(seat, "seat", 1, 5);
        Fields.text(deviceId, "deviceId", 1, 128);
        Fields.text(nonce, "nonce", 22, 128);
        Fields.match(nonce, "nonce", Fields.NONCE);
        Fields.text(sig, "sig", 40, 200);
        Fields.match(sig, "sig", Fields.BASE64);
    }

    /** Mensaje nuevo con {@code v = 1} e {@code id} aleatorio. */
    public Hello(String fightId, Integer seat, String deviceId, String nonce, String sig) {
        this(VERSION, Fields.newId(), TYPE, fightId, seat, deviceId, nonce, sig);
    }
}
