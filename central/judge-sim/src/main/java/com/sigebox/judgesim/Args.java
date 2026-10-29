package com.sigebox.judgesim;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Argumentos de la línea de comandos.
 *
 * <pre>
 * judge-sim --host 192.168.43.1 --port 8765 --judges 3 --script demo_data/pelea_referencia.json \
 *           --tokens TOKEN1,TOKEN2,TOKEN3 [--fight ID] [--drop 60s] [--timeout 30m]
 * </pre>
 *
 * Los tokens son los de cada lugar que muestra la central junto a su QR, en orden de lugar.
 */
public record Args(
        String host, int port, int judges, Path script, String fightId, List<String> tokens, Duration drop,
        Duration timeout) {

    public static final String USAGE = "uso: judge-sim --judges 1..5 --script ARCHIVO --tokens T1,T2,… "
            + "[--host localhost] [--port 8765] [--fight ID] [--drop 60s] [--timeout 30m]";

    private static final Pattern DURATION = Pattern.compile("(\\d+)(ms|s|m)");

    public static Args parse(String... argv) {
        String host = "localhost";
        int port = 8765;
        Integer judges = null;
        Path script = null;
        String fightId = null;
        List<String> tokens = List.of();
        Duration drop = Duration.ZERO;
        Duration timeout = Duration.ofMinutes(30);
        for (int i = 0; i < argv.length; i++) {
            var flag = argv[i];
            if (i + 1 >= argv.length) {
                throw new IllegalArgumentException("falta el valor de " + flag);
            }
            var value = argv[++i];
            switch (flag) {
                case "--host" -> host = value;
                case "--port" -> port = integer(flag, value, 1, 65535);
                case "--judges" -> judges = integer(flag, value, 1, 5);
                case "--script" -> script = Path.of(value);
                case "--fight" -> fightId = value;
                case "--tokens" -> tokens = new ArrayList<>(Arrays.stream(value.split(","))
                        .map(String::strip).filter(t -> !t.isEmpty()).toList());
                case "--drop" -> drop = duration(flag, value);
                case "--timeout" -> timeout = duration(flag, value);
                default -> throw new IllegalArgumentException("opción desconocida: " + flag);
            }
        }
        if (judges == null) {
            throw new IllegalArgumentException("falta --judges");
        }
        if (script == null) {
            throw new IllegalArgumentException("falta --script");
        }
        if (tokens.size() != judges) {
            throw new IllegalArgumentException("--tokens debe traer un token por juez (" + judges + ")");
        }
        return new Args(host, port, judges, script, fightId, List.copyOf(tokens), drop, timeout);
    }

    static Duration duration(String flag, String value) {
        var m = DURATION.matcher(value.strip());
        if (!m.matches()) {
            throw new IllegalArgumentException(flag + " espera algo como 60s, 500ms o 2m");
        }
        long n = Long.parseLong(m.group(1));
        return switch (m.group(2)) {
            case "ms" -> Duration.ofMillis(n);
            case "s" -> Duration.ofSeconds(n);
            default -> Duration.ofMinutes(n);
        };
    }

    private static int integer(String flag, String value, int min, int max) {
        int n;
        try {
            n = Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(flag + " debe ser un número", e);
        }
        if (n < min || n > max) {
            throw new IllegalArgumentException(flag + " debe estar entre " + min + " y " + max);
        }
        return n;
    }
}
