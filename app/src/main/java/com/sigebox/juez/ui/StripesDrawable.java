package com.sigebox.juez.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.sigebox.juez.R;

/**
 * Fondo de DESIGN.md v2 §4.2: blanco con franjas diagonales tenues que se desvanecen hacia abajo.
 * Se dibuja una vez en un bitmap por tamaño y se reutiliza.
 */
public final class StripesDrawable extends Drawable {

    private static final float ANGLE_DEG = 115f;
    private final float stripe;
    private final float gap;
    private final int stripeColor;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap cache;

    public StripesDrawable(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        stripe = 14 * density;
        gap = 16 * density;
        stripeColor = ContextCompat.getColor(context, R.color.stripe);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        if (b.width() <= 0 || b.height() <= 0) {
            return;
        }
        if (cache == null || cache.getWidth() != b.width() || cache.getHeight() != b.height()) {
            cache = render(b.width(), b.height());
        }
        canvas.drawBitmap(cache, b.left, b.top, null);
    }

    private Bitmap render(int w, int h) {
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        c.drawColor(Color.WHITE);
        Paint stripes = new Paint(Paint.ANTI_ALIAS_FLAG);
        stripes.setColor(stripeColor);
        stripes.setStrokeWidth(stripe);
        c.save();
        c.rotate(ANGLE_DEG - 90f, w / 2f, h / 2f);
        float span = (float) Math.hypot(w, h);
        for (float x = -span; x < span * 2; x += stripe + gap) {
            c.drawLine(x, -span, x, span * 2, stripes);
        }
        c.restore();
        // Desvanece hacia el blanco en la parte inferior.
        paint.setShader(new LinearGradient(0, h * 0.35f, 0, h * 0.8f, Color.TRANSPARENT, Color.WHITE,
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        return bitmap;
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
