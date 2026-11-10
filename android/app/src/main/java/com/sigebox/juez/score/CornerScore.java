package com.sigebox.juez.score;

import android.animation.Animator;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.sigebox.juez.R;
import com.sigebox.juez.ui.CornerCard;

import java.util.function.IntConsumer;

/**
 * Tarjeta de esquina v5 (DESIGN.md v5 §6) sobre {@link CornerCard}: franja izquierda de 6 dp del color de la esquina
 * y número que rueda al tocar − o +. La háptica corta del toque ya la da {@link CornerCard}. Solo rueda un cambio que
 * viene de un toque: un redibujado (conexión, ACK, round nuevo, bandeja de salida) cambia el número sin moverlo.
 */
public final class CornerScore {
    private final CornerCard card;
    private final TextView value;
    private int tapped;
    private Integer shown;
    private Animator lastRoll;

    /** @param onAdjust recibe −1 o +1 */
    public CornerScore(View root, boolean red, IntConsumer onAdjust) {
        value = root.findViewById(R.id.corner_value);
        View stripe = root.findViewById(R.id.corner_stripe);
        stripe.setBackgroundColor(ContextCompat.getColor(root.getContext(), red ? R.color.red : R.color.blue));
        root.setClipToOutline(true); // la franja sigue la esquina redonda de la tarjeta
        card = new CornerCard(root, red, delta -> {
            tapped = delta; // antes de avisar: el ViewModel redibuja en la misma llamada
            onAdjust.accept(delta);
        });
    }

    public void setName(String boxer) {
        card.setName(boxer);
    }

    public void setDimmed(boolean dimmed) {
        card.setDimmed(dimmed);
    }

    /** Muestra {@code v} ("—" si es null); si cambió por un toque, el número rueda en la dirección del toque. */
    public void bind(Integer v, boolean enabled) {
        Integer before = shown;
        card.bind(v, enabled);
        shown = v;
        int direction = tapped;
        tapped = 0;
        lastRoll = direction != 0 && v != null && !v.equals(before) ? NumberRoll.roll(value, direction) : null;
    }

    /** La animación del último {@link #bind}, o {@code null} si no rodó. */
    public Animator lastRoll() {
        return lastRoll;
    }
}
