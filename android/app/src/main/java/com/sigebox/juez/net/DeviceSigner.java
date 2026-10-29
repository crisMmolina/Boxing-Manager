package com.sigebox.juez.net;

/** Identidad del teléfono: llave EC P-256 que no sale del equipo (ver {@code DeviceProof} en core). */
public interface DeviceSigner {
    String deviceId();

    /** Llave pública X.509 DER en Base64 estándar. */
    String publicKey();

    /** Firma SHA256withECDSA de {@code text} en Base64 estándar. */
    String sign(String text);
}
