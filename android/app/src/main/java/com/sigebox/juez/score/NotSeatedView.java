package com.sigebox.juez.score;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sigebox.juez.R;

/**
 * Pantalla "No participas en esta pelea" (spec 002): cubre la puntuación cuando la central manda {@code NEXT_FIGHT}
 * sin lugar. Se arma en código y con los estilos del tema, así no toca layouts ni recursos de otras tareas.
 */
final class NotSeatedView {
    static final String TAG = "not_seated";

    private NotSeatedView() {
    }

    /** Muestra (o actualiza) la cubierta sobre el contenido de la actividad. */
    static void show(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        View existing = content.findViewWithTag(TAG);
        if (existing != null) {
            existing.setVisibility(View.VISIBLE);
            return;
        }
        FrameLayout cover = new FrameLayout(activity);
        cover.setTag(TAG);
        cover.setBackgroundResource(R.color.bg);
        cover.setClickable(true);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        int pad = (int) (24 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, pad);
        box.addView(text(activity, R.style.Text_Sigebox_CardTitle, activity.getString(R.string.not_seated_title)));
        box.addView(text(activity, R.style.Text_Sigebox_Support, activity.getString(R.string.not_seated_support)));
        cover.addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        content.addView(cover, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    static void hide(Activity activity) {
        View existing = activity.findViewById(android.R.id.content).findViewWithTag(TAG);
        if (existing != null) {
            existing.setVisibility(View.GONE);
        }
    }

    private static TextView text(Activity activity, int style, String value) {
        TextView view = new TextView(activity, null, 0, style);
        view.setText(value);
        view.setGravity(Gravity.CENTER);
        return view;
    }
}
