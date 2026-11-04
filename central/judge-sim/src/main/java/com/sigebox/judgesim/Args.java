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
 * judge-sim --host 192.168.43.1 --script demo_data/pelea_referencia.json --codes 482913,105377,660214
 * </pre>
 *
 * Los tokens son los de cada lugar que muestra la central junto a su QR, en orden de lugar. Con
 * {@code --codes} basta el código de 6 dígitos de cada lugar: un juez por código y sin {@code --fight}.
 */
public record Args(
        String host, int port, int judges, Path script, String fightId, List<String> tokens, Duration drop,
        Duration timeout, List<String> codes) {

    public static final String USAGE = "uso: judge-sim --judges 1..5 --script ARCHIVO --tokens T1,T2,… "
            + "[--host localhost] [--port 8765] [--fight ID] [--drop 60s] [--timeout 30m]"
            + " | --script ARCHIVO --codes C1,C2,… [--host localhost] [--port 8765] [--drop 60s] [--timeout 30m]";

    private static final Pattern CODE = Pattern.compile("\\d{6}");

    private static final Pattern DURATION = Pattern.compile("(\\d+)(ms|s|m)");

    public static Args parse(String... argv) {
        String host = "localhost";
        int port = 8765;
        Integer judges = null;
        Path script = null;
        String fightId = null;
        List<String> tokens = List.of();
        List<String> codes = List.of();
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
                case "--codes" -> codes = Arrays.stream(value.split(","))
                        .map(String::strip).filter(t -> !t.isEmpty()).toList();
                case "--drop" -> drop = duration(flag, value);
                case "--timeout" -> timeout = duration(flag, value);
                default -> throw new IllegalArgumentException("opción desconocida: " + flag);
            }
        }
        if (!codes.isEmpty()) {
            if (!tokens.isEmpty() || fightId != null) {
                throw new IllegalArgumentException("--codes no se mezcla con --tokens ni --fight");
            }
            if (codes.size() > 5 || codes.stream().anyMatch(c -> !CODE.matcher(c).matches())) {
                throw new IllegalArgumentException("--codes espera de 1 a 5 códigos de 6 dígitos");
            }
            if (judges != null && judges != codes.size()) {
                throw new IllegalArgumentException("--codes debe traer un código por juez (" + judges + ")");
            }
            judges = codes.size();
        }
        if (judges == null) {
            throw new IllegalArgumentException("falta --judges");
        }
        if (script == null) {
            throw new IllegalArgumentException("falta --script");
        }
        if (codes.isEmpty() && tokens.size() != judges) {
            throw new IllegalArgumentException("--tokens debe traer un token por juez (" + judges + ")");
        }
        return new Args(host, port, judges, script, fightId, List.copyOf(tokens), drop, timeout, codes);
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
