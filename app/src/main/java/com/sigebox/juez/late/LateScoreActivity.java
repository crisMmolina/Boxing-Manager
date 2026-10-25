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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * La central cerró un round sin la calificación de este juez: vibra y exige calificarlo ya, a pantalla completa
 * (DESIGN.md §6.5). "Atrás" no la cierra hasta enviar; el envío pasa por la bandeja de salida y la central lo marca
 * tardío.
 */
public class LateScoreActivity extends AppCompatActivity {
    private static final int[][] KEYS = {{0, 1, 2, 3}, {4, 5, 6, -1}, {7, 8, 9, 10}};
    private static final long VIBRATION_MS = 1_200;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final LateGate gate = new LateGate();
    private ScoreViewModel vm;
    private JudgeConnection connection;
    private SigeboxDatabase database;
    private int round;

    private final JudgeListener listener = new JudgeListener() {
        @Override
        public void onMessage(Message message) {
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
        buildKeypad();
        findViewById(R.id.late_red_panel).setOnClickListener(v -> vm.selectCorner(ScoreUi.Corner.RED));
        findViewById(R.id.late_blue_panel).setOnClickListener(v -> vm.selectCorner(ScoreUi.Corner.BLUE));
        findViewById(R.id.late_send).setOnClickListener(v -> vm.submit());
        vm.ui().observe(this, this::render);
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
                runOnUiThread(() -> vm.setSession(session.fightId, session.seat, session.judgeName));
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
        Integer red = ui.card != null ? ui.card.red : ui.red;
        Integer blue = ui.card != null ? ui.card.blue : ui.blue;
        ((TextView) findViewById(R.id.late_red_value)).setText(red == null ? getString(R.string.score_empty) : String.valueOf(red));
        ((TextView) findViewById(R.id.late_blue_value)).setText(blue == null ? getString(R.string.score_empty) : String.valueOf(blue));
        boolean input = ui.inputEnabled();
        MaterialCardView redPanel = findViewById(R.id.late_red_panel);
        MaterialCardView bluePanel = findViewById(R.id.late_blue_panel);
        redPanel.setStrokeWidth(input && ui.selected == ScoreUi.Corner.RED ? dp(4) : 0);
        bluePanel.setStrokeWidth(input && ui.selected == ScoreUi.Corner.BLUE ? dp(4) : 0);
        LinearLayout keypad = findViewById(R.id.late_keypad);
        for (int i = 0; i < keypad.getChildCount(); i++) {
            LinearLayout line = (LinearLayout) keypad.getChildAt(i);
            for (int j = 0; j < line.getChildCount(); j++) {
                line.getChildAt(j).setEnabled(input);
            }
        }
        findViewById(R.id.late_send).setEnabled(ui.sendEnabled);

        TextView message = findViewById(R.id.late_message);
        if (ui.lastError != null) {
            message.setText(getString(R.string.late_error, ui.lastError.name()));
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

    private void buildKeypad() {
        LinearLayout keypad = findViewById(R.id.late_keypad);
        for (int[] row : KEYS) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (int value : row) {
                MaterialButton key = new MaterialButton(this, null,
                        com.google.android.material.R.attr.materialButtonOutlinedStyle);
                key.setText(value < 0 ? "⌫" : String.valueOf(value));
                key.setTextSize(32);
                key.setMinWidth(dp(64));
                if (value < 0) {
                    key.setContentDescription(getString(R.string.score_backspace));
                }
                key.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if (value < 0) {
                        vm.backspace();
                    } else {
                        vm.key(value);
                    }
                });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(72), 1f);
                params.setMargins(dp(4), dp(4), dp(4), dp(4));
                line.addView(key, params);
            }
            keypad.addView(line);
        }
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
