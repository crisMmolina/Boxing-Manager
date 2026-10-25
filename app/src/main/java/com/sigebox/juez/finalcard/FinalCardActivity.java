package com.sigebox.juez.finalcard;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.SubmitFinalCard;
import com.sigebox.juez.R;
import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.net.JudgeConnection;
import com.sigebox.juez.net.JudgeListener;
import com.sigebox.juez.net.JudgeRuntime;
import com.sigebox.juez.net.KeystoreSigner;
import com.sigebox.juez.score.OwnCards;
import com.sigebox.juez.score.ScoreUi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Solo la tarjeta propia, sin fila de totales ni ganador (DESIGN.md §6.6), firmada al enviarla. */
public class FinalCardActivity extends AppCompatActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private JudgeConnection connection;
    private SigeboxDatabase database;
    private SessionEntity session;
    private Map<Integer, OwnCards.Row> rows;
    private volatile String sentId;

    private final JudgeListener listener = new JudgeListener() {
        @Override
        public void onMessage(Message message) {
            if (message instanceof Ack ack && ack.ackId().equals(sentId)) {
                runOnUiThread(() -> onAck(ack));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_card);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        JudgeRuntime runtime = JudgeRuntime.get(this);
        connection = runtime.connection();
        database = runtime.database();
        findViewById(R.id.final_send).setEnabled(false);
        findViewById(R.id.final_send).setOnClickListener(v -> send());
    }

    @Override
    protected void onStart() {
        super.onStart();
        connection.addListener(listener);
        io.execute(() -> {
            SessionEntity loaded = database.sessionDao().load();
            if (loaded == null) {
                runOnUiThread(this::finish);
                return;
            }
            Map<Integer, OwnCards.Row> loadedRows =
                    OwnCards.load(database.getOpenHelper().getReadableDatabase(), loaded.fightId);
            runOnUiThread(() -> {
                session = loaded;
                rows = loadedRows;
                render();
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

    private void render() {
        ((TextView) findViewById(R.id.final_title)).setText(getString(R.string.final_title, session.seat));
        TableLayout table = findViewById(R.id.final_table);
        table.removeAllViews();
        table.addView(row(getString(R.string.final_col_round), getString(R.string.score_red),
                getString(R.string.score_blue), getString(R.string.final_col_status)));
        boolean allConfirmed = !rows.isEmpty();
        List<OwnCards.Row> sorted = new ArrayList<>(rows.values());
        sorted.sort((a, b) -> Integer.compare(a.round, b.round));
        for (OwnCards.Row card : sorted) {
            table.addView(row(String.valueOf(card.round), String.valueOf(card.red), String.valueOf(card.blue),
                    getString(statusText(card.status))));
            allConfirmed &= card.status != ScoreUi.CardStatus.SENDING;
        }
        if (sentId == null) {
            ((TextView) findViewById(R.id.final_body)).setText(allConfirmed ? R.string.final_body : R.string.final_wait_acks);
            findViewById(R.id.final_send).setEnabled(allConfirmed);
        }
    }

    private void send() {
        if (sentId != null || session == null) {
            return;
        }
        findViewById(R.id.final_send).setEnabled(false);
        String fightId = session.fightId;
        int seat = session.seat;
        List<com.sigebox.core.protocol.State.OwnCard> cards = FinalCard.toOwnCards(rows);
        io.execute(() -> {
            SubmitFinalCard message = FinalCard.build(fightId, seat, cards, new KeystoreSigner()::sign);
            sentId = message.id();
            connection.submit(message);
            runOnUiThread(() -> ((TextView) findViewById(R.id.final_body)).setText(R.string.status_sending));
        });
    }

    private void onAck(Ack ack) {
        TextView body = findViewById(R.id.final_body);
        if (ack.ok() || ack.error() == Ack.Error.ALREADY_FINAL) {
            body.setText(R.string.final_sent);
            findViewById(R.id.final_send).setVisibility(View.GONE);
        } else {
            body.setText(getString(R.string.final_error, ack.error().name()));
        }
    }

    private TableRow row(String... cells) {
        TableRow row = new TableRow(this);
        for (String cell : cells) {
            TextView text = new TextView(this);
            text.setText(cell);
            text.setTextSize(18);
            text.setPadding(8, 12, 8, 12);
            row.addView(text);
        }
        return row;
    }

    private static int statusText(ScoreUi.CardStatus status) {
        switch (status) {
            case SENDING:
                return R.string.status_sending;
            case CORRECTED:
                return R.string.status_corrected;
            case LATE:
                return R.string.status_late;
            case SENT:
            default:
                return R.string.status_sent;
        }
    }
}
