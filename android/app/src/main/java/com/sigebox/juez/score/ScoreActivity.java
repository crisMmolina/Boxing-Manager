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
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.sigebox.core.protocol.Message;
import com.sigebox.juez.R;
import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.finalcard.AutoFinalCard;
import com.sigebox.juez.net.ConnectionState;
import com.sigebox.juez.net.JudgeConnection;
import com.sigebox.juez.net.JudgeListener;
import com.sigebox.core.protocol.NextFight;
import com.sigebox.core.protocol.State;
import com.sigebox.juez.net.JudgeRuntime;
import com.sigebox.juez.ui.ConfirmSheet;
import com.sigebox.juez.ui.ErrorText;
import com.sigebox.juez.ui.Screen;
import com.sigebox.juez.ui.TopBar;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Puntuar (DESIGN.md v2 §6.4 y v5 §6): segmentos de rounds, una tarjeta por esquina con − y + y el número que rueda,
 * una hoja de confirmación antes de enviar y, tras el ACK, "Round N enviado". Nunca muestra totales ni ganador.
 */
public class ScoreActivity extends AppCompatActivity {
    /** La captura tardía la crea AND-006; se abre por nombre para no depender de ella. */
    static final String LATE_ACTIVITY = "com.sigebox.juez.late.LateScoreActivity";
    /** Extra con el round que hay que calificar tarde. */
    public static final String EXTRA_ROUND = "round";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private ScoreViewModel vm;
    private JudgeConnection connection;
    private SigeboxDatabase database;
    private CornerScore red;
    private CornerScore blue;
    private SentPanel sentPanel;
    private boolean wasOpen;

    private com.sigebox.juez.ui.CornerNames cornerNames;
    private volatile String currentFightId;

    private final JudgeListener listener = new JudgeListener() {
        @Override
        public void onMessage(Message message) {
            if (cornerNames.remember(message)) {
                runOnUiThread(() -> showNames(currentFightId));
            }
            if (message instanceof NextFight next) {
                // Con lugar, el aviso global relanza esta pantalla con la pelea nueva; sin lugar se avisa aquí.
                if (next.seat() == null) {
                    runOnUiThread(() -> NotSeatedView.show(ScoreActivity.this));
                }
                return;
            }
            if (message instanceof State) {
                runOnUiThread(() -> NotSeatedView.hide(ScoreActivity.this));
            }
            runOnUiThread(() -> vm.onMessage(message));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);
        Screen.setup(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        JudgeRuntime runtime = JudgeRuntime.get(this);
        AutoFinalCard.install(runtime); // la tarjeta final sale sola al terminar la pelea (spec 004)
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

        red = new CornerScore(findViewById(R.id.score_red_card), true, d -> vm.adjust(ScoreUi.Corner.RED, d));
        blue = new CornerScore(findViewById(R.id.score_blue_card), false, d -> vm.adjust(ScoreUi.Corner.BLUE, d));
        // "Round N enviado" con "Corregir esta tarjeta": el mismo flujo de corrección (v5 §6).
        sentPanel = new SentPanel(findViewById(R.id.score_root), vm::startCorrection);
        cornerNames = new com.sigebox.juez.ui.CornerNames(this);
        findViewById(R.id.score_send).setOnClickListener(v -> confirm());
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
                currentFightId = session.fightId;
                showNames(session.fightId);
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

    /** Enviar pasa siempre por la hoja de confirmación (DESIGN.md v2 §5.6). */
    private void confirm() {
        ScoreUi ui = vm.ui().getValue();
        if (ui == null || !ui.sendEnabled || ui.red == null || ui.blue == null) {
            return;
        }
        ConfirmSheet.show(this, ui.round, ui.red, ui.blue, () -> {
            findViewById(R.id.score_send).performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            vm.submit();
        });
    }

    private void render(ScoreUi ui) {
        boolean online = ui.connection == ConnectionState.Status.CONNECTED;
        TopBar.bind(findViewById(R.id.score_root), online,
                ui.seat > 0 ? getString(R.string.score_judge, ui.seat) : getString(R.string.status_disconnected));

        TextView round = findViewById(R.id.score_round);
        // Antes del primer STATE, los rounds salen del JOIN_APPROVED guardado.
        Integer rounds = ui.rounds != null ? ui.rounds
                : currentFightId == null ? null : cornerNames.rounds(currentFightId);
        if (ui.round > 0 && rounds != null) {
            round.setText(getString(R.string.score_round_of, ui.round, rounds));
        } else if (ui.round > 0) {
            round.setText(getString(R.string.score_round, ui.round));
        } else {
            round.setText(R.string.score_title_waiting);
        }
        renderStatus(ui);
        RoundSegments.bind(findViewById(R.id.score_progress), rounds, ui.round, ui.card != null);
        boolean sentShown = sentPanel.bind(ui, rounds);

        boolean input = ui.inputEnabled();
        boolean opened = input && !wasOpen;
        wasOpen = input;
        if (opened) {
            // Sin valor por defecto: el juez toca + o − en cada esquina antes de enviar.
            findViewById(R.id.score_root).performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        boolean showSent = ui.card != null && !ui.editing;
        red.bind(showSent ? ui.card.red : ui.red, input);
        blue.bind(showSent ? ui.card.blue : ui.blue, input);
        red.setDimmed(!input && !showSent);
        blue.setDimmed(!input && !showSent);

        var send = (com.google.android.material.button.MaterialButton) findViewById(R.id.score_send);
        send.setEnabled(ui.sendEnabled);
        if (showSent && ui.card.status != ScoreUi.CardStatus.SENDING) {
            send.setText(getString(R.string.score_sent_n, ui.round));
        } else if (ui.round > 0) {
            send.setText(getString(R.string.score_send_n, ui.round));
        } else {
            send.setText(R.string.score_send);
        }
        renderMessage(ui, online, sentShown);

        if (ui.lateRound != null) {
            vm.lateHandled();
            startActivity(new Intent().setClassName(this, LATE_ACTIVITY).putExtra(EXTRA_ROUND, (int) ui.lateRound));
        }
    }

    /** Píldora neutra: estado del round y de la tarjeta, sin colores (DESIGN.md v2 §5.5). */
    /** Nombres de los boxeadores en sus tarjetas (DESIGN.md v3 §5.2), si la central ya los mandó. */
    private void showNames(String fightId) {
        if (fightId == null) {
            return;
        }
        red.setName(cornerNames.red(fightId));
        blue.setName(cornerNames.blue(fightId));
    }

    private void renderStatus(ScoreUi ui) {
        TextView pill = findViewById(R.id.score_status);
        findViewById(R.id.score_sending).setVisibility(
                ui.card != null && ui.card.status == ScoreUi.CardStatus.SENDING ? View.VISIBLE : View.GONE);
        int text;
        if (ui.card != null) {
            switch (ui.card.status) {
                case SENDING:
                    text = R.string.status_sending;
                    break;
                case CORRECTED:
                    text = R.string.status_corrected;
                    break;
                case LATE:
                    text = R.string.status_late;
                    break;
                case SENT:
                default:
                    text = R.string.status_round_sent;
                    break;
            }
        } else if (ui.roundOpen) {
            text = R.string.status_round_open;
        } else {
            text = R.string.status_waiting_round;
        }
        pill.setText(text);
    }

    /** @param sentShown "Round N enviado" ya dice qué esperar: aquí solo errores y sin conexión. */
    private void renderMessage(ScoreUi ui, boolean online, boolean sentShown) {
        TextView message = findViewById(R.id.score_message);
        String text = null;
        if (ui.lastError != null) {
            text = getString(ErrorText.of(ui.lastError));
        } else if (!online && ui.seat > 0) {
            text = getString(R.string.score_offline);
        } else if (sentShown) {
            text = null;
        } else if (!ui.roundOpen && ui.round == 0) {
            text = getString(R.string.score_waiting_start);
        } else if (!ui.roundOpen && ui.card != null) {
            text = getString(R.string.score_waiting_next);
        } else if (!ui.roundOpen) {
            text = getString(R.string.score_waiting_open, ui.round);
        }
        message.setVisibility(text == null ? View.GONE : View.VISIBLE);
        message.setText(text);
    }
}
