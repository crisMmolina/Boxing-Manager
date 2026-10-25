package com.sigebox.juez.ui;

import android.content.res.ColorStateList;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.sigebox.juez.R;
import java.util.function.IntConsumer;

/**
 * Une una {@code view_corner_card} con su esquina: píldora y + en el color de la esquina, número en
 * su color numeral, − en cristal y medidor proporcional al valor (DESIGN.md v2 §5.2–5.4).
 */
public final class CornerCard {

    private final View root;
    private final boolean red;
    private final TextView value;
    private final MaterialButton minus;
    private final MaterialButton plus;
    private final LinearProgressIndicator meter;

    /** @param onAdjust recibe −1 o +1 */
    public CornerCard(View root, boolean red, IntConsumer onAdjust) {
        this.root = root;
        this.red = red;
        this.value = root.findViewById(R.id.corner_value);
        this.minus = root.findViewById(R.id.corner_minus);
        this.plus = root.findViewById(R.id.corner_plus);
        this.meter = root.findViewById(R.id.corner_meter);
        var ctx = root.getContext();
        TextView label = root.findViewById(R.id.corner_label);
        label.setText(red ? R.string.corner_red : R.string.corner_blue);
        label.setBackgroundResource(red ? R.drawable.pill_red : R.drawable.pill_blue);
        int corner = ContextCompat.getColor(ctx, red ? R.color.red_corner : R.color.blue_corner);
        plus.setBackgroundTintList(ColorStateList.valueOf(corner));
        meter.setIndicatorColor((corner & 0x00FFFFFF) | 0xD9000000); // 85 %
        value.setTextColor(ContextCompat.getColor(ctx, red ? R.color.red_numeral : R.color.blue_numeral));
        minus.setOnClickListener(v -> tap(v, onAdjust, -1));
        plus.setOnClickListener(v -> tap(v, onAdjust, 1));
    }

    /** Muestra {@code v} ("—" si es null) y habilita − y + según los límites 0–10. */
    public void bind(Integer v, boolean enabled) {
        value.setText(v == null ? root.getContext().getString(R.string.score_empty) : String.valueOf(v));
        meter.setProgressCompat(v == null ? 0 : v, true);
        minus.setEnabled(enabled && v != null && v > 0);
        plus.setEnabled(enabled && (v == null || v < 10));
        minus.setAlpha(minus.isEnabled() ? 1f : 0.35f);
        plus.setAlpha(plus.isEnabled() ? 1f : 0.35f);
        var ctx = root.getContext();
        var corner = ctx.getString(red ? R.string.corner_red_lower : R.string.corner_blue_lower);
        var shown = v == null ? ctx.getString(R.string.score_empty_a11y) : String.valueOf(v);
        minus.setContentDescription(ctx.getString(R.string.score_minus_a11y, corner, shown));
        plus.setContentDescription(ctx.getString(R.string.score_plus_a11y, corner, shown));
    }

    /** Opacidad de toda la tarjeta (50 % esperando round). */
    public void setDimmed(boolean dimmed) {
        root.animate().alpha(dimmed ? 0.5f : 1f).setDuration(200).start();
    }

    private static void tap(View v, IntConsumer onAdjust, int delta) {
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        onAdjust.accept(delta);
    }
}
