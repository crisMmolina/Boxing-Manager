package com.sigebox.juez.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import com.sigebox.juez.R;

/**
 * Chip de conexión de la barra (DESIGN.md v5 §5.17): cápsula de 28 dp. Conectado = punto lleno de 7 dp
 * {@code connected} que late sobre {@code success_soft}; Sin conexión = anillo sobre {@code surface_2}, y
 * tocarlo abre la ayuda si hay oyente. Se distingue por forma y texto, no solo por color.
 */
public final class ConnectionChip extends LinearLayout {

    private View dot;
    private TextView text;
    private boolean connected;
    private View.OnClickListener helpListener;
    private ObjectAnimator pulse;

    public ConnectionChip(Context context) {
        this(context, null);
    }

    public ConnectionChip(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.chip_height));
        int start = dp(10);
        setPaddingRelative(start, 0, dp(12), 0);
        if (attrs == null) {
            buildChildren();
            render();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        dot = findViewById(R.id.top_dot);
        text = findViewById(R.id.top_pill_text);
        if (dot == null || text == null) {
            removeAllViews();
            buildChildren();
        }
        render();
    }

    /** Sin layout: el punto y el texto se crean aquí, con los mismos ids que en {@code view_top_bar}. */
    private void buildChildren() {
        int size = getResources().getDimensionPixelSize(R.dimen.chip_dot);
        dot = new View(getContext());
        dot.setId(R.id.top_dot);
        addView(dot, new LayoutParams(size, size));
        text = new TextView(getContext());
        text.setId(R.id.top_pill_text);
        text.setMaxLines(1);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        text.setTypeface(ResourcesCompat.getFont(getContext(), R.font.inter_semibold));
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.setMarginStart(dp(6));
        addView(text, params);
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        render();
    }

    public boolean isConnected() {
        return connected;
    }

    /** Lo que pasa al tocar el chip sin conexión (la ayuda de §5.8). Conectado no es botón. */
    public void setOnHelpListener(View.OnClickListener listener) {
        helpListener = listener;
        render();
    }

    public boolean isPulsing() {
        return pulse != null && pulse.isStarted();
    }

    public View dot() {
        return dot;
    }

    public TextView text() {
        return text;
    }

    private void render() {
        if (dot == null || text == null) {
            return;
        }
        Context ctx = getContext();
        setBackgroundResource(connected ? R.drawable.chip_connected : R.drawable.pill);
        dot.setBackgroundResource(connected ? R.drawable.dot_connected : R.drawable.dot_disconnected);
        text.setText(connected ? R.string.status_connected : R.string.status_disconnected);
        text.setTextColor(ContextCompat.getColor(ctx, connected ? R.color.success_ink : R.color.ink));
        boolean button = !connected && helpListener != null;
        setOnClickListener(button ? helpListener : null);
        setClickable(button);
        ViewCompat.replaceAccessibilityAction(this, AccessibilityActionCompat.ACTION_CLICK,
                button ? ctx.getString(R.string.chip_help_action) : null, null);
        updatePulse();
    }

    private void updatePulse() {
        boolean wanted = connected && isAttachedToWindow();
        if (wanted && pulse == null) {
            pulse = Motion.pulse(dot);
            if (pulse != null) {
                pulse.start();
            }
        } else if (!wanted && pulse != null) {
            pulse.cancel();
            pulse = null;
            dot.setAlpha(1f);
            dot.setScaleX(1f);
            dot.setScaleY(1f);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updatePulse();
    }

    @Override
    protected void onDetachedFromWindow() {
        // Sin ventana no late: no deja un animador infinito vivo.
        if (pulse != null) {
            pulse.cancel();
            pulse = null;
        }
        super.onDetachedFromWindow();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
