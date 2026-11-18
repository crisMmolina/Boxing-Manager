package com.sigebox.central.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Credenciales de un solo uso por lugar: el token del QR (22 caracteres) y el código manual de 6
 * dígitos que el juez escribe si no puede escanear (contrato AND-004). Cualquiera de los dos une al
 * lugar y los dos quedan usados. Los intentos fallidos se cuentan por lugar, no por conexión; al
 * agotarlos se descartan y hace falta emitir otros. Cada conexión tiene un solo intento de
 * {@code JOIN} (un nonce), así que el límite por conexión es uno.
 *
 * <p>El código es también el código de lugar de {@code CODE_LOOKUP} (ADR 0004): es único entre los
 * lugares de las peleas abiertas y {@link #forget} libera los de una pelea al cerrarse.
 */
public final class SeatTokens {

    public enum Check { OK, INVALID, USED }

    /** Lo que muestra la central por lugar: el token va en el QR y el código se lee en voz alta. */
    public record Invitation(String token, String code) {}

    /** El lugar al que pertenece un código. */
    public record Seat(String fightId, int seat) {}

    /** Intentos fallidos permitidos por lugar antes de descartar su token. */
    public static final int MAX_FAILURES = 5;

    public static final int CODE_LENGTH = 6;

    private record Key(String fightId, int seat) {}

    private static final class Entry {
        final String token;
        final String code;
        /** Código de vuelta (DSK-055): el único que abre un lugar ya registrado. */
        final boolean rejoin;
        boolean used;
        int failures;

        Entry(String token, String code, boolean rejoin) {
            this.token = token;
            this.code = code;
            this.rejoin = rejoin;
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
        return issue(fightId, seat, false);
    }

    private Invitation issue(String fightId, int seat, boolean rejoin) {
        var bytes = new byte[16];
        random.nextBytes(bytes);
        var token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var key = new Key(fightId, seat);
        String code;
        // También distinto del código anterior del mismo lugar, que deja de servir.
        do {
            code = String.format("%0" + CODE_LENGTH + "d", random.nextInt(1_000_000));
        } while (codeTaken(code));
        tokens.put(key, new Entry(token, code, rejoin));
        return new Invitation(token, code);
    }

    /** Emite un código de vuelta para un lugar ya registrado (DSK-055); el anterior deja de servir. */
    public synchronized Invitation issueRejoin(String fightId, int seat) {
        return issue(fightId, seat, true);
    }

    /** {@code true} si el lugar tiene un código de vuelta sin usar. */
    public synchronized boolean rejoinOpen(String fightId, int seat) {
        var entry = tokens.get(new Key(fightId, seat));
        return entry != null && entry.rejoin && !entry.used;
    }

    /**
     * Usa el código de vuelta si {@code token} es su token o su código. Un intento que no coincide no cuenta como
     * fallo: el mismo teléfono reenvía su JOIN viejo al reconectar.
     */
    public synchronized boolean consumeRejoin(String fightId, int seat, String token) {
        var entry = tokens.get(new Key(fightId, seat));
        if (entry == null || !entry.rejoin || entry.used) {
            return false;
        }
        boolean matches = equal(entry.token, token) | equal(entry.code, token);
        if (matches) {
            entry.used = true;
        }
        return matches;
    }

    /** Descarta el código de vuelta del lugar (el teléfono ya volvió); una invitación normal sigue igual. */
    public synchronized void forgetRejoin(String fightId, int seat) {
        var key = new Key(fightId, seat);
        var entry = tokens.get(key);
        if (entry != null && entry.rejoin) {
            tokens.remove(key);
        }
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

    /** El lugar libre de {@code code}; vacío si no existe o ya se usó. */
    public synchronized Optional<Seat> lookup(String code) {
        if (code == null) {
            return Optional.empty();
        }
        Seat found = null;
        // Se recorren todos y se compara en tiempo constante: el tiempo no dice si el código existe.
        for (var e : tokens.entrySet()) {
            if (equal(e.getValue().code, code) && !e.getValue().used) {
                found = new Seat(e.getKey().fightId(), e.getKey().seat());
            }
        }
        return Optional.ofNullable(found);
    }

    /** Descarta los tokens y códigos de la pelea (al cerrarse); sus códigos quedan libres. */
    public synchronized void forget(String fightId) {
        tokens.keySet().removeIf(k -> k.fightId().equals(fightId));
    }

    /** Descarta el token y el código de un solo lugar (al bajar los jueces); los demás lugares siguen igual. */
    public synchronized void forget(String fightId, int seat) {
        tokens.remove(new Key(fightId, seat));
    }

    private boolean codeTaken(String code) {
        return tokens.values().stream().anyMatch(e -> e.code.equals(code));
    }

    private static boolean equal(String expected, String given) {
        return given != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }
}
