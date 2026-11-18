package com.sigebox.central.ui.result;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * DSK-053: en macOS una ventana con dueño a pantalla completa congela la interfaz (JavaFX entra a
 * {@code _enterFullscreen} y no vuelve); ahí el resultado se abre maximizado. En Windows sigue a pantalla completa.
 */
class ResultWindowModeTest {

    @Test
    void enMacOsElResultadoSeAbreMaximizadoNoAPantallaCompleta() {
        assertEquals(ResultScreen.Mode.MAXIMIZADA, ResultScreen.modeFor("Mac OS X"));
    }

    @Test
    void enWindowsElResultadoSigueAPantallaCompleta() {
        assertEquals(ResultScreen.Mode.PANTALLA_COMPLETA, ResultScreen.modeFor("Windows 11"));
    }
}
