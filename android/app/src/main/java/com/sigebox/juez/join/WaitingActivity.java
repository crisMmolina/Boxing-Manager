package com.sigebox.juez.join;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sigebox.juez.R;
import com.sigebox.juez.net.ConnectionState;
import com.sigebox.juez.net.JudgeRuntime;
import com.sigebox.juez.net.KeystoreSigner;

import java.util.Locale;

/** "Esperando aprobación" hasta {@code JOIN_APPROVED} (→ puntuación) o {@code JOIN_REJECTED} (→ motivo). */
public class WaitingActivity extends AppCompatActivity {
    public static final String EXTRA_SEAT = "seat";
    public static final String EXTRA_NAME = "name";
    /** La pantalla de puntuación la crea AND-005; se abre por nombre para no depender de ella. */
    static final String SCORE_ACTIVITY = "com.sigebox.juez.score.ScoreActivity";

    private com.sigebox.juez.net.JudgeConnection connection;
    private com.sigebox.juez.ui.CornerNames cornerNames;
    /** JOIN_APPROVED trae los nombres de los boxeadores; se guardan para la pantalla de puntuación (DESIGN v3 §5.2). */
    private final com.sigebox.juez.net.JudgeListener namesListener = new com.sigebox.juez.net.JudgeListener() {
        @Override
        public void onMessage(com.sigebox.core.protocol.Message message) {
            cornerNames.remember(message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);
        com.sigebox.juez.ui.Screen.setup(this);
        TextView seat = findViewById(R.id.waiting_seat);
        seat.setText(getString(R.string.waiting_seat,
                getIntent().getIntExtra(EXTRA_SEAT, 0), getIntent().getStringExtra(EXTRA_NAME)));
        TextView fingerprint = findViewById(R.id.waiting_fingerprint);
        fingerprint.setText(getString(R.string.waiting_fingerprint, shortFingerprint(new KeystoreSigner().deviceId())));
        Button cancel = findViewById(R.id.waiting_cancel);
        cancel.setOnClickListener(v -> finish());
        connection = JudgeRuntime.get(this).connection();
        cornerNames = new com.sigebox.juez.ui.CornerNames(this);
        connection.state().observe(this, this::render);
    }

    @Override
    protected void onStart() {
        super.onStart();
        connection.addListener(namesListener);
    }

    @Override
    protected void onStop() {
        connection.removeListener(namesListener);
        super.onStop();
    }

    private void render(ConnectionState state) {
        TextView status = findViewById(R.id.waiting_status);
        status.setText(WaitingText.forState(state));
        boolean rejected = state.status == ConnectionState.Status.REJECTED;
        findViewById(R.id.waiting_body).setVisibility(rejected ? View.GONE : View.VISIBLE);
        Button cancel = findViewById(R.id.waiting_cancel);
        cancel.setText(rejected ? R.string.waiting_back : R.string.waiting_cancel);
        if (state.status == ConnectionState.Status.CONNECTED) {
            startActivity(new Intent().setClassName(this, SCORE_ACTIVITY)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }
    }

    /** Los primeros 8 caracteres en dos grupos ("AB12 CD34"): lo que el organizador compara en la central. */
    static String shortFingerprint(String fingerprint) {
        String upper = fingerprint.toUpperCase(Locale.ROOT);
        return upper.substring(0, 4) + " " + upper.substring(4, 8);
    }
}
