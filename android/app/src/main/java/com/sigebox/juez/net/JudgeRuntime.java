package com.sigebox.juez.net;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.NextFight;
import com.sigebox.juez.data.Clock;
import com.sigebox.juez.data.Outbox;
import com.sigebox.juez.data.SessionStore;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.finalcard.AutoFinalCard;
import com.sigebox.juez.wait.FightEndFlow;
import com.sigebox.juez.wait.NextFightActivity;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Punto de entrada único para las pantallas: {@code JudgeRuntime.get(context).connection()}. La primera llamada abre
 * la base, ata el proceso al Wi‑Fi y reconecta al lugar guardado (recuperación tras un cierre forzado).
 */
public final class JudgeRuntime {
    /** Latido: PING y reenvío de lo que no tiene ACK. */
    static final long TICK_MS = 5_000;
    /** La pantalla de puntuación se abre por nombre, como hace la espera de aprobación. */
    static final String SCORE_ACTIVITY = "com.sigebox.juez.score.ScoreActivity";

    private static volatile JudgeRuntime instance;

    private final SigeboxDatabase database;
    private final JudgeConnection connection;
    private final NetworkBinder networkBinder;

    private JudgeRuntime(Context app) {
        this(app, SigeboxDatabase.create(app), new KeystoreSigner());
    }

    /** Las pruebas pasan base en memoria y llave de software: Robolectric no tiene AndroidKeyStore. */
    JudgeRuntime(Context app, SigeboxDatabase database, DeviceSigner signer) {
        this.database = database;
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        Outbox outbox = new Outbox(database.outboxDao(), Clock.SYSTEM);
        SessionStore sessions = new SessionStore(database.sessionDao());
        connection = new JudgeConnection(client, outbox, sessions, signer, Clock.SYSTEM,
                new Backoff(500, 5_000), TICK_MS);
        networkBinder = new NetworkBinder(app.getSystemService(ConnectivityManager.class));
        // NEXT_FIGHT lleva al juez a la pantalla de puntuación desde donde esté, sin confirmación (spec 002).
        connection.addListener(new JudgeListener() {
            @Override
            public void onMessage(Message message) {
                if (message instanceof NextFight) {
                    app.startActivity(new Intent().setClassName(app, SCORE_ACTIVITY)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                }
            }
        });
        // Antes que el cierre de pelea: un ACK primero le avisa a la tarjeta final. Sale sola en cualquier pantalla.
        AutoFinalCard.install(this);
        connection.addListener(new FightEndFlow(sessions::load, sessions::clear, outbox::pendingCount,
                connection::stop, NextFightActivity.screens(app)));
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
