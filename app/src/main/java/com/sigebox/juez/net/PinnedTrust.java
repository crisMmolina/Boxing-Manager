package com.sigebox.juez.net;

import com.sigebox.core.protocol.FinalCardHash;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

/**
 * Para {@code wss://}: solo se acepta el certificado cuya huella SHA-256 vino en el QR (parámetro {@code f}). La
 * central usa un certificado propio sin autoridad ni nombre de dominio, así que la huella sustituye a ambos.
 */
public final class PinnedTrust implements X509TrustManager {
    private final String fingerprint;

    public PinnedTrust(String fingerprint) {
        this.fingerprint = fingerprint.toLowerCase(Locale.ROOT);
    }

    public static OkHttpClient pin(OkHttpClient base, String fingerprint) {
        PinnedTrust trust = new PinnedTrust(fingerprint);
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] {trust}, new SecureRandom());
            return base.newBuilder()
                    .sslSocketFactory(context.getSocketFactory(), trust)
                    // El certificado ya está fijado por huella; la IP de la LAN no coincide con ningún nombre.
                    .hostnameVerifier((host, sslSession) -> true)
                    .build();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("no se pudo preparar TLS", e);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new CertificateException("sin certificado");
        }
        String actual = FinalCardHash.sha256Hex(chain[0].getEncoded());
        if (!fingerprint.equals(actual)) {
            throw new CertificateException("el certificado de la central no coincide con el QR");
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        throw new CertificateException("el juez no acepta clientes");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
