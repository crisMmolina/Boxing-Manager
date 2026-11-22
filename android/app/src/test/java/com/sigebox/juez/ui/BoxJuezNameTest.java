package com.sigebox.juez.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.sigebox.juez.R;
import com.sigebox.juez.terms.TermsActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * AND-033 (spec 008 §Nombre): la app del juez se llama «Box Juez» en el teléfono, la barra, los términos y los
 * textos; donde se nombra a la central dice «Box Central». La marca cabe junto a «Juez N», el chip y el «?».
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, qualifiers = "w390dp-h844dp-xxhdpi")
public class BoxJuezNameTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void elNombreDeLaAppEsBoxJuez() {
        assertEquals("Box Juez", context.getString(R.string.app_name));
    }

    @Test
    public void laMarcaDeLaBarraEsBoxJuez() {
        assertEquals("Box Juez", context.getString(R.string.brand));
    }

    @Test
    public void losTerminosDicenBoxJuezYNoSigebox() {
        String terms = context.getString(R.string.terms_body);
        assertTrue(terms, terms.contains("Box Juez"));
        assertFalse(terms, terms.contains("SIGEBOX"));
    }

    @Test
    public void losTextosNombranBoxJuezOBoxCentral() {
        assertEquals("Este código no es de Box Juez", context.getString(R.string.join_not_sigebox));
        String help = context.getString(R.string.help_central_steps);
        assertTrue(help, help.contains("deja pasar a Box Central"));
        assertFalse(help, help.contains("SIGEBOX"));
    }

    @Test
    public void laMarcaBoxJuezCabeConJuezChipYAyudaA390dp() {
        Activity activity = Robolectric.buildActivity(TermsActivity.class).setup().get();
        LinearLayout screen = new LinearLayout(activity);
        screen.setOrientation(LinearLayout.VERTICAL);
        int margin = activity.getResources().getDimensionPixelSize(R.dimen.screen_margin);
        screen.setPadding(margin, margin, margin, margin);
        screen.setClipToPadding(false);
        LayoutInflater.from(activity).inflate(R.layout.view_top_bar, screen, true);
        TopBar.bind(screen, false, "Juez 12");
        int width = Math.round(390 * activity.getResources().getDisplayMetrics().density);
        screen.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        screen.layout(0, 0, width, screen.getMeasuredHeight());
        ViewGroup bar = screen.findViewById(R.id.top_bar);
        TextView brand = (TextView) bar.getChildAt(1);
        String text = brand.getText().toString();
        assertEquals("Box Juez", text);
        float needed = brand.getPaint().measureText(text) + brand.getPaddingStart() + brand.getPaddingEnd();
        assertTrue("la marca mide " + brand.getWidth() + " px y necesita " + needed, brand.getWidth() >= needed);
    }
}
