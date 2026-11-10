package com.sigebox.juez.ui;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AnimationUtils;
import com.sigebox.juez.R;

/**
 * Movimiento del juez (DESIGN.md v5 §3.7): tokens {@code motion_*}, el latido del punto de conexión y la
 * transición de eje compartido X entre pantallas. Con la escala de animación del sistema en 0 no hay
 * desplazamientos ni latidos: solo fundidos de hasta 150 ms.
 */
public final class Motion {

    private Motion() {
    }

    /** {@code Settings.Global.ANIMATOR_DURATION_SCALE}; 1 si el sistema no lo define. */
    public static float durationScale(Context context) {
        return Settings.Global.getFloat(context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
    }

    /** Movimiento reducido: la escala de animación del sistema está en 0. */
    public static boolean animationsOff(Context context) {
        return durationScale(context) == 0f;
    }

    /**
     * Latido del punto de conectado: crece y se desvanece un poco cada {@code motion_pulse}, sin fin. Lo arranca y
     * lo cancela quien lo pide. {@code null} con movimiento reducido.
     */
    public static ObjectAnimator pulse(View dot) {
        Context context = dot.getContext();
        if (animationsOff(context)) {
            return null;
        }
        ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(dot,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.35f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.35f, 1f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.45f, 1f));
        pulse.setDuration(context.getResources().getInteger(R.integer.motion_pulse));
        pulse.setInterpolator(AnimationUtils.loadInterpolator(context, R.anim.motion_curve));
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        return pulse;
    }

    /** {entrada, salida} al abrir una pantalla: eje compartido X, o fundido corto con movimiento reducido. */
    public static int[] openTransition(Context context) {
        return animationsOff(context)
                ? new int[] {R.anim.fade_reduced_in, R.anim.fade_reduced_out}
                : new int[] {R.anim.shared_axis_x_open_enter, R.anim.shared_axis_x_open_exit};
    }

    /** {entrada, salida} al cerrar una pantalla y volver a la anterior. */
    public static int[] closeTransition(Context context) {
        return animationsOff(context)
                ? new int[] {R.anim.fade_reduced_in, R.anim.fade_reduced_out}
                : new int[] {R.anim.shared_axis_x_close_enter, R.anim.shared_axis_x_close_exit};
    }

    /**
     * Aplica el eje compartido a la apertura y al cierre de {@code activity}. Se llama desde {@code onCreate}
     * (lo hace {@link Screen#setup}).
     */
    @SuppressWarnings("deprecation")
    public static void sharedAxis(Activity activity) {
        int[] open = openTransition(activity);
        int[] close = closeTransition(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, open[0], open[1]);
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, close[0], close[1]);
        } else {
            activity.overridePendingTransition(open[0], open[1]);
        }
    }
}
