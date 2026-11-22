package com.sigebox.juez.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * La marca «Box Juez» de la barra con una inclinación leve hacia adelante (v6 «Esquina», spec 007): el único
 * gesto deportivo de la interfaz. Solo la marca; títulos y texto van rectos.
 */
public class BrandText extends AppCompatTextView {

    static final float SKEW = -0.14f;

    public BrandText(Context context) {
        this(context, null);
    }

    public BrandText(Context context, AttributeSet attrs) {
        super(context, attrs);
        getPaint().setTextSkewX(SKEW);
        // La inclinación saca el borde derecho de la última letra: un poco de aire para que no se recorte.
        setPaddingRelative(getPaddingStart(), getPaddingTop(),
                getPaddingEnd() + Math.round(4 * getResources().getDisplayMetrics().density), getPaddingBottom());
    }
}
