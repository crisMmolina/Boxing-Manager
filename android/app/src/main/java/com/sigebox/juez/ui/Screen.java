package com.sigebox.juez.ui;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.sigebox.juez.R;

/**
 * Lo común a todas las pantallas del juez (DESIGN.md v4): de borde a borde con los insets de las
 * barras del sistema aplicados a la raíz (targetSdk 37 dibuja bajo la barra de estado), fondo `bg`
 * y la barra superior negra hasta los bordes.
 */
public final class Screen {

    private Screen() {
    }

    /** Llamar justo después de {@code setContentView}. */
    public static void setup(Activity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        // Fondo sólido `bg`, sin rayas ni cristal.
        activity.getWindow().getDecorView().setBackgroundResource(R.color.bg);
        ViewGroup content = activity.findViewById(android.R.id.content);
        View root = content.getChildCount() > 0 ? content.getChildAt(0) : content;
        View bar = root.findViewById(R.id.top_bar);
        if (bar != null) {
            extendBar(activity, content, bar);
        }
        applyInsets(root);
        // v5 §3.7: eje compartido X al entrar y salir; con la escala de animación en 0, solo un fundido corto.
        Motion.sharedAxis(activity);
    }

    /** La barra negra llega a los bordes y sigue bajo la barra de estado, con íconos claros. */
    private static void extendBar(Activity activity, ViewGroup content, View bar) {
        ((ViewGroup) bar.getParent()).setClipToPadding(false);
        View strip = new View(activity);
        strip.setId(R.id.status_strip);
        strip.setBackgroundResource(R.color.bar);
        strip.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        content.addView(strip, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
    }

    /** Suma las barras del sistema y el recorte de la cámara al padding original de {@code root}. */
    public static void applyInsets(View root) {
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        root.setTag(R.id.screen_insets_applied, Boolean.TRUE);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(left + bars.left, top + bars.top, right + bars.right,
                    bottom + Math.max(bars.bottom, ime.bottom));
            View strip = v.getRootView().findViewById(R.id.status_strip);
            if (strip != null && strip.getLayoutParams().height != bars.top) {
                strip.getLayoutParams().height = bars.top;
                strip.requestLayout();
            }
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
