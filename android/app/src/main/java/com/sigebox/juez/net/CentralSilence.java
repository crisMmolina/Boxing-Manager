package com.sigebox.juez.net;

import com.sigebox.juez.data.Clock;

/**
 * AND-032: cuenta el tiempo sin mensajes de la central. A los 10 min sin nada, y sin round abierto, la app pregunta
 * «¿La pelea sigue?». Cualquier mensaje de la central, o «Seguir esperando», reinicia la cuenta.
 */
public final class CentralSilence {

    public static final long ASK_AFTER_MS = 10 * 60_000L;

    private final Clock clock;
    private volatile long lastContact;

    public CentralSilence(Clock clock) {
        this.clock = clock;
        this.lastContact = clock.now();
    }

    public void onCentralMessage() {
        lastContact = clock.now();
    }

    public void keepWaiting() {
        lastContact = clock.now();
    }

    public boolean shouldAsk(boolean roundOpen) {
        return !roundOpen && clock.now() - lastContact >= ASK_AFTER_MS;
    }
}
