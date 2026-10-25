package com.sigebox.central.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Tokens de un solo uso por lugar (van en el QR). Los intentos fallidos se cuentan por lugar, no
 * por conexión; al agotarlos el token se descarta y hace falta emitir uno nuevo.
 */
public final class SeatTokens {

    public enum Check { OK, INVALID, USED }

    /** Intentos fallidos permitidos por lugar antes de descartar su token. */
    public static final int MAX_FAILURES = 5;

    private record Key(String fightId, int seat) {}

    private static final class Entry {
        final String token;
        boolean used;
        int failures;

        Entry(String token) {
            this.token = token;
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
        var bytes = new byte[16];
        random.nextBytes(bytes);
        var token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(new Key(fightId, seat), new Entry(token));
        return token;
    }

    /** Consume el token si coincide. */
    public synchronized Check consume(String fightId, int seat, String token) {
        var key = new Key(fightId, seat);
        var entry = tokens.get(key);
        if (entry == null) {
            return Check.INVALID;
        }
        boolean matches = token != null && MessageDigest.isEqual(
                entry.token.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
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
}
