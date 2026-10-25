package com.sigebox.core.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.TarjetaRound;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.SubmitRound;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ScoreValidatorTest {

    private static final Pelea PELEA = new Pelea("f-1", "Roja", "Azul", 4, 3, EstadoPelea.EN_CURSO, "2026-10-24");
    private static final ScoreValidator.Sesion SESION = new ScoreValidator.Sesion("f-1", 2, 4);

    @ParameterizedTest
    @ValueSource(strings = {"0", "7", "10"})
    void aceptaEnterosDeCeroADiez(String valor) {
        assertEquals(Optional.of(Integer.parseInt(valor)), ScoreValidator.parse(valor));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "11", "-1", "+5", "07", " 7", "7 ", "9.5", "diez", "1e1", "٣"})
    void rechazaTodoLoDemasSinValorPorDefecto(String valor) {
        assertEquals(Optional.empty(), ScoreValidator.parse(valor));
    }

    @Test
    void valoresNulosOFueraDeRangoSonInvalidos() {
        assertEquals(Optional.of(Ack.Error.INVALID_SCORE), ScoreValidator.validarValores(null, 9));
        assertEquals(Optional.of(Ack.Error.INVALID_SCORE), ScoreValidator.validarValores(10, null));
        assertEquals(Optional.of(Ack.Error.INVALID_SCORE), ScoreValidator.validarValores(11, 9));
        assertEquals(Optional.of(Ack.Error.INVALID_SCORE), ScoreValidator.validarValores(10, -1));
        assertEquals(Optional.empty(), ScoreValidator.validarValores(0, 10));
    }

    @Test
    void submitRoundDeLaSesionEsValido() {
        assertEquals(Optional.empty(), ScoreValidator.validar(SESION, new SubmitRound("f-1", 2, 4, 10, 9)));
    }

    @Test
    void rechazaJuezPeleaORoundAjenosALaSesion() {
        assertEquals(Optional.of(Ack.Error.WRONG_SESSION), ScoreValidator.validar(SESION, new SubmitRound("f-1", 3, 1, 10, 9)));
        assertEquals(Optional.of(Ack.Error.WRONG_SESSION), ScoreValidator.validar(SESION, new SubmitRound("f-2", 2, 1, 10, 9)));
        assertEquals(Optional.of(Ack.Error.WRONG_SESSION), ScoreValidator.validar(SESION, new SubmitRound("f-1", 2, 5, 10, 9)));
    }

    @Test
    void validaTarjetaContraLaPelea() {
        assertTrue(ScoreValidator.validar(PELEA, tarjeta("f-1", 3, 4, 10, 9)).isEmpty());
        assertEquals(Optional.of(Ack.Error.WRONG_SESSION), ScoreValidator.validar(PELEA, tarjeta("f-9", 1, 1, 10, 9)));
        assertEquals(Optional.of(Ack.Error.WRONG_SESSION), ScoreValidator.validar(PELEA, tarjeta("f-1", 4, 1, 10, 9)));
        assertEquals(Optional.of(Ack.Error.WRONG_SESSION), ScoreValidator.validar(PELEA, tarjeta("f-1", 0, 1, 10, 9)));
        assertEquals(Optional.of(Ack.Error.WRONG_SESSION), ScoreValidator.validar(PELEA, tarjeta("f-1", 1, 5, 10, 9)));
        assertEquals(Optional.of(Ack.Error.WRONG_SESSION), ScoreValidator.validar(PELEA, tarjeta("f-1", 1, 0, 10, 9)));
        assertEquals(Optional.of(Ack.Error.INVALID_SCORE), ScoreValidator.validar(PELEA, tarjeta("f-1", 1, 1, 10, 11)));
        assertFalse(ScoreValidator.validar(PELEA, tarjeta("f-1", 1, 1, -1, 9)).isEmpty());
    }

    @Test
    void sesionFueraDeRangoEsUnError() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ScoreValidator.Sesion("f", 6, 4));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ScoreValidator.Sesion("f", 1, 13));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ScoreValidator.Sesion(null, 1, 4));
    }

    static TarjetaRound tarjeta(String pelea, int juez, int round, int rojo, int azul) {
        return new TarjetaRound(pelea, juez, round, rojo, azul, EstadoTarjeta.ENVIADA, OrigenTarjeta.JUEZ, 0L);
    }
}
