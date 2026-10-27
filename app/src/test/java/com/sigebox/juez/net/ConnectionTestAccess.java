package com.sigebox.juez.net;

import com.sigebox.core.protocol.Message;
import com.sigebox.juez.data.Clock;
import com.sigebox.juez.data.Outbox;
import com.sigebox.juez.data.SessionStore;
import com.sigebox.juez.data.SigeboxDatabase;

import okhttp3.OkHttpClient;

/** Puente de pruebas para entregar un mensaje como si llegara de la central (dispatch es del paquete). */
public final class ConnectionTestAccess {
    private ConnectionTestAccess() {}

    /** Conexión sin red (nunca se llama a start) con una llave en memoria. */
    public static JudgeConnection newConnection(SigeboxDatabase db) throws Exception {
        return new JudgeConnection(new OkHttpClient(), new Outbox(db.outboxDao(), Clock.SYSTEM),
                new SessionStore(db.sessionDao()), new JudgeSessionTest.TestSigner(), Clock.SYSTEM,
                new Backoff(50, 200), 60_000);
    }

    public static void dispatch(JudgeConnection connection, Message message) {
        connection.dispatch(message);
    }
}
