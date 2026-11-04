package com.sigebox.core.protocol;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * URI del QR de un lugar de juez: {@code sigebox://join?h=<host>&p=<puerto>&c=<código>[&f=<huella>]}. La forma anterior,
 * {@code sigebox://join?h=<host>&p=<puerto>&s=<fightId>&seat=<n>&t=<token>[&f=<huella>]}, se sigue leyendo y generando.
 *
 * <p>Con {@code code}, la URI no dice pelea ni lugar: {@code fightId} y {@code token} son {@code null} y {@code seat}
 * es 0. El teléfono los obtiene con CODE_LOOKUP antes de JOIN, y manda el código como token.
 *
 * @param port 1–65535
 * @param fightId pelea en la forma anterior; {@code null} con {@code code}
 * @param seat 1–5 en la forma anterior; 0 con {@code code}
 * @param token token de un solo uso del lugar en la forma anterior; {@code null} con {@code code}
 * @param code código de 6 dígitos del lugar; {@code null} en la forma anterior
 * @param certFingerprint SHA-256 en hex del certificado TLS de la central para fijarlo; {@code null} mientras la
 *     conexión sea {@code ws://} (reservado para {@code wss://})
 */
public record JoinUri(String host, int port, String fightId, int seat, String token, String code,
        String certFingerprint) {

    public static final String SCHEME = "sigebox";
    public static final String ACTION = "join";

    public JoinUri {
        Fields.text(host, "h", 1, 253);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("puerto fuera de rango: " + port);
        }
        if (code == null) {
            Fields.text(fightId, "s", 1, 64);
            Fields.text(token, "t", 1, 128);
            if (seat < 1 || seat > 5) {
                throw new IllegalArgumentException("lugar fuera de rango: " + seat);
            }
        } else {
            Fields.match(code, "c", Fields.CODE);
            if (fightId != null || seat != 0 || token != null) {
                throw new IllegalArgumentException("c no se mezcla con s, seat ni t");
            }
        }
        if (certFingerprint != null) {
            Fields.match(certFingerprint, "f", Fields.HEX64);
        }
    }

    /** Forma anterior, con pelea, lugar y token. */
    public JoinUri(String host, int port, String fightId, int seat, String token, String certFingerprint) {
        this(host, port, fightId, seat, token, null, certFingerprint);
    }

    /** Forma con código de 6 dígitos. */
    public JoinUri(String host, int port, String code, String certFingerprint) {
        this(host, port, null, 0, null, code, certFingerprint);
    }

    /**
     * Lee las dos formas; si llegan las dos, manda {@code c}.
     *
     * @throws ProtocolException si no es una URI de unión válida
     */
    public static JoinUri parse(String uri) {
        try {
            URI parsed = new URI(uri);
            if (!SCHEME.equals(parsed.getScheme()) || !ACTION.equals(parsed.getAuthority())) {
                throw new IllegalArgumentException("no es " + SCHEME + "://" + ACTION);
            }
            Map<String, String> q = query(parsed.getRawQuery());
            if (q.containsKey("c")) {
                return new JoinUri(q.get("h"), number(q, "p"), q.get("c"), q.get("f"));
            }
            return new JoinUri(q.get("h"), number(q, "p"), q.get("s"), number(q, "seat"), q.get("t"), q.get("f"));
        } catch (Exception e) {
            throw new ProtocolException("URI de unión inválida", e);
        }
    }

    public String toUri() {
        String place = code != null ? "&c=" + code
                : "&s=" + encode(fightId) + "&seat=" + seat + "&t=" + encode(token);
        return SCHEME + "://" + ACTION + "?h=" + encode(host) + "&p=" + port + place
                + (certFingerprint == null ? "" : "&f=" + certFingerprint);
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
