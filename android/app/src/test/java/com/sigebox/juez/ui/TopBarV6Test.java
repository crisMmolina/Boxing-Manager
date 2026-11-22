package com.sigebox.juez.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sigebox.juez.R;
import com.sigebox.juez.terms.TermsActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * AND-027 (spec 007): la marca de la barra lleva una inclinación leve (el único gesto deportivo de la v6) y, a
 * 390 dp con "Juez 2", el chip "Sin conexión" y el "?", se lee completa: nunca "SIGEBO…".
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, qualifiers = "w390dp-h844dp-xxhdpi")
public class TopBarV6Test {

    private TextView brand(View screen) {
        ViewGroup bar = screen.findViewById(R.id.top_bar);
        return (TextView) bar.getChildAt(1);
    }

    private View terms() {
        return Robolectric.buildActivity(TermsActivity.class).setup().get().getWindow().getDecorView();
    }

    @Test
    public void laMarcaVaInclinada() {
        float skew = brand(terms()).getPaint().getTextSkewX();
        assertTrue("inclinación leve hacia adelante: " + skew, skew < -0.05f && skew > -0.3f);
    }

    @Test
    public void conJuezChipYAyudaLaMarcaSeLeeCompletaA390dp() {
        // La barra completa, como en Puntuar: "Juez 2", el chip y el "?".
        android.app.Activity activity = Robolectric.buildActivity(TermsActivity.class).setup().get();
        android.widget.LinearLayout screen = new android.widget.LinearLayout(activity);
        screen.setOrientation(android.widget.LinearLayout.VERTICAL);
        int margin = activity.getResources().getDimensionPixelSize(R.dimen.screen_margin);
        screen.setPadding(margin, margin, margin, margin);
        screen.setClipToPadding(false);
        android.view.LayoutInflater.from(activity).inflate(R.layout.view_top_bar, screen, true);
        TopBar.bind(screen, false, "Juez 2");
        int width = Math.round(390 * activity.getResources().getDisplayMetrics().density);
        screen.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        screen.layout(0, 0, width, screen.getMeasuredHeight());
        TextView brand = brand(screen);
        String text = brand.getText().toString();
        assertTrue(text, text.startsWith("Box Juez"));
        float needed = brand.getPaint().measureText(text) + brand.getPaddingStart() + brand.getPaddingEnd();
        assertTrue("la marca mide " + brand.getWidth() + " px y necesita " + needed,
                brand.getWidth() >= needed);
    }
}
