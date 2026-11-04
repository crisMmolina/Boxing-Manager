package com.sigebox.juez.net;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import com.sigebox.core.protocol.DeviceProof;

import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;

/** Llave EC P-256 del teléfono en Android Keystore: la privada nunca sale del equipo. */
public final class KeystoreSigner implements DeviceSigner {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String ALIAS = "sigebox-device";

    private final PrivateKey privateKey;
    private final String publicKey;

    public KeystoreSigner() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(ALIAS)) {
                KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE);
                generator.initialize(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                        .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .build());
                generator.generateKeyPair();
            }
            privateKey = (PrivateKey) keyStore.getKey(ALIAS, null);
            PublicKey pub = keyStore.getCertificate(ALIAS).getPublicKey();
            publicKey = DeviceProof.encodePublicKey(pub);
        } catch (GeneralSecurityException | java.io.IOException e) {
            throw new IllegalStateException("no se pudo abrir la llave del equipo", e);
        }
    }

    /** La huella de la llave pública: estable mientras no se reinstale la app. */
    @Override
    public String deviceId() {
        return DeviceProof.fingerprint(publicKey);
    }

    @Override
    public String publicKey() {
        return publicKey;
    }

    @Override
    public String sign(String text) {
        return DeviceProof.sign(privateKey, text);
    }
}
