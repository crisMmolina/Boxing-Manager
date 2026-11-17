package com.sigebox.juez.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.sigebox.juez.R;

/**
 * Anillo de rounds de Puntuar (v6 «Esquina», spec 007): un arco por round alrededor del round actual, como el
 * anillo del temporizador de las apps de entrenamiento. Cerrados en {@code ring_done}, el actual en
 * {@code ring_current} (el dorado, único acento) y los futuros en {@code ring_pending}. Decorativo para TalkBack:
 * el texto del centro dice el round. Nunca muestra puntos ni totales (principio 3).
 *
 * <p>Al abrir un round nuevo su arco se dibuja en 300 ms; con "reducir movimiento" (escala de animación del
 * sistema en 0) aparece completo.
 */
public class RoundRing extends View {

    static final long DRAW_MS = 300;
    /** Hueco entre arcos, en grados. */
    private static final float GAP = 6f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private int rounds;
    private int current;
    private boolean currentDone;
    private float progress = 1f;
    @Nullable private Animator lastAnimation;

    public RoundRing(Context context) {
        this(context, null);
    }

    public RoundRing(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(getResources().getDimension(R.dimen.round_ring_stroke));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** {@code rounds} null (todavía sin STATE ni JOIN_APPROVED): un anillo vacío de pista. */
    public void bind(@Nullable Integer rounds, int current, boolean currentDone) {
        int total = rounds == null ? 0 : rounds;
        boolean newRound = current != this.current && current > 0 && this.current > 0;
        this.rounds = total;
        this.current = current;
        this.currentDone = currentDone;
        if (lastAnimation != null) {
            lastAnimation.cancel();
            lastAnimation = null;
        }
        if (newRound && !Motion.animationsOff(getContext())) {
            ValueAnimator draw = ValueAnimator.ofFloat(0f, 1f);
            draw.setDuration(DRAW_MS);
            draw.setInterpolator(new DecelerateInterpolator());
            draw.addUpdateListener(a -> {
                progress = (float) a.getAnimatedValue();
                invalidate();
            });
            lastAnimation = draw;
            draw.start();
        } else {
            progress = 1f;
        }
        invalidate();
    }

    public int segmentCount() {
        return rounds;
    }

    /** Color del arco {@code index} (0 = round 1). */
    public int segmentColor(int index) {
        int round = index + 1;
        int color = round < current || (round == current && currentDone) ? R.color.ring_done
                : round == current ? R.color.ring_current : R.color.ring_pending;
        return ContextCompat.getColor(getContext(), color);
    }

    @Nullable
    public Animator lastAnimation() {
        return lastAnimation;
    }

    /** Cuánto del arco actual está dibujado (1 = completo). */
    public float currentProgress() {
        return progress;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float half = paint.getStrokeWidth() / 2f;
        oval.set(getPaddingLeft() + half, getPaddingTop() + half,
                getWidth() - getPaddingRight() - half, getHeight() - getPaddingBottom() - half);
        if (rounds <= 0) {
            paint.setColor(ContextCompat.getColor(getContext(), R.color.ring_pending));
            canvas.drawArc(oval, 0f, 360f, false, paint);
            return;
        }
        float sweep = 360f / rounds;
        float arc = Math.max(1f, sweep - GAP);
        for (int i = 0; i < rounds; i++) {
            float start = -90f + i * sweep + GAP / 2f;
            boolean isCurrent = i + 1 == current;
            if (isCurrent && progress < 1f) {
                paint.setColor(ContextCompat.getColor(getContext(), R.color.ring_pending));
                canvas.drawArc(oval, start, arc, false, paint);
            }
            paint.setColor(segmentColor(i));
            canvas.drawArc(oval, start, isCurrent ? arc * progress : arc, false, paint);
        }
    }

    /** El centro del anillo: "Round", el número del round actual y "de M". */
    public static void bindCenter(View screen, @Nullable Integer rounds, int current) {
        Context ctx = screen.getContext();
        TextView number = screen.findViewById(R.id.score_ring_number);
        TextView of = screen.findViewById(R.id.score_ring_of);
        View box = screen.findViewById(R.id.score_ring_box);
        if (number == null || of == null || box == null) {
            return;
        }
        number.setText(current > 0 ? String.valueOf(current) : "–");
        of.setText(rounds != null ? ctx.getString(R.string.ring_of, rounds) : "");
        of.setVisibility(rounds != null ? View.VISIBLE : View.GONE);
        box.setContentDescription(current <= 0 ? ctx.getString(R.string.score_title_waiting)
                : rounds != null ? ctx.getString(R.string.score_round_of, current, rounds)
                : ctx.getString(R.string.score_round, current));
    }
}
