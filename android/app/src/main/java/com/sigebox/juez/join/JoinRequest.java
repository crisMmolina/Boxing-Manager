package com.sigebox.juez.join;

import com.sigebox.core.protocol.JoinUri;

import java.util.regex.Pattern;

import okhttp3.HttpUrl;

/**
 * Lo que el juez llenó para unirse, ya validado: lugar (del QR, del código de lugar o de la forma manual vieja) y
 * nombre.
 */
public final class JoinRequest {
    public static final int DEFAULT_PORT = 8765;
    static final int MAX_NAME = 80;
    public static final int CODE_LENGTH = 6;
    private static final Pattern CODE = Pattern.compile("\\d{" + CODE_LENGTH + "}");

    public enum Error {
        NO_SEAT, NAME_EMPTY, NAME_TOO_LONG, HOST_INVALID, FIGHT_INVALID, SEAT_INVALID, CODE_INVALID
    }

    /** {@code request} o {@code error}, nunca ambos. */
    public static final class Result {
        public final JoinRequest request;
        public final Error error;

        private Result(JoinRequest request, Error error) {
            this.request = request;
            this.error = error;
        }
    }

    public final JoinUri uri;
    public final String judgeName;

    private JoinRequest(JoinUri uri, String judgeName) {
        this.uri = uri;
        this.judgeName = judgeName;
    }

    public static Result fromQr(JoinUri uri, String rawName) {
        if (uri == null) {
            return fail(Error.NO_SEAT);
        }
        if (!isUrlHost(uri.host(), uri.port())) {
            return fail(Error.HOST_INVALID);
        }
        String name = cleanName(rawName);
        Error nameError = nameError(name);
        return nameError != null ? fail(nameError) : new Result(new JoinRequest(uri, name), null);
    }

    /**
     * Nombre, código de lugar (exactamente 6 dígitos) e IP de la central con {@code :puerto} opcional (ADR 0004): la
     * pelea y el lugar los dice la central con {@code CODE_INFO}.
     */
    public static Result withCode(String rawHost, String rawCode, String rawName) {
        return withCode(rawHost, rawCode, rawName, null);
    }

    /** Igual, con la huella de la central que traía el QR ({@code null} si se escribió a mano). */
    public static Result withCode(String rawHost, String rawCode, String rawName, String certFingerprint) {
        String[] hostPort = hostPort(rawHost);
        if (hostPort == null) {
            return fail(Error.HOST_INVALID);
        }
        String code = rawCode == null ? "" : rawCode.trim();
        if (!CODE.matcher(code).matches()) {
            return fail(Error.CODE_INVALID);
        }
        JoinUri uri;
        try {
            uri = new JoinUri(hostPort[0], Integer.parseInt(hostPort[1]), code, certFingerprint);
        } catch (IllegalArgumentException e) {
            return fail(Error.HOST_INVALID);
        }
        return fromQr(uri, rawName);
    }

    /** Forma anterior: IP de la central (con {@code :puerto} opcional), pelea, lugar y código de 6 dígitos. */
    public static Result manual(String rawHost, String rawFight, String rawSeat, String rawCode, String rawName) {
        String[] hostPort = hostPort(rawHost);
        if (hostPort == null) {
            return fail(Error.HOST_INVALID);
        }
        String host = hostPort[0];
        int port = Integer.parseInt(hostPort[1]);
        String fight = rawFight == null ? "" : rawFight.trim();
        if (fight.isEmpty() || fight.length() > 64) {
            return fail(Error.FIGHT_INVALID);
        }
        int seat = parseInt(rawSeat == null ? "" : rawSeat.trim(), -1);
        if (seat < 1 || seat > 5) {
            return fail(Error.SEAT_INVALID);
        }
        String code = rawCode == null ? "" : rawCode.replaceAll("\\s", "");
        if (!CODE.matcher(code).matches()) {
            return fail(Error.CODE_INVALID);
        }
        JoinUri uri;
        try {
            uri = new JoinUri(host, port, fight, seat, code, null);
        } catch (IllegalArgumentException e) {
            return fail(Error.HOST_INVALID);
        }
        return fromQr(uri, rawName);
    }

    /** {@code {host, puerto}} o {@code null} si la IP no sirve. */
    private static String[] hostPort(String raw) {
        String hostPort = raw == null ? "" : raw.trim();
        String host = hostPort;
        int port = DEFAULT_PORT;
        int colon = hostPort.lastIndexOf(':');
        if (colon > 0 && hostPort.indexOf(':') == colon) {
            host = hostPort.substring(0, colon);
            port = parseInt(hostPort.substring(colon + 1), -1);
        }
        if (host.isEmpty() || host.contains(" ") || port < 1 || port > 65535 || !isUrlHost(host, port)) {
            return null;
        }
        return new String[] {host, String.valueOf(port)};
    }

    /** La conexión arma una URL con el host: lo que no se vuelve URL truena allá, no aquí. */
    private static boolean isUrlHost(String host, int port) {
        String bracketed = host.contains(":") && !host.startsWith("[") ? "[" + host + "]" : host;
        return HttpUrl.parse("http://" + bracketed + ":" + port + "/") != null;
    }

    private static String cleanName(String raw) {
        return raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
    }

    private static Error nameError(String name) {
        if (name.isEmpty()) {
            return Error.NAME_EMPTY;
        }
        return name.length() > MAX_NAME ? Error.NAME_TOO_LONG : null;
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Result fail(Error error) {
        return new Result(null, error);
    }
}
