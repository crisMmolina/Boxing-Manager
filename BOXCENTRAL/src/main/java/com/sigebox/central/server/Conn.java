package com.sigebox.central.server;

/** Una conexión WebSocket vista por {@link JudgeHub}; en pruebas se reemplaza por una falsa. */
public interface Conn {

    /** Política violada (firma, nonce, mensaje antes de autenticarse, tiempo agotado). */
    int POLICY = 1008;

    /** Mensaje demasiado grande. */
    int TOO_BIG = 1009;

    String id();

    void send(String text);

    void close(int code, String reason);
}
