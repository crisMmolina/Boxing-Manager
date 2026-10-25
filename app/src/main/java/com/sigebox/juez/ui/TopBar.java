package com.sigebox.juez.ui;

import android.view.View;
import android.widget.TextView;
import com.sigebox.juez.R;

/** Píldora de la barra superior: punto de conexión + "Juez N" (o "Sin conexión" al unirse). */
public final class TopBar {

    private TopBar() {
    }

    public static void bind(View screen, boolean connected, String text) {
        View dot = screen.findViewById(R.id.top_dot);
        dot.setBackgroundResource(connected ? R.drawable.dot_connected : R.drawable.dot_disconnected);
        ((TextView) screen.findViewById(R.id.top_pill_text)).setText(text);
        var ctx = screen.getContext();
        screen.findViewById(R.id.top_pill).setContentDescription(
                text + ", " + ctx.getString(connected ? R.string.a11y_connected : R.string.a11y_disconnected));
    }
}
