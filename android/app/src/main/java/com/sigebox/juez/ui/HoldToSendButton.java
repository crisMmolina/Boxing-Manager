package com.sigebox.juez.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import com.google.android.material.button.MaterialButton;
import com.sigebox.juez.R;

/**
 * Botón «Mantén para enviar» (spec 009, DESIGN.md «Intención con un gesto»). Hay que mantenerlo
 * {@link #HOLD_MS} ms para que haga clic; soltar antes cancela. Un anillo a la izquierda se llena
 * mientras se mantiene, salvo con las animaciones del sistema apagadas: entonces queda quieto y el
 * tiempo se respeta igual. La acción de accesibilidad «Enviar round» envía sin mantener (WCAG 2.5.7).
 */
public class HoldToSendButton extends MaterialButton {

    /** Tiempo que hay que mantener, en ms. */
    public static final long HOLD_MS = 800;

    /** Intervalo del anillo, en ms (unos 60 cuadros por segundo). */
    private static final long FRAME_MS = 16;

    private final Runnable complete = this::completeHold;
    private final Runnable tick = this::tick;
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ring = new RectF();
    private long downAt;
    private float progress;
    private boolean holding;

    public HoldToSendButton(Context context) {
        super(context);
        init();
    }

    public HoldToSendButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HoldToSendButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float stroke = 3f * getResources().getDisplayMetrics().density;
        for (Paint p : new Paint[] {track, arc}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(stroke);
            p.setStrokeCap(Paint.Cap.ROUND);
        }
        ViewCompat.replaceAccessibilityAction(this, AccessibilityActionCompat.ACTION_CLICK,
                getContext().getString(R.string.confirm_send_a11y), null);
    }

    /** Avance del anillo, de 0 a 1. */
    public float progress() {
        return progress;
    }

    @SuppressLint("ClickableViewAccessibility") // el clic llega por performClick() al completar
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startHold();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (holding && !inside(event)) {
                    cancelHold();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelHold();
                return true;
            default:
                return true;
        }
    }

    private boolean inside(MotionEvent e) {
        return e.getX() >= 0 && e.getY() >= 0 && e.getX() < getWidth() && e.getY() < getHeight();
    }

    private void startHold() {
        cancelHold();
        holding = true;
        setPressed(true);
        downAt = SystemClock.uptimeMillis();
        postDelayed(complete, HOLD_MS);
        if (animationsEnabled()) {
            postDelayed(tick, FRAME_MS);
        }
    }

    /** Avanza el anillo según el tiempo mantenido, no por cuadros: no se adelanta ni se atrasa. */
    private void tick() {
        if (!holding) {
            return;
        }
        progress = Math.min(1f, (SystemClock.uptimeMillis() - downAt) / (float) HOLD_MS);
        invalidate();
        if (progress < 1f) {
            postDelayed(tick, FRAME_MS);
        }
    }

    private void cancelHold() {
        holding = false;
        removeCallbacks(complete);
        removeCallbacks(tick);
        progress = 0f;
        setPressed(false);
        invalidate();
    }

    private void completeHold() {
        cancelHold();
        performClick();
    }

    private boolean animationsEnabled() {
        float scale = Settings.Global.getFloat(getContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        return scale > 0f;
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelHold();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!holding) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        float r = 10f * density;
        float cx = getPaddingLeft() + r + 4f * density;
        float cy = getHeight() / 2f;
        ring.set(cx - r, cy - r, cx + r, cy + r);
        int ink = getCurrentTextColor();
        track.setColor(ink);
        track.setAlpha(70);
        arc.setColor(ink);
        canvas.drawOval(ring, track);
        canvas.drawArc(ring, -90f, 360f * progress, false, arc);
    }
}
