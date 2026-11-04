package com.sigebox.juez;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.join.JoinActivity;
import com.sigebox.juez.join.WaitingActivity;
import com.sigebox.juez.net.JudgeRuntime;
import com.sigebox.juez.score.ScoreActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Punto de entrada. Arranca la conexión (tras un cierre forzado reconecta con {@code HELLO} y reenvía la bandeja de
 * salida) y vuelve a la pantalla del lugar guardado: puntuación, espera de aprobación o unión.
 */
public class MainActivity extends AppCompatActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sin contenido propio: se ve el tema de arranque (logo sobre bg) hasta que abre la primera pantalla.
        JudgeRuntime runtime = JudgeRuntime.get(this);
        io.execute(() -> {
            SessionEntity session = runtime.database().sessionDao().load();
            runOnUiThread(() -> {
                Intent next = new Intent(this, routeFor(session));
                if (session != null && !session.approved) {
                    next.putExtra(WaitingActivity.EXTRA_SEAT, session.seat)
                            .putExtra(WaitingActivity.EXTRA_NAME, session.judgeName);
                }
                startActivity(next);
                finish();
            });
        });
    }

    @Override
    protected void onDestroy() {
        io.shutdown();
        super.onDestroy();
    }

    /** Lugar aprobado → puntuación; pendiente → espera; sin lugar → unión (que pide los términos si faltan). */
    static Class<? extends Activity> routeFor(SessionEntity session) {
        if (session == null) {
            return JoinActivity.class;
        }
        return session.approved ? ScoreActivity.class : WaitingActivity.class;
    }
}
