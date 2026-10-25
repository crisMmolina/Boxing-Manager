package com.sigebox.juez.score;

import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Message;
import com.sigebox.juez.R;
import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.finalcard.FinalCardActivity;
import com.sigebox.juez.net.ConnectionState;
import com.sigebox.juez.net.JudgeConnection;
import com.sigebox.juez.net.JudgeListener;
import com.sigebox.juez.net.JudgeRuntime;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Teclado de puntuación para una mano (DESIGN.md §6.4). Nunca muestra totales ni ganador. */
public class ScoreActivity extends AppCompatActivity {
    /** Filas del teclado: 10 y 9 abajo, junto al pulgar; -1 = borrar. */
    private static final int[][] KEYS = {{0, 1, 2, 3}, {4, 5, 6, -1}, {7, 8, 9, 10}};

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private ScoreViewModel vm;
    private JudgeConnection connection;
    private SigeboxDatabase database;
    private boolean finalOpened;

    private final JudgeListener listener = new JudgeListener() {
        @Override
        public void onMessage(Message message) {
            runOnUiThread(() -> vm.onMessage(message));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        JudgeRuntime runtime = JudgeRuntime.get(this);
        connection = runtime.connection();
        database = runtime.database();
        vm = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new ScoreViewModel(connection::submit);
            }
        }).get(ScoreViewModel.class);

        buildKeypad();
        findViewById(R.id.score_red_panel).setOnClickListener(v -> vm.selectCorner(ScoreUi.Corner.RED));
        findViewById(R.id.score_blue_panel).setOnClickListener(v -> vm.selectCorner(ScoreUi.Corner.BLUE));
        findViewById(R.id.score_send).setOnClickListener(v -> vm.submit());
        findViewById(R.id.score_correct).setOnClickListener(v -> vm.startCorrection());
        vm.ui().observe(this, this::render);
        connection.state().observe(this, vm::onConnection);
    }

    @Override
    protected void onStart() {
        super.onStart();
        connection.addListener(listener);
        io.execute(() -> {
            SessionEntity session = database.sessionDao().load();
            if (session == null) {
                runOnUiThread(this::finish);
                return;
            }
            Map<Integer, OwnCards.Row> rows = OwnCards.load(database.getOpenHelper().getReadableDatabase(),
                    session.fightId);
            runOnUiThread(() -> {
                vm.setSession(session.fightId, session.seat, session.judgeName);
                vm.onOwnCards(rows);
            });
        });
    }

    @Override
    protected void onStop() {
        connection.removeListener(listener);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        io.shutdown();
        super.onDestroy();
    }

    private void buildKeypad() {
        LinearLayout keypad = findViewById(R.id.score_keypad);
        int height = dp(72);
        for (int[] row : KEYS) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (int value : row) {
                MaterialButton key = new MaterialButton(this, null,
                        com.google.android.material.R.attr.materialButtonOutlinedStyle);
                key.setText(value < 0 ? "⌫" : String.valueOf(value));
                key.setTextSize(32);
                key.setMinWidth(dp(64));
                key.setTag(value);
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
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, height, 1f);
                params.setMargins(dp(4), dp(4), dp(4), dp(4));
                line.addView(key, params);
            }
            keypad.addView(line);
        }
    }

    private void render(ScoreUi ui) {
        ((TextView) findViewById(R.id.score_header)).setText(getString(R.string.score_header, ui.seat, ui.judgeName));
        TextView conn = findViewById(R.id.score_connection);
        boolean online = ui.connection == ConnectionState.Status.CONNECTED;
        conn.setText(online ? R.string.status_connected : R.string.status_disconnected);
        conn.setTextColor(color(online ? R.color.status_sent_fg : R.color.status_disconnected_fg));

        TextView round = findViewById(R.id.score_round);
        if (ui.round > 0 && ui.rounds != null) {
            round.setText(getString(R.string.score_round_of, ui.round, ui.rounds));
        } else if (ui.round > 0) {
            round.setText(getString(R.string.score_round, ui.round));
        } else {
            round.setText("");
        }
        renderStatus(ui);

        ((TextView) findViewById(R.id.score_red_value)).setText(value(ui.card != null && !ui.editing ? ui.card.red : ui.red));
        ((TextView) findViewById(R.id.score_blue_value)).setText(value(ui.card != null && !ui.editing ? ui.card.blue : ui.blue));
        boolean input = ui.inputEnabled();
        MaterialCardView redPanel = findViewById(R.id.score_red_panel);
        MaterialCardView bluePanel = findViewById(R.id.score_blue_panel);
        redPanel.setStrokeWidth(input && ui.selected == ScoreUi.Corner.RED ? dp(4) : 0);
        bluePanel.setStrokeWidth(input && ui.selected == ScoreUi.Corner.BLUE ? dp(4) : 0);
        float alpha = ui.card != null && !ui.editing ? 0.6f : 1f;
        redPanel.setAlpha(alpha);
        bluePanel.setAlpha(alpha);

        LinearLayout keypad = findViewById(R.id.score_keypad);
        for (int i = 0; i < keypad.getChildCount(); i++) {
            LinearLayout line = (LinearLayout) keypad.getChildAt(i);
            for (int j = 0; j < line.getChildCount(); j++) {
                line.getChildAt(j).setEnabled(input);
            }
        }
        findViewById(R.id.score_send).setEnabled(ui.sendEnabled);
        findViewById(R.id.score_correct).setVisibility(ui.canCorrect ? View.VISIBLE : View.GONE);
        renderMessage(ui);

        if (ui.fightEnded && !finalOpened) {
            finalOpened = true;
            startActivity(new Intent(this, FinalCardActivity.class));
            finish();
        }
    }

    private void renderStatus(ScoreUi ui) {
        TextView chip = findViewById(R.id.score_status);
        int text;
        int fg;
        int bg;
        if (ui.card == null) {
            text = R.string.status_pending;
            fg = R.color.status_pending_fg;
            bg = R.color.status_pending_bg;
        } else {
            switch (ui.card.status) {
                case SENDING:
                    text = R.string.status_sending;
                    fg = R.color.status_pending_fg;
                    bg = R.color.status_pending_bg;
                    break;
                case CORRECTED:
                    text = R.string.status_corrected;
                    fg = R.color.status_corrected_fg;
                    bg = R.color.status_corrected_bg;
                    break;
                case LATE:
                    text = R.string.status_late;
                    fg = R.color.status_late_fg;
                    bg = R.color.status_late_bg;
                    break;
                case SENT:
                default:
                    text = R.string.status_sent;
                    fg = R.color.status_sent_fg;
                    bg = R.color.status_sent_bg;
                    break;
            }
        }
        chip.setText(text);
        chip.setTextColor(color(fg));
        chip.setBackgroundTintList(null);
        chip.setBackgroundColor(color(bg));
        chip.setVisibility(ui.round > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    private void renderMessage(ScoreUi ui) {
        TextView message = findViewById(R.id.score_message);
        String text = null;
        if (ui.lastError == Ack.Error.ROUND_NOT_OPEN) {
            text = getString(R.string.score_error_round_not_open);
        } else if (ui.lastError != null) {
            text = getString(R.string.score_error_generic, ui.lastError.name());
        } else if (!ui.roundOpen && ui.round == 0) {
            text = getString(R.string.score_waiting_start);
        } else if (!ui.roundOpen && ui.card != null) {
            text = getString(R.string.score_waiting_next, ui.round + 1);
        }
        message.setVisibility(text == null ? View.GONE : View.VISIBLE);
        message.setText(text);
        message.setTextColor(color(ui.lastError != null ? R.color.status_disconnected_fg : R.color.text_secondary));
    }

    private String value(Integer v) {
        return v == null ? getString(R.string.score_empty) : String.valueOf(v);
    }

    private int color(int res) {
        return ContextCompat.getColor(this, res);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
