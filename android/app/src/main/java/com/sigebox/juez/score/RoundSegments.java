package com.sigebox.juez.score;

import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.sigebox.juez.R;

/**
 * Segmentos de rounds (DESIGN.md v5 §5.16): uno por round, cápsula de 6 dp; cerrado {@code connected}, actual
 * {@code accent}, futuro {@code ring_track}. Decorativos: el round ya se anuncia en el título. Sirve a Puntuar, Round
 * enviado y Round tardío.
 */
public final class RoundSegments {
    private static final int HEIGHT_DP = 6;
    private static final int GAP_DP = 4;

    private RoundSegments() {
    }

    /**
     * @param current round actual (1..rounds); 0 si aún no empieza
     * @param currentDone el juez ya envió el round actual: cuenta como cerrado
     */
    public static void bind(LinearLayout row, Integer rounds, int current, boolean currentDone) {
        row.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        int n = rounds == null ? 0 : rounds;
        if (row.getChildCount() != n) {
            row.removeAllViews();
            float density = row.getResources().getDisplayMetrics().density;
            for (int i = 0; i < n; i++) {
                View segment = new View(row.getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Math.round(HEIGHT_DP * density), 1f);
                lp.setMarginEnd(i == n - 1 ? 0 : Math.round(GAP_DP * density));
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.RECTANGLE);
                shape.setCornerRadius(HEIGHT_DP * density / 2f);
                segment.setBackground(shape);
                row.addView(segment, lp);
            }
        }
        for (int i = 0; i < n; i++) {
            int round = i + 1;
            int color = round < current || (round == current && currentDone) ? R.color.connected
                    : round == current ? R.color.accent : R.color.ring_track;
            ((GradientDrawable) row.getChildAt(i).getBackground())
                    .setColor(ContextCompat.getColor(row.getContext(), color));
        }
        row.setVisibility(n == 0 ? View.GONE : View.VISIBLE);
    }
}
