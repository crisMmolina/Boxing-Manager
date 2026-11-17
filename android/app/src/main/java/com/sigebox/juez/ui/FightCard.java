package com.sigebox.juez.ui;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.sigebox.juez.R;

/**
 * Tarjeta de pelea de Esperando y Siguiente pelea (AND-030, spec 007 v6 «Esquina»): la fila
 * {@code view_fight_card} con la píldora roja, el chip de rounds y la píldora azul, más la línea de nombres
 * {@code fight_names} si la pantalla la tiene. Sin las dos esquinas todo queda oculto; sin rounds, el chip.
 * Nunca muestra puntos (principio 3).
 */
public final class FightCard {
    private FightCard() {
    }

    public static void bind(View root, @Nullable String red, @Nullable String blue, @Nullable Integer rounds) {
        View card = root.findViewById(R.id.fight_card);
        if (card == null) {
            return;
        }
        Context ctx = root.getContext();
        boolean known = red != null && !red.isEmpty() && blue != null && !blue.isEmpty();
        card.setVisibility(known ? View.VISIBLE : View.GONE);
        TextView chip = root.findViewById(R.id.fight_rounds);
        boolean hasRounds = known && rounds != null && rounds > 0;
        chip.setVisibility(hasRounds ? View.VISIBLE : View.GONE);
        if (hasRounds) {
            chip.setText(ctx.getResources().getQuantityString(R.plurals.fight_rounds, rounds, rounds));
        }
        TextView names = root.findViewById(R.id.fight_names);
        if (names != null) {
            names.setVisibility(known ? View.VISIBLE : View.GONE);
            if (known) {
                names.setText(ctx.getString(R.string.next_fight_corners, red, blue));
            }
        }
        card.setContentDescription(known ? ctx.getString(R.string.fight_card_a11y, red, blue) : null);
    }
}
