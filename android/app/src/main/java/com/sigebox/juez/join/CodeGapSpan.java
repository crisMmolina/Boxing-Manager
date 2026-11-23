package com.sigebox.juez.join;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;

/**
 * AND-037: dibuja su carácter y deja un hueco después, para que el código se lea «123 456» sin meter un espacio en
 * el texto (el campo sigue entregando solo los seis dígitos).
 */
final class CodeGapSpan extends ReplacementSpan {

    /** Hueco en múltiplos del ancho de un espacio. */
    private static final float GAP_SPACES = 1.5f;

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        if (fm != null) {
            paint.getFontMetricsInt(fm);
        }
        return Math.round(paint.measureText(text, start, end) + gap(paint));
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom,
            Paint paint) {
        canvas.drawText(text, start, end, x, y, paint);
    }

    private static float gap(Paint paint) {
        return paint.measureText(" ") * GAP_SPACES;
    }
}
