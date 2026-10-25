package com.sigebox.core.protocol;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * Prueba de que un mensaje viene del teléfono que se registró (amenaza Spoofing del plan).
 *
 * <p>El teléfono crea un par de llaves EC P-256 que nunca sale del equipo (Android Keystore) y manda la llave
 * pública en {@code JOIN}. La central abre cada conexión con {@code CHALLENGE{nonce}}; {@code JOIN} y {@code HELLO}
 * firman ese nonce con SHA256withECDSA, y {@code SUBMIT_FINAL_CARD} firma la tarjeta final. Así, capturar un
 * {@code HELLO} en la red no permite reconectarse como ese juez. Llaves y firmas viajan en Base64 estándar (la llave en
 * X.509 DER, la firma en DER). Solo APIs de Android 8 (API 26).
 */
public final class DeviceProof {

    private static final String ALGORITHM = "SHA256withECDSA";
    private static final int NONCE_BYTES = 16;

    private DeviceProof() {}

    /** Texto que firma {@code JOIN}. */
    public static String joinText(String fightId, int seat, String token, String deviceId, String nonce) {
        return "SIGEBOX-JOIN-v1\n" + fightId + "\n" + seat + "\n" + token + "\n" + deviceId + "\n" + nonce + "\n";
    }

    /** Texto que firma {@code HELLO}. */
    public static String helloText(String fightId, int seat, String deviceId, String nonce) {
        return "SIGEBOX-HELLO-v1\n" + fightId + "\n" + seat + "\n" + deviceId + "\n" + nonce + "\n";
    }

    /** Texto que firma {@code SUBMIT_FINAL_CARD}: el mismo texto canónico de {@link FinalCardHash}. */
    public static String finalCardText(String fightId, int seat, List<State.OwnCard> cards) {
        return FinalCardHash.canonical(fightId, seat, cards);
    }

    /** Nonce de 128 bits en Base64 URL sin relleno (22 caracteres). */
    public static String newNonce(SecureRandom random) {
        byte[] bytes = new byte[NONCE_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /** Huella de la credencial del equipo: SHA-256 en hex de la llave pública (X.509 DER). */
    public static String fingerprint(String publicKey) {
        return FinalCardHash.sha256Hex(Base64.getDecoder().decode(publicKey));
    }

    /** Firma {@code text}; en Android sirve con una llave del Keystore. */
    public static String sign(PrivateKey key, String text) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(key);
            signature.update(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("no se pudo firmar", e);
        }
    }

    /** {@code true} solo si {@code sig} es una firma válida de {@code text} con una llave EC P-256. */
    public static boolean verify(String publicKey, String text, String sig) {
        try {
            PublicKey key = KeyFactory.getInstance("EC")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
            if (!(key instanceof ECPublicKey) || ((ECPublicKey) key).getParams().getCurve().getField().getFieldSize() != 256) {
                return false;
            }
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(key);
            signature.update(text.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sig));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }
}
