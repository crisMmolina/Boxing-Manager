package com.sigebox.juez.ui;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.sigebox.juez.R;

import java.util.function.IntConsumer;

/**
 * Une una {@code view_corner_card} con su esquina (DESIGN.md v3 §5.1–5.2): píldora en el color de la esquina, nombre
 * del boxeador, número en su color numeral, − en {@code surface_2} y + en el color de la esquina. − y + se aplanan al
 * presionarlos con un rebote leve.
 */
public final class CornerCard {
    /** Duración del aplanado; con movimiento reducido (escala 0) ValueAnimator salta al final. */
    private static final long PRESS_MS = 150;

    private final View root;
    private final boolean red;
    private final TextView value;
    private final TextView name;
    private final MaterialButton minus;
    private final MaterialButton plus;

    /** @param onAdjust recibe −1 o +1 */
    public CornerCard(View root, boolean red, IntConsumer onAdjust) {
        this.root = root;
        this.red = red;
        this.value = root.findViewById(R.id.corner_value);
        this.name = root.findViewById(R.id.corner_name);
        this.minus = root.findViewById(R.id.corner_minus);
        this.plus = root.findViewById(R.id.corner_plus);
        var ctx = root.getContext();
        TextView label = root.findViewById(R.id.corner_label);
        label.setText(red ? R.string.corner_red : R.string.corner_blue);
        label.setBackgroundResource(red ? R.drawable.pill_red : R.drawable.pill_blue);
        int onCorner = ContextCompat.getColor(ctx, red ? R.color.on_red : R.color.on_blue);
        label.setTextColor(onCorner);
        plus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, red ? R.color.red : R.color.blue)));
        plus.setIconTint(ColorStateList.valueOf(onCorner));
        // v6: en la fila de arena (tag "arena") el número usa el tono claro de su esquina, que pasa AA sobre negro.
        boolean arena = "arena".equals(root.getTag());
        value.setTextColor(ContextCompat.getColor(ctx, arena
                ? (red ? R.color.arena_red : R.color.arena_blue)
                : (red ? R.color.red_numeral : R.color.blue_numeral)));
        if (arena) {
            plus.setStrokeWidth(0);
        }
        flattenOnPress(minus);
        flattenOnPress(plus);
        minus.setOnClickListener(v -> tap(v, onAdjust, -1));
        plus.setOnClickListener(v -> tap(v, onAdjust, 1));
    }

    /** Nombre del boxeador ({@code redCorner} / {@code blueCorner}); vacío hasta que la central lo manda. */
    public void setName(String boxer) {
        name.setText(boxer == null ? "" : boxer);
    }

    /** Muestra {@code v} ("—" si es null) y habilita − y + según los límites 0–10. */
    public void bind(Integer v, boolean enabled) {
        value.setText(v == null ? root.getContext().getString(R.string.score_empty) : String.valueOf(v));
        minus.setEnabled(enabled && v != null && v > 0);
        plus.setEnabled(enabled && (v == null || v < 10));
        minus.setAlpha(minus.isEnabled() ? 1f : 0.38f);
        plus.setAlpha(plus.isEnabled() ? 1f : 0.38f);
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

    @SuppressLint("ClickableViewAccessibility") // el clic sigue en setOnClickListener; esto solo anima la forma
    private static void flattenOnPress(MaterialButton button) {
        float pressed = button.getResources().getDimension(R.dimen.round_button_pressed_radius);
        float rest = button.getResources().getDimension(R.dimen.round_button) / 2f;
        button.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN && v.isEnabled()) {
                animateCorners(button, rest, pressed);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                animateCorners(button, pressed, rest);
            }
            return false;
        });
    }

    private static void animateCorners(MaterialButton button, float from, float to) {
        ValueAnimator animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(PRESS_MS);
        animator.setInterpolator(new OvershootInterpolator(1.2f));
        animator.addUpdateListener(a -> button.setShapeAppearanceModel(
                ShapeAppearanceModel.builder().setAllCornerSizes((float) a.getAnimatedValue()).build()));
        animator.start();
    }

    private static void tap(View v, IntConsumer onAdjust, int delta) {
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        onAdjust.accept(delta);
    }
}
