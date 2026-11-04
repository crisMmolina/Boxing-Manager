package com.sigebox.juez.late;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.sigebox.core.protocol.Message;
import com.sigebox.juez.R;
import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.net.JudgeConnection;
import com.sigebox.juez.net.JudgeListener;
import com.sigebox.juez.net.JudgeRuntime;
import com.sigebox.juez.score.ScoreActivity;
import com.sigebox.juez.score.ScoreUi;
import com.sigebox.juez.score.ScoreViewModel;
import com.sigebox.juez.ui.ConfirmSheet;
import com.sigebox.juez.ui.CornerCard;
import com.sigebox.juez.ui.ErrorText;
import com.sigebox.juez.ui.Screen;
import com.sigebox.juez.ui.TopBar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * La central cerró un round sin la calificación de este juez: vibra y exige calificarlo ya, a pantalla completa
 * (DESIGN.md §6.5). "Atrás" no la cierra hasta enviar; el envío pasa por la bandeja de salida y la central lo marca
 * tardío.
 */
public class LateScoreActivity extends AppCompatActivity {
    private static final long VIBRATION_MS = 1_200;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final LateGate gate = new LateGate();
    private ScoreViewModel vm;
    private JudgeConnection connection;
    private SigeboxDatabase database;
    private int round;
    private CornerCard red;
    private CornerCard blue;
    private boolean defaulted;

    private com.sigebox.juez.ui.CornerNames cornerNames;
    private volatile String currentFightId;

    private final JudgeListener listener = new JudgeListener() {
        @Override
        public void onMessage(Message message) {
            if (cornerNames.remember(message)) {
                runOnUiThread(() -> showNames(currentFightId));
            }
            runOnUiThread(() -> vm.onMessage(message));
        }
    };

    private final OnBackPressedCallback blockBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            // Bloqueado hasta enviar: no hacer nada.
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_late_score);
        Screen.setup(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getOnBackPressedDispatcher().addCallback(this, blockBack);
        round = getIntent().getIntExtra(ScoreActivity.EXTRA_ROUND, 0);
        if (round < 1) {
            finish();
            return;
        }
        JudgeRuntime runtime = JudgeRuntime.get(this);
        connection = runtime.connection();
        database = runtime.database();
        vm = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                ScoreViewModel model = new ScoreViewModel(connection::submit);
                model.openLate(round);
                return (T) model;
            }
        }).get(ScoreViewModel.class);

        ((TextView) findViewById(R.id.late_title)).setText(getString(R.string.late_title, round));
        red = new CornerCard(findViewById(R.id.late_red_card), true, d -> vm.adjust(ScoreUi.Corner.RED, d));
        blue = new CornerCard(findViewById(R.id.late_blue_card), false, d -> vm.adjust(ScoreUi.Corner.BLUE, d));
        cornerNames = new com.sigebox.juez.ui.CornerNames(this);
        findViewById(R.id.late_send).setOnClickListener(v -> confirm());
        vm.ui().observe(this, this::render);
        // El punto de conexión de la barra (DESIGN v3 §5); en modo tardío el round no cambia con esto.
        connection.state().observe(this, vm::onConnection);
        if (savedInstanceState == null && gate.shouldVibrate()) {
            vibrate();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (connection == null) {
            return;
        }
        connection.addListener(listener);
        io.execute(() -> {
            SessionEntity session = database.sessionDao().load();
            if (session != null) {
                runOnUiThread(() -> {
                    currentFightId = session.fightId;
                    showNames(session.fightId);
                    vm.setSession(session.fightId, session.seat, session.judgeName);
                });
            }
        });
    }

    @Override
    protected void onStop() {
        if (connection != null) {
            connection.removeListener(listener);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        io.shutdown();
        super.onDestroy();
    }

    private void render(ScoreUi ui) {
        boolean online = ui.connection == com.sigebox.juez.net.ConnectionState.Status.CONNECTED;
        TopBar.bind(findViewById(R.id.late_root), online,
                ui.seat > 0 ? getString(R.string.score_judge, ui.seat) : getString(R.string.status_disconnected));
        boolean input = ui.inputEnabled();
        if (input && !defaulted) {
            defaulted = true;
            if (vm.applyDefault()) {
                return; // empieza en 10–10; el ViewModel vuelve a publicar
            }
        }
        Integer r = ui.card != null ? ui.card.red : ui.red;
        Integer b = ui.card != null ? ui.card.blue : ui.blue;
        red.bind(r, input);
        blue.bind(b, input);
        TextView latePill = findViewById(R.id.late_status);
        if (ui.card == null) {
            latePill.setText(getString(R.string.late_status, round)); // "Round 3 cerrado" (DESIGN v3 §6.5)
        } else {
            latePill.setText(ui.card.status == ScoreUi.CardStatus.SENDING ? R.string.status_sending : R.string.status_late);
        }
        findViewById(R.id.late_send).setEnabled(ui.sendEnabled);

        TextView message = findViewById(R.id.late_message);
        if (ui.lastError != null) {
            message.setText(ErrorText.of(ui.lastError));
            message.setVisibility(View.VISIBLE);
        } else if (ui.card != null) {
            message.setText(ui.card.status == ScoreUi.CardStatus.SENDING ? R.string.status_sending : R.string.late_sent);
            message.setVisibility(View.VISIBLE);
        } else {
            message.setVisibility(View.GONE);
        }

        blockBack.setEnabled(!gate.canLeave(ui));
        if (gate.isConfirmed(ui)) {
            finish();
        }
    }

    /** Enviar pasa por la hoja de confirmación (DESIGN.md v2 §5.6). */
    private void confirm() {
        ScoreUi ui = vm.ui().getValue();
        if (ui == null || !ui.sendEnabled || ui.red == null || ui.blue == null) {
            return;
        }
        ConfirmSheet.show(this, round, ui.red, ui.blue, () -> {
            findViewById(R.id.late_send).performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            vm.submit();
        });
    }

    private void vibrate() {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager == null ? null : manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void showNames(String fightId) {
        if (fightId == null) {
            return;
        }
        red.setName(cornerNames.red(fightId));
        blue.setName(cornerNames.blue(fightId));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
