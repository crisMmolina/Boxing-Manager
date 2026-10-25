package com.sigebox.core.validation;

import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.TarjetaRound;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.SubmitRound;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Valida calificaciones en el teléfono y otra vez en la central (principio 2): solo enteros 0–10 por esquina, sin
 * valor por defecto, y solo del juez, la pelea y un round de la sesión. Devuelve el código de rechazo del {@code ACK}.
 */
public final class ScoreValidator {

    public static final int MIN = 0;
    public static final int MAX = 10;

    private static final Pattern VALOR = Pattern.compile("^(10|[0-9])$");

    private ScoreValidator() {}

    /**
     * La sesión aprobada de un juez: de aquí salen la pelea y el lugar, nunca del mensaje.
     *
     * @param seat 1–5
     * @param rounds rounds programados, 1–12
     */
    public record Sesion(String fightId, int seat, int rounds) {
        public Sesion {
            if (fightId == null || fightId.isEmpty()) {
                throw new IllegalArgumentException("falta fightId");
            }
            if (seat < 1 || seat > 5) {
                throw new IllegalArgumentException("lugar fuera de rango");
            }
            if (rounds < 1 || rounds > 12) {
                throw new IllegalArgumentException("rounds fuera de rango");
            }
        }
    }

    /** Lee lo que el juez teclea: exactamente {@code 0}…{@code 10} en dígitos ASCII; cualquier otra cosa es vacío. */
    public static Optional<Integer> parse(String texto) {
        if (texto == null || !VALOR.matcher(texto).matches()) {
            return Optional.empty();
        }
        return Optional.of(Integer.valueOf(texto));
    }

    /** Ambos valores presentes y en 0–10. */
    public static Optional<Ack.Error> validarValores(Integer rojo, Integer azul) {
        if (!enRango(rojo) || !enRango(azul)) {
            return Optional.of(Ack.Error.INVALID_SCORE);
        }
        return Optional.empty();
    }

    /** Un {@code SUBMIT_ROUND} recibido en la conexión de {@code sesion}. */
    public static Optional<Ack.Error> validar(Sesion sesion, SubmitRound msg) {
        if (!sesion.fightId().equals(msg.fightId()) || sesion.seat() != msg.seat()
                || msg.round() < 1 || msg.round() > sesion.rounds()) {
            return Optional.of(Ack.Error.WRONG_SESSION);
        }
        return validarValores(msg.red(), msg.blue());
    }

    /** Una tarjeta (del juez o capturada por el organizador) de {@code pelea}; sin estado es inválida. */
    public static Optional<Ack.Error> validar(Pelea pelea, TarjetaRound tarjeta) {
        if (!pelea.id().equals(tarjeta.peleaId()) || tarjeta.juez() < 1 || tarjeta.juez() > pelea.jueces()
                || tarjeta.round() < 1 || tarjeta.round() > pelea.rounds()) {
            return Optional.of(Ack.Error.WRONG_SESSION);
        }
        if (tarjeta.estado() == null) {
            return Optional.of(Ack.Error.INVALID_SCORE);
        }
        return validarValores(tarjeta.rojo(), tarjeta.azul());
    }

    private static boolean enRango(Integer valor) {
        return valor != null && valor >= MIN && valor <= MAX;
    }
}
