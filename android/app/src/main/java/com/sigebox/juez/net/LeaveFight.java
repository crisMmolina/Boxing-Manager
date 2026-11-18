package com.sigebox.juez.net;

import java.util.function.IntSupplier;

/**
 * AND-032: «Salir de la pelea». Siempre desconecta. Sin tarjetas sin ACK borra la sesión y vuelve a Unirme; con
 * tarjetas pendientes no borra nada (ni sesión ni Outbox) para reenviarlas si el juez vuelve a unirse, y devuelve
 * cuántas son para que la pantalla lo diga.
 */
public final class LeaveFight {

    private final IntSupplier pendingCount;
    private final Runnable clearSession;
    private final Runnable stopConnection;
    private final Runnable goToJoin;

    public LeaveFight(IntSupplier pendingCount, Runnable clearSession, Runnable stopConnection, Runnable goToJoin) {
        this.pendingCount = pendingCount;
        this.clearSession = clearSession;
        this.stopConnection = stopConnection;
        this.goToJoin = goToJoin;
    }

    /** @return las tarjetas sin ACK que se quedan guardadas (0 si salió del todo). */
    public int leave() {
        int pending = pendingCount.getAsInt();
        stopConnection.run();
        if (pending == 0) {
            clearSession.run();
        }
        goToJoin.run();
        return pending;
    }
}
