package com.sigebox.juez.net;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sigebox.core.protocol.FinalCardHash;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.junit.Test;

public class PinnedTrustTest {

    private static X509Certificate cert(String der) throws Exception {
        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getEncoded()).thenReturn(der.getBytes(StandardCharsets.UTF_8));
        return cert;
    }

    @Test
    public void aceptaSoloElCertificadoDelQr() throws Exception {
        X509Certificate central = cert("central");
        String fingerprint = FinalCardHash.sha256Hex("central".getBytes(StandardCharsets.UTF_8));
        PinnedTrust trust = new PinnedTrust(fingerprint.toUpperCase());

        trust.checkServerTrusted(new X509Certificate[] {central}, "ECDHE_ECDSA");
        assertThrows(CertificateException.class,
                () -> trust.checkServerTrusted(new X509Certificate[] {cert("impostor")}, "ECDHE_ECDSA"));
        assertThrows(CertificateException.class, () -> trust.checkServerTrusted(new X509Certificate[0], "RSA"));
    }
}
