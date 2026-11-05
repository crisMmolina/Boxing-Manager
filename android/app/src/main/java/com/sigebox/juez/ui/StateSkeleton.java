package com.sigebox.juez.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.State;
import com.sigebox.juez.R;
import com.sigebox.juez.net.JudgeConnection;
import com.sigebox.juez.net.JudgeListener;
import com.sigebox.juez.net.JudgeRuntime;

import java.util.function.Function;

/**
 * DESIGN.md v4 §5.6: ocupa el lugar de `score_content` con la forma de las tarjetas hasta que llega el
 * primer STATE. Sin STATE no hay round conocido, así que esconder la pantalla no le quita nada al juez.
 */
public class StateSkeleton extends LinearLayout {

    static final long SLOW_AFTER_MS = 3_000;
    private static final long SHINE_MS = 1_400;

    /** Pruebas: la conexión que escucha. */
    static Function<Context, JudgeConnection> connections = context -> JudgeRuntime.get(context).connection();

    private final Paint shinePaint = new Paint();
    private final Matrix shineMatrix = new Matrix();
    private final Runnable showSlow = () -> findViewById(R.id.skeleton_slow).setVisibility(VISIBLE);
    private final JudgeListener listener = new JudgeListener() {
        @Override
        public void onMessage(Message message) {
            // Llega en el hilo de la conexión.
            if (message instanceof State) {
                post(StateSkeleton.this::reveal);
            }
        }
    };

    private JudgeConnection connection;
    private ValueAnimator shine;
    private float shineProgress;
    private boolean revealed;

    public StateSkeleton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode() || revealed) {
            return;
        }
        postDelayed(showSlow, SLOW_AFTER_MS);
        startShine();
        connection = connections.apply(getContext());
        connection.addListener(listener);
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    private void reveal() {
        if (revealed) {
            return;
        }
        revealed = true;
        stop();
        setVisibility(GONE);
        ((View) getParent()).findViewById(R.id.score_content).setVisibility(VISIBLE);
    }

    private void stop() {
        removeCallbacks(showSlow);
        if (shine != null) {
            shine.cancel();
            shine = null;
        }
        if (connection != null) {
            connection.removeListener(listener);
            connection = null;
        }
    }

    private void startShine() {
        float scale = Settings.Global.getFloat(getContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        if (scale == 0f) {
            return; // Movimiento reducido: bloques fijos.
        }
        shine = ValueAnimator.ofFloat(0f, 1f);
        shine.setDuration(SHINE_MS);
        shine.setRepeatCount(ValueAnimator.INFINITE);
        shine.setInterpolator(new LinearInterpolator());
        shine.addUpdateListener(animation -> {
            shineProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        shine.start();
    }

    boolean isShining() {
        return shine != null;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        // Color del fondo: aclara los bloques y no se nota en los huecos entre ellos.
        int color = ContextCompat.getColor(getContext(), R.color.bg);
        int clear = ColorUtils.setAlphaComponent(color, 0);
        shinePaint.setShader(new LinearGradient(0, 0, bandWidth(), 0,
                new int[] {clear, ColorUtils.setAlphaComponent(color, 160), clear}, null, Shader.TileMode.CLAMP));
    }

    private float bandWidth() {
        return getWidth() * 0.6f;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        Shader shader = shinePaint.getShader();
        if (shine == null || shader == null) {
            return;
        }
        float band = bandWidth();
        float x = -band + shineProgress * (getWidth() + band);
        shineMatrix.setTranslate(x, 0);
        shader.setLocalMatrix(shineMatrix);
        canvas.drawRect(x, 0, x + band, getHeight(), shinePaint);
    }
}
