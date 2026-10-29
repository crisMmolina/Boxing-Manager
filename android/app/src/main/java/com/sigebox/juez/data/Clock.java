package com.sigebox.juez.data;

/** Reloj inyectable (milisegundos) para que las pruebas no dependan del tiempo real. */
public interface Clock {
    Clock SYSTEM = System::currentTimeMillis;

    long now();
}
