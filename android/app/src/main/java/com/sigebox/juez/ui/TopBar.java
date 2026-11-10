package com.sigebox.juez.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.sigebox.juez.R;
import com.sigebox.juez.help.ConnectionHelp;

/**
 * Barra superior: "Juez N", el chip de conexión (DESIGN.md v5 §5.17) y el "?" de ayuda de conexión, siempre
 * visible y destacado en dorado mientras no hay conexión (DESIGN.md v4 §5.8).
 */
public final class TopBar {

    private TopBar() {
    }

    public static void bind(View screen, boolean connected, String text) {
        var ctx = screen.getContext();
        ConnectionChip chip = screen.findViewById(R.id.top_pill);
        chip.setConnected(connected);
        chip.setOnHelpListener(v -> ConnectionHelp.show(v.getContext(), false));
        // El chip ya dice Conectado o Sin conexión: "Juez N" solo se muestra cuando aporta algo.
        TextView judge = screen.findViewById(R.id.top_judge);
        boolean repeatsState = text.equals(ctx.getString(R.string.status_connected))
                || text.equals(ctx.getString(R.string.status_disconnected));
        judge.setText(text);
        judge.setVisibility(repeatsState ? View.GONE : View.VISIBLE);
        chip.setContentDescription(
                text + ", " + ctx.getString(connected ? R.string.a11y_connected : R.string.a11y_disconnected));
        bindHelp((ViewGroup) chip.getParent(), connected);
    }

    private static void bindHelp(ViewGroup bar, boolean connected) {
        TextView help = bar.findViewById(R.id.top_help);
        if (help == null) {
            help = createHelp(bar.getContext());
            bar.addView(help);
        }
        Context ctx = help.getContext();
        help.setSelected(!connected);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        int stroke = Math.round(2 * ctx.getResources().getDisplayMetrics().density);
        if (connected) {
            circle.setStroke(stroke, ContextCompat.getColor(ctx, R.color.on_bar));
            help.setTextColor(ContextCompat.getColor(ctx, R.color.on_bar));
        } else {
            circle.setColor(ContextCompat.getColor(ctx, R.color.bar_accent));
            circle.setStroke(stroke, ContextCompat.getColor(ctx, R.color.bar_accent));
            help.setTextColor(ContextCompat.getColor(ctx, R.color.bar));
        }
        help.setBackground(circle);
        help.setOnClickListener(v -> ConnectionHelp.show(v.getContext(), connected));
    }

    private static TextView createHelp(Context ctx) {
        int size = Math.round(48 * ctx.getResources().getDisplayMetrics().density);
        TextView help = new TextView(ctx);
        help.setId(R.id.top_help);
        help.setText(R.string.help_icon);
        help.setContentDescription(ctx.getString(R.string.help_a11y));
        help.setGravity(Gravity.CENTER);
        help.setTextSize(22);
        help.setTypeface(ResourcesCompat.getFont(ctx, R.font.barlow_condensed_extrabold));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginStart(Math.round(8 * ctx.getResources().getDisplayMetrics().density));
        help.setLayoutParams(params);
        return help;
    }
}
