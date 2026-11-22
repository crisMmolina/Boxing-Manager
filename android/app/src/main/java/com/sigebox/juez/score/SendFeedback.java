package com.sigebox.juez.score;

import android.content.Context;
import android.os.Build;
import android.view.HapticFeedbackConstants;

import com.sigebox.juez.R;
import com.sigebox.juez.net.ConnectionState;
import com.sigebox.juez.ui.ErrorText;

/**
 * AND-036 (spec 009): vibra y anuncia cuando llega el ACK de la central, no al tocar "Enviar".
 * Con ACK correcto, CONFIRM (VIRTUAL_KEY antes de API 30) y «Round N enviado»; con error, REJECT y el
 * texto del error. Al perder la conexión anuncia «Sin conexión» una sola vez, hasta reconectar.
 */
public final class SendFeedback {

    /** Destino del aviso; en la pantalla es la vista raíz. */
    public interface Sink {
        void haptic(int constant);

        void announce(CharSequence text);
    }

    private final Context context;
    private final Sink sink;
    private boolean sending;
    private boolean wasConnected;

    public SendFeedback(Context context, Sink sink) {
        this.context = context;
        this.sink = sink;
    }

    public void onUi(ScoreUi ui) {
        boolean nowSending = ui.card != null && ui.card.status == ScoreUi.CardStatus.SENDING;
        if (sending && !nowSending) {
            if (ui.lastError != null) {
                sink.haptic(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.LONG_PRESS);
                sink.announce(context.getString(ErrorText.of(ui.lastError)));
            } else if (ui.card != null) {
                sink.haptic(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.VIRTUAL_KEY);
                int round = ui.late && ui.lateRound != null ? ui.lateRound : ui.round;
                sink.announce(context.getString(R.string.score_sent_n, round));
            }
        }
        sending = nowSending;

        boolean connected = ui.connection == ConnectionState.Status.CONNECTED;
        if (wasConnected && !connected) {
            sink.announce(context.getString(R.string.status_disconnected));
        }
        wasConnected = connected;
    }
}
