package com.sigebox.juez.score;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.sigebox.juez.R;
import com.sigebox.juez.ui.Motion;

/**
 * El número del juez rueda al nuevo valor (DESIGN.md v5 §3.7, {@code motion_number}): con + entra desde abajo y con −
 * desde arriba, con un resorte leve. Con la escala de animación del sistema en 0 el número solo cambia.
 */
final class NumberRoll {
    /** Recorrido si la vista aún no tiene alto (antes del primer layout). */
    private static final int FALLBACK_DP = 24;
    private static final float SPRING = 0.8f;

    private NumberRoll() {
    }

    /**
     * Anima la entrada del texto que {@code value} ya muestra. {@code null} con movimiento reducido o sin dirección.
     *
     * @param direction +1 (tocó +) o −1 (tocó −)
     */
    static ObjectAnimator roll(TextView value, int direction) {
        if (direction == 0 || Motion.animationsOff(value.getContext())) {
            value.setTranslationY(0f);
            value.setAlpha(1f);
            return null;
        }
        float density = value.getResources().getDisplayMetrics().density;
        float distance = value.getHeight() > 0 ? value.getHeight() * 0.4f : FALLBACK_DP * density;
        float from = direction > 0 ? distance : -distance;
        ObjectAnimator roll = ObjectAnimator.ofPropertyValuesHolder(value,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, from, 0f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.2f, 1f));
        roll.setDuration(value.getResources().getInteger(R.integer.motion_number));
        roll.setInterpolator(new OvershootInterpolator(SPRING));
        roll.setAutoCancel(true);
        roll.start();
        return roll;
    }
}
