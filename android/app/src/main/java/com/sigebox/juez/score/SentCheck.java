package com.sigebox.juez.score;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.core.content.ContextCompat;

import com.sigebox.juez.R;
import com.sigebox.juez.ui.Motion;

/**
 * Palomita de "Round N enviado" (DESIGN.md v5 §3.7, {@code motion_check}): el círculo {@code success_soft} crece
 * (0.6 → 1.08 → 1) y el trazo {@code success_ink} de {@code check} (Lucide) se dibuja. Con la escala de animación del
 * sistema en 0 aparece completa y quieta. Decorativa: el título ya dice que se envió.
 */
public class SentCheck extends View {
    /** Fracción de {@code motion_check} en la que el trazo empieza a dibujarse (antes solo crece el círculo). */
    private static final float STROKE_START = 0.3f;
    private static final float STROKE_DP = 4f;

    private final Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path check = new Path();
    private final Path partial = new Path();
    private PathMeasure measure;
    private float progress = 1f;
    private ValueAnimator animator;

    public SentCheck(Context context, AttributeSet attrs) {
        super(context, attrs);
        circle.setStyle(Paint.Style.FILL);
        // v6: sobre arena (tag "arena") el círculo es verde oscuro, no menta (spec 007).
        boolean arena = attrs != null && "arena".equals(
                attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "tag"));
        circle.setColor(ContextCompat.getColor(context, arena ? R.color.arena_success : R.color.success_soft));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setColor(ContextCompat.getColor(context, arena ? R.color.arena_success_ink : R.color.success_ink));
        stroke.setStrokeWidth(STROKE_DP * getResources().getDisplayMetrics().density);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** Dibuja la palomita desde cero, o la muestra completa con movimiento reducido. */
    public void play() {
        if (animator != null) {
            animator.cancel();
        }
        if (Motion.animationsOff(getContext())) {
            animator = null;
            apply(1f);
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(getResources().getInteger(R.integer.motion_check));
        animator.setInterpolator(AnimationUtils.loadInterpolator(getContext(), R.anim.motion_curve));
        animator.addUpdateListener(a -> apply((float) a.getAnimatedValue()));
        animator.start();
    }

    /** {@code true} mientras se dibuja. */
    public boolean isAnimating() {
        return animator != null && animator.isRunning();
    }

    /** Fracción del trazo ya dibujada (0 a 1). */
    public float progress() {
        return progress;
    }

    /** La última animación de {@link #play}, o {@code null} si fue sin movimiento. */
    public ValueAnimator animator() {
        return animator;
    }

    private void apply(float f) {
        float scale = f < 0.5f ? lerp(0.6f, 1.08f, f / 0.5f) : lerp(1.08f, 1f, (f - 0.5f) / 0.5f);
        setScaleX(scale);
        setScaleY(scale);
        progress = Math.max(0f, Math.min(1f, (f - STROKE_START) / (1f - STROKE_START)));
        invalidate();
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // "M20 6 9 17l-5-5" sobre 24, dibujado desde el brazo corto; ocupa la mitad del círculo.
        float side = Math.min(w, h) * 0.5f;
        float left = (w - side) / 2f;
        float top = (h - side) / 2f;
        float unit = side / 24f;
        check.reset();
        check.moveTo(left + 4 * unit, top + 12 * unit);
        check.lineTo(left + 9 * unit, top + 17 * unit);
        check.lineTo(left + 20 * unit, top + 6 * unit);
        measure = new PathMeasure(check, false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        canvas.drawCircle(w / 2f, h / 2f, Math.min(w, h) / 2f, circle);
        if (measure == null || progress <= 0f) {
            return;
        }
        partial.reset();
        measure.getSegment(0f, measure.getLength() * progress, partial, true);
        canvas.drawPath(partial, stroke);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.end();
        }
        super.onDetachedFromWindow();
    }
}
