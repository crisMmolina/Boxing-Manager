package com.sigebox.juez.score;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.sigebox.juez.R;

/**
 * "Round N enviado" (DESIGN.md v5 §6): tras el {@code ACK}, la palomita que se dibuja, el título, "La central lo
 * recibió…", la tarjeta propia (solo la del juez, nunca totales ni ganador: principio 3) y "Corregir esta tarjeta"
 * como acción secundaria, que sigue el flujo de corrección de siempre. Mientras se ve, las tarjetas de puntuar y
 * Enviar se ocultan; al corregir vuelven.
 */
public final class SentPanel {
    private final View panel;
    private final SentCheck check;
    private final TextView title;
    private final TextView support;
    private final View card;
    private final TextView red;
    private final TextView blue;
    private final View redCard;
    private final View blueCard;
    private final View send;
    private final View correct;
    private boolean showing;

    /** @param root la pantalla {@code activity_score} */
    public SentPanel(View root, Runnable onCorrect) {
        panel = root.findViewById(R.id.score_sent_panel);
        check = root.findViewById(R.id.score_sent_check);
        title = root.findViewById(R.id.score_sent_title);
        support = root.findViewById(R.id.score_sent_support);
        card = root.findViewById(R.id.score_sent_card);
        red = root.findViewById(R.id.score_sent_red);
        blue = root.findViewById(R.id.score_sent_blue);
        redCard = root.findViewById(R.id.score_red_card);
        blueCard = root.findViewById(R.id.score_blue_card);
        send = root.findViewById(R.id.score_send);
        correct = root.findViewById(R.id.score_correct);
        correct.setOnClickListener(v -> onCorrect.run());
    }

    /** La central ya confirmó la tarjeta del round actual y el juez no la está corrigiendo. */
    static boolean isSent(ScoreUi ui) {
        return ui.card != null && ui.card.status != ScoreUi.CardStatus.SENDING && !ui.editing && !ui.late;
    }

    /**
     * @param rounds rounds de la pelea, o {@code null} si aún no se conocen
     * @return {@code true} si se ve "Round N enviado"
     */
    public boolean bind(ScoreUi ui, Integer rounds) {
        boolean sent = isSent(ui);
        panel.setVisibility(sent ? View.VISIBLE : View.GONE);
        int scoring = sent ? View.GONE : View.VISIBLE;
        redCard.setVisibility(scoring);
        blueCard.setVisibility(scoring);
        send.setVisibility(scoring);
        correct.setVisibility(ui.canCorrect ? View.VISIBLE : View.GONE);
        if (sent) {
            Context ctx = panel.getContext();
            title.setText(ctx.getString(R.string.score_sent_n, ui.round));
            boolean last = rounds != null && ui.round >= rounds;
            support.setText(last ? ctx.getString(R.string.sent_support_last)
                    : ctx.getString(R.string.sent_support_next, ui.round + 1));
            red.setText(String.valueOf(ui.card.red));
            blue.setText(String.valueOf(ui.card.blue));
            card.setContentDescription(ctx.getString(R.string.sent_card_a11y, ui.round, ui.card.red, ui.card.blue));
            if (!showing) {
                check.play(); // una vez al llegar el ACK, no en cada redibujado
            }
        }
        showing = sent;
        return sent;
    }
}
