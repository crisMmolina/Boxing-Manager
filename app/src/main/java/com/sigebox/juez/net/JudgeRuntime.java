package com.sigebox.juez.net;

import android.content.Context;
import android.net.ConnectivityManager;

import com.sigebox.juez.data.Clock;
import com.sigebox.juez.data.Outbox;
import com.sigebox.juez.data.SessionStore;
import com.sigebox.juez.data.SigeboxDatabase;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Punto de entrada único para las pantallas: {@code JudgeRuntime.get(context).connection()}. La primera llamada abre
 * la base, ata el proceso al Wi‑Fi y reconecta al lugar guardado (recuperación tras un cierre forzado).
 */
public final class JudgeRuntime {
    /** Latido: PING y reenvío de lo que no tiene ACK. */
    static final long TICK_MS = 5_000;

    private static volatile JudgeRuntime instance;

    private final SigeboxDatabase database;
    private final JudgeConnection connection;
    private final NetworkBinder networkBinder;

    private JudgeRuntime(Context app) {
        database = SigeboxDatabase.create(app);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        connection = new JudgeConnection(client, new Outbox(database.outboxDao(), Clock.SYSTEM),
                new SessionStore(database.sessionDao()), new KeystoreSigner(), Clock.SYSTEM,
                new Backoff(500, 5_000), TICK_MS);
        networkBinder = new NetworkBinder(app.getSystemService(ConnectivityManager.class));
    }

    public static JudgeRuntime get(Context context) {
        JudgeRuntime local = instance;
        if (local == null) {
            synchronized (JudgeRuntime.class) {
                local = instance;
                if (local == null) {
                    local = new JudgeRuntime(context.getApplicationContext());
                    local.networkBinder.start();
                    local.connection.start();
                    instance = local;
                }
            }
        }
        return local;
    }

    public JudgeConnection connection() {
        return connection;
    }

    public SigeboxDatabase database() {
        return database;
    }
}
