package com.sigebox.juez.score;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.view.HapticFeedbackConstants;

import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.protocol.Ack;
import com.sigebox.juez.R;
import com.sigebox.juez.net.ConnectionState;
import com.sigebox.juez.ui.ErrorText;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** AND-036 (spec 009): vibración y TalkBack dicen si la central recibió la tarjeta, no el toque. */
@RunWith(RobolectricTestRunner.class)
public class SendFeedbackTest {

    private final List<Integer> haptics = new ArrayList<>();
    private final List<String> announced = new ArrayList<>();
    private Context context;
    private SendFeedback feedback;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        feedback = new SendFeedback(context, new SendFeedback.Sink() {
            @Override
            public void haptic(int constant) {
                haptics.add(constant);
            }

            @Override
            public void announce(CharSequence text) {
                announced.add(text.toString());
            }
        });
    }

    private static ScoreUi ui(ConnectionState.Status connection, int round, ScoreUi.Card card, Ack.Error error) {
        return new ScoreUi("Juez", 2, connection, round, 12, true, ScoreUi.Corner.RED, null, null, false, false,
                false, card, error, false, null, false);
    }

    private static ScoreUi.Card card(ScoreUi.CardStatus status) {
        return new ScoreUi.Card(10, 9, status);
    }

    @Test
    @Config(sdk = 34)
    public void ackOkEnSdk34ConfirmaYAnunciaRoundEnviado() {
        feedback.onUi(ui(ConnectionState.Status.CONNECTED, 3, card(ScoreUi.CardStatus.SENDING), null));
        assertEquals(0, haptics.size());

        feedback.onUi(ui(ConnectionState.Status.CONNECTED, 3, card(ScoreUi.CardStatus.SENT), null));

        assertEquals(1, haptics.size());
        assertEquals(HapticFeedbackConstants.CONFIRM, (int) haptics.get(0));
        assertEquals(1, announced.size());
        assertEquals(context.getString(R.string.score_sent_n, 3), announced.get(0));
    }

    @Test
    @Config(sdk = 29)
    public void ackOkEnSdk29UsaVirtualKey() {
        feedback.onUi(ui(ConnectionState.Status.CONNECTED, 4, card(ScoreUi.CardStatus.SENDING), null));
        feedback.onUi(ui(ConnectionState.Status.CONNECTED, 4, card(ScoreUi.CardStatus.CORRECTED), null));

        assertEquals(1, haptics.size());
        assertEquals(HapticFeedbackConstants.VIRTUAL_KEY, (int) haptics.get(0));
        assertEquals(context.getString(R.string.score_sent_n, 4), announced.get(0));
    }

    @Test
    @Config(sdk = 34)
    public void ackConErrorRechazaYAnunciaElError() {
        feedback.onUi(ui(ConnectionState.Status.CONNECTED, 5, card(ScoreUi.CardStatus.SENDING), null));
        feedback.onUi(ui(ConnectionState.Status.CONNECTED, 5, null, Ack.Error.ROUND_NOT_OPEN));

        assertEquals(1, haptics.size());
        assertEquals(HapticFeedbackConstants.REJECT, (int) haptics.get(0));
        assertEquals(1, announced.size());
        assertEquals(context.getString(ErrorText.of(Ack.Error.ROUND_NOT_OPEN)), announced.get(0));
    }

    @Test
    @Config(sdk = 34)
    public void sinConexionSeAnunciaUnaSolaVez() {
        feedback.onUi(ui(ConnectionState.Status.CONNECTED, 1, null, null));
        feedback.onUi(ui(ConnectionState.Status.DISCONNECTED, 1, null, null));
        feedback.onUi(ui(ConnectionState.Status.DISCONNECTED, 1, null, null));
        feedback.onUi(ui(ConnectionState.Status.CONNECTING, 1, null, null));

        assertEquals(1, announced.size());
        assertEquals(context.getString(R.string.status_disconnected), announced.get(0));
        assertEquals(0, haptics.size());
    }

    @Test
    @Config(sdk = 34)
    public void abrirLaPantallaSinConexionNoAnunciaNada() {
        feedback.onUi(ui(ConnectionState.Status.DISCONNECTED, 0, null, null));
        feedback.onUi(ui(ConnectionState.Status.CONNECTING, 0, null, null));

        assertEquals(0, announced.size());
    }
}
