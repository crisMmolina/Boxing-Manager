package com.sigebox.juez.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.sigebox.juez.R;

/**
 * Lo común a todas las pantallas del juez (DESIGN.md v2 §6): de borde a borde con los insets de las
 * barras del sistema aplicados a la raíz (targetSdk 37 dibuja bajo la barra de estado) y el fondo
 * blanco con franjas tenues.
 */
public final class Screen {

    private Screen() {
    }

    /** Llamar justo después de {@code setContentView}. */
    public static void setup(Activity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        activity.getWindow().getDecorView().setBackground(new StripesDrawable(activity));
        ViewGroup content = activity.findViewById(android.R.id.content);
        View root = content.getChildCount() > 0 ? content.getChildAt(0) : content;
        applyInsets(root);
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
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
