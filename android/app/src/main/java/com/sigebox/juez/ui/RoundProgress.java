package com.sigebox.juez.ui;

import android.widget.LinearLayout;
import android.view.View;
import com.sigebox.juez.R;

/** Fila de segmentos de 6 dp, uno por round (DESIGN.md v2 §5.7). Decorativa: no se anuncia. */
public final class RoundProgress {

    private RoundProgress() {
    }

    /** @param current round actual (1..rounds); 0 si aún no empieza */
    public static void bind(LinearLayout row, Integer rounds, int current, boolean currentDone) {
        row.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        int n = rounds == null ? 0 : rounds;
        if (row.getChildCount() != n) {
            row.removeAllViews();
            float density = row.getResources().getDisplayMetrics().density;
            for (int i = 0; i < n; i++) {
                var seg = new View(row.getContext());
                var lp = new LinearLayout.LayoutParams(0, Math.round(6 * density), 1f);
                lp.setMarginEnd(i == n - 1 ? 0 : Math.round(4 * density));
                row.addView(seg, lp);
            }
        }
        for (int i = 0; i < n; i++) {
            int round = i + 1;
            int bg = round < current || (round == current && currentDone) ? R.drawable.segment_done
                    : round == current ? R.drawable.segment_current : R.drawable.segment_pending;
            row.getChildAt(i).setBackgroundResource(bg);
        }
        row.setVisibility(n == 0 ? View.GONE : View.VISIBLE);
    }
}
