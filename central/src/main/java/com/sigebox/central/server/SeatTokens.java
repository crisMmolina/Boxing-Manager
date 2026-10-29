package com.sigebox.central.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Credenciales de un solo uso por lugar: el token del QR (22 caracteres) y el código manual de 6
 * dígitos que el juez escribe si no puede escanear (contrato AND-004). Cualquiera de los dos une al
 * lugar y los dos quedan usados. Los intentos fallidos se cuentan por lugar, no por conexión; al
 * agotarlos se descartan y hace falta emitir otros. Cada conexión tiene un solo intento de
 * {@code JOIN} (un nonce), así que el límite por conexión es uno.
 */
public final class SeatTokens {

    public enum Check { OK, INVALID, USED }

    /** Lo que muestra la central por lugar: el token va en el QR y el código se lee en voz alta. */
    public record Invitation(String token, String code) {}

    /** Intentos fallidos permitidos por lugar antes de descartar su token. */
    public static final int MAX_FAILURES = 5;

    public static final int CODE_LENGTH = 6;

    private record Key(String fightId, int seat) {}

    private static final class Entry {
        final String token;
        final String code;
        boolean used;
        int failures;

        Entry(String token, String code) {
            this.token = token;
            this.code = code;
        }
    }

    private final SecureRandom random;
    private final Map<Key, Entry> tokens = new HashMap<>();

    public SeatTokens() {
        this(new SecureRandom());
    }

    SeatTokens(SecureRandom random) {
        this.random = random;
    }

    /** Emite (o reemplaza) el token del lugar: 22 caracteres base64url, 128 bits aleatorios. */
    public synchronized String issue(String fightId, int seat) {
        return issueInvitation(fightId, seat).token();
    }

    /** Emite (o reemplaza) el token y el código del lugar; los anteriores dejan de servir. */
    public synchronized Invitation issueInvitation(String fightId, int seat) {
        var bytes = new byte[16];
        random.nextBytes(bytes);
        var token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var code = String.format("%0" + CODE_LENGTH + "d", random.nextInt(1_000_000));
        tokens.put(new Key(fightId, seat), new Entry(token, code));
        return new Invitation(token, code);
    }

    /** Consume el token si coincide. */
    public synchronized Check consume(String fightId, int seat, String token) {
        var key = new Key(fightId, seat);
        var entry = tokens.get(key);
        if (entry == null) {
            return Check.INVALID;
        }
        // Se comparan los dos siempre, en tiempo constante.
        boolean byToken = equal(entry.token, token);
        boolean byCode = equal(entry.code, token);
        boolean matches = byToken | byCode;
        if (matches) {
            if (entry.used) {
                return Check.USED;
            }
            entry.used = true;
            return Check.OK;
        }
        if (++entry.failures >= MAX_FAILURES) {
            tokens.remove(key);
        }
        return Check.INVALID;
    }

    private static boolean equal(String expected, String given) {
        return given != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }
}
