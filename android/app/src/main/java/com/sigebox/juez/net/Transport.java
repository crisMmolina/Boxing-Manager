package com.sigebox.juez.net;

/** Un socket abierto hacia la central (OkHttp en la app, un doble en las pruebas). */
public interface Transport {
    /** @return {@code false} si el socket ya no acepta mensajes */
    boolean send(String text);

    void close();
}
