package com.sigebox.juez.wait;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.NextFight;
import com.sigebox.juez.R;
import com.sigebox.juez.net.JudgeConnection;
import com.sigebox.juez.net.JudgeListener;
import com.sigebox.juez.net.JudgeRuntime;
import com.sigebox.juez.ui.Screen;

import java.util.function.Function;

/**
 * Entre peleas (spec 004): "Espera la siguiente pelea", con las esquinas si ya se conocen, hasta que {@code NEXT_FIGHT}
 * lleve a puntuar; o "El evento terminó", que vuelve solo a Unirse con el nombre ya escrito.
 */
public final class NextFightActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT_ENDED = "eventEnded";
    /** Nombre del juez para Unirse. */
    public static final String EXTRA_JUDGE_NAME = "judgeName";

    static final long BACK_TO_JOIN_MS = 4_000;
    /** Unirse se abre por nombre, como la puntuación en {@code JudgeRuntime}. */
    private static final String JOIN_ACTIVITY = "com.sigebox.juez.join.JoinActivity";

    static Function<Context, JudgeConnection> connections = context -> JudgeRuntime.get(context).connection();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable backToJoin = this::backToJoin;
    private final JudgeListener listener = new JudgeListener() {
        @Override
        public void onMessage(Message message) {
            if (message instanceof NextFight next) {
                runOnUiThread(() -> showCorners(next));
            }
        }
    };

    private boolean ended;
    private JudgeConnection connection;
    private TextView corners;
    private com.sigebox.juez.ui.StillThereDialog stillThere;

    /** Las pantallas de {@link FightEndFlow}: se abren desde donde esté el juez, sin dejar nada atrás. */
    public static FightEndFlow.Screens screens(Context app) {
        return new FightEndFlow.Screens() {
            @Override
            public void waitNextFight() {
                app.startActivity(intent(app));
            }

            @Override
            public void eventEnded(String judgeName) {
                app.startActivity(intent(app).putExtra(EXTRA_EVENT_ENDED, true)
                        .putExtra(EXTRA_JUDGE_NAME, judgeName));
            }
        };
    }

    private static Intent intent(Context app) {
        return new Intent(app, NextFightActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_fight);
        Screen.setup(this);
        corners = findViewById(R.id.next_fight_corners);
        ended = getIntent().getBooleanExtra(EXTRA_EVENT_ENDED, false);
        if (ended) {
            // La sesión ya se borró: esta pantalla no toca la conexión.
            findViewById(R.id.next_fight_loading).setVisibility(View.GONE);
            showEndedBadge();
            ((TextView) findViewById(R.id.next_fight_title)).setText(R.string.next_fight_event_ended);
            ((TextView) findViewById(R.id.next_fight_support)).setText(R.string.next_fight_event_ended_support);
            View back = findViewById(R.id.next_fight_back);
            back.setVisibility(View.VISIBLE);
            back.setOnClickListener(view -> backToJoin());
            fillBack((com.google.android.material.button.MaterialButton) back);
        } else {
            connection = connections.apply(this);
            // AND-031: con "reducir movimiento" el indicador quedaría quieto como una mancha; el reloj ya dice que se espera.
            if (com.sigebox.juez.ui.Motion.animationsOff(this)) {
                findViewById(R.id.next_fight_loading).setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ended) {
            handler.postDelayed(backToJoin, BACK_TO_JOIN_MS);
        } else {
            connection.addListener(listener); // repite el NEXT_FIGHT sin lugar que ya llegó
            if (stillThere == null) {
                stillThere = new com.sigebox.juez.ui.StillThereDialog(this, () -> false);
            }
            stillThere.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ended) {
            handler.removeCallbacks(backToJoin);
        } else {
            connection.removeListener(listener);
            stillThere.stop();
        }
    }

    private void showCorners(NextFight next) {
        corners.setText(getString(R.string.next_fight_corners, next.redCorner(), next.blueCorner()));
        corners.setVisibility(View.VISIBLE);
        com.sigebox.juez.ui.FightCard.bind(getWindow().getDecorView(), next.redCorner(), next.blueCorner(), next.rounds());
    }

    /** AND-031: al terminar el evento "Volver a Unirse" es la única acción, así que va en píldora llena. */
    private void fillBack(com.google.android.material.button.MaterialButton back) {
        back.setStrokeWidth(0);
        back.setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(this, R.color.pill_bg));
        back.setTextColor(androidx.core.content.ContextCompat.getColorStateList(this, R.color.pill_text));
    }

    /** El evento terminó: la insignia pasa del reloj a la palomita sobre el verde tenue (DESIGN v5 §3.8). */
    private void showEndedBadge() {
        var badge = (com.google.android.material.card.MaterialCardView) findViewById(R.id.next_fight_badge);
        badge.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.success_soft));
        var icon = (android.widget.ImageView) findViewById(R.id.next_fight_badge_icon);
        icon.setImageResource(R.drawable.ic_check);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(this, R.color.success_ink)));
        icon.setTag("check");
    }

    private void backToJoin() {
        handler.removeCallbacks(backToJoin);
        startActivity(new Intent().setClassName(this, JOIN_ACTIVITY)
                .putExtra(EXTRA_JUDGE_NAME, getIntent().getStringExtra(EXTRA_JUDGE_NAME))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
