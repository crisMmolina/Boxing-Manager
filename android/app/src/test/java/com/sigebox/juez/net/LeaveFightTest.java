package com.sigebox.juez.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

/** AND-032: «Salir de la pelea» con y sin tarjetas sin ACK. */
public class LeaveFightTest {

    private final AtomicBoolean cleared = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final AtomicBoolean wentToJoin = new AtomicBoolean();

    private LeaveFight leaveWith(int pending) {
        return new LeaveFight(() -> pending, () -> cleared.set(true), () -> stopped.set(true),
                () -> wentToJoin.set(true));
    }

    @Test
    public void salirSinPendientesDesconectaBorraLaSesionYVuelveAUnirme() {
        int left = leaveWith(0).leave();

        assertEquals(0, left);
        assertTrue("desconecta", stopped.get());
        assertTrue("borra la sesión", cleared.get());
        assertTrue("vuelve a Unirme", wentToJoin.get());
    }

    @Test
    public void salirConPendientesDiceCuantasYConservaSesionYOutbox() {
        int left = leaveWith(2).leave();

        assertEquals("dice cuántas tarjetas siguen sin enviar", 2, left);
        assertTrue("desconecta", stopped.get());
        assertFalse("no borra la sesión: hace falta para reenviar al volver", cleared.get());
        assertTrue("vuelve a Unirme para poder reunirse", wentToJoin.get());
    }
}
