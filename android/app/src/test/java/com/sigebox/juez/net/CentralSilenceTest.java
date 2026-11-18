package com.sigebox.juez.net;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sigebox.juez.data.Clock;

import org.junit.Test;

/** AND-032: la pregunta «¿La pelea sigue?» con reloj falso. */
public class CentralSilenceTest {

    private static final class FakeClock implements Clock {
        long now = 1_000_000;

        @Override
        public long now() {
            return now;
        }
    }

    private final FakeClock clock = new FakeClock();
    private final CentralSilence silence = new CentralSilence(clock);

    @Test
    public void preguntaTrasDiezMinutosSinMensajes() {
        clock.now += CentralSilence.ASK_AFTER_MS - 1;
        assertFalse("antes de 10 min no pregunta", silence.shouldAsk(false));
        clock.now += 1;
        assertTrue("a los 10 min sin mensajes pregunta", silence.shouldAsk(false));
    }

    @Test
    public void conRoundAbiertoNoPregunta() {
        clock.now += 3 * CentralSilence.ASK_AFTER_MS;
        assertFalse("con round abierto nunca pregunta", silence.shouldAsk(true));
        assertTrue(silence.shouldAsk(false));
    }

    @Test
    public void cualquierMensajeDeLaCentralReiniciaLaCuenta() {
        clock.now += CentralSilence.ASK_AFTER_MS - 1_000;
        silence.onCentralMessage();
        clock.now += CentralSilence.ASK_AFTER_MS - 1_000;
        assertFalse("el mensaje reinició la cuenta", silence.shouldAsk(false));
        clock.now += 1_000;
        assertTrue(silence.shouldAsk(false));
    }

    @Test
    public void seguirEsperandoReiniciaLaCuenta() {
        clock.now += CentralSilence.ASK_AFTER_MS;
        assertTrue(silence.shouldAsk(false));
        silence.keepWaiting();
        assertFalse("tras «Seguir esperando» no vuelve a preguntar enseguida", silence.shouldAsk(false));
        clock.now += CentralSilence.ASK_AFTER_MS;
        assertTrue("vuelve a preguntar tras otros 10 min", silence.shouldAsk(false));
    }
}
