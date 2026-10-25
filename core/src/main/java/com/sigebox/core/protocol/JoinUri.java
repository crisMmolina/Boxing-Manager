package com.sigebox.core.protocol;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * URI del QR de un lugar de juez: {@code sigebox://join?h=<host>&p=<puerto>&s=<fightId>&seat=<n>&t=<token>[&f=<huella>]}.
 *
 * @param port 1–65535
 * @param seat 1–5
 * @param token token de un solo uso del lugar
 * @param certFingerprint SHA-256 en hex del certificado TLS de la central para fijarlo; {@code null} mientras la
 *     conexión sea {@code ws://} (reservado para {@code wss://})
 */
public record JoinUri(String host, int port, String fightId, int seat, String token, String certFingerprint) {

    public static final String SCHEME = "sigebox";
    public static final String ACTION = "join";

    public JoinUri {
        requireText(host, "h");
        requireText(fightId, "s");
        requireText(token, "t");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("puerto fuera de rango: " + port);
        }
        if (seat < 1 || seat > 5) {
            throw new IllegalArgumentException("lugar fuera de rango: " + seat);
        }
        if (certFingerprint != null) {
            Fields.match(certFingerprint, "f", Fields.HEX64);
        }
    }

    /** @throws ProtocolException si no es una URI de unión válida */
    public static JoinUri parse(String uri) {
        try {
            URI parsed = new URI(uri);
            if (!SCHEME.equals(parsed.getScheme()) || !ACTION.equals(parsed.getAuthority())) {
                throw new IllegalArgumentException("no es " + SCHEME + "://" + ACTION);
            }
            Map<String, String> q = query(parsed.getRawQuery());
            return new JoinUri(q.get("h"), number(q, "p"), q.get("s"), number(q, "seat"), q.get("t"), q.get("f"));
        } catch (Exception e) {
            throw new ProtocolException("URI de unión inválida", e);
        }
    }

    public String toUri() {
        return SCHEME + "://" + ACTION + "?h=" + encode(host) + "&p=" + port + "&s=" + encode(fightId)
                + "&seat=" + seat + "&t=" + encode(token) + (certFingerprint == null ? "" : "&f=" + certFingerprint);
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null) {
            return out;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                out.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            }
        }
        return out;
    }

    private static int number(Map<String, String> q, String key) {
        String value = q.get(key);
        if (value == null) {
            throw new IllegalArgumentException("falta " + key);
        }
        return Integer.parseInt(value);
    }

    private static void requireText(String value, String key) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("falta " + key);
        }
    }

    // Las sobrecargas con Charset no existen en Android 8 (API 26).
    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
