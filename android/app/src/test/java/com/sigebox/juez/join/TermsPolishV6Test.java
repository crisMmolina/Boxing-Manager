package com.sigebox.juez.join;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.graphics.ColorUtils;

import com.google.android.material.button.MaterialButton;
import com.sigebox.juez.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** AND-031 (pulido v6): la píldora principal deshabilitada se lee (≥ 3:1) en claro y en oscuro. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class TermsPolishV6Test {

    private static double contrastDisabled(MaterialButton button) {
        int[] disabled = {-android.R.attr.state_enabled};
        int bg = button.getBackgroundTintList().getColorForState(disabled, Color.MAGENTA);
        int text = button.getTextColors().getColorForState(disabled, Color.MAGENTA);
        assertEquals("fondo opaco: el contraste no depende de lo que hay detrás", 255, Color.alpha(bg));
        return ColorUtils.calculateContrast(ColorUtils.setAlphaComponent(text, 255), bg);
    }

    private static MaterialButton accept() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Context themed = new ContextThemeWrapper(activity, R.style.Theme_Sigebox);
        FrameLayout parent = new FrameLayout(themed);
        View root = LayoutInflater.from(themed).inflate(R.layout.activity_terms, parent, false);
        return root.findViewById(R.id.terms_accept);
    }

    @Test
    public void aceptarDeshabilitadoPasaTresAUnoEnClaro() {
        double ratio = contrastDisabled(accept());
        assertTrue("contraste " + ratio, ratio >= 3.0);
    }

    @Test
    @Config(qualifiers = "night")
    public void aceptarDeshabilitadoPasaTresAUnoEnOscuro() {
        double ratio = contrastDisabled(accept());
        assertTrue("contraste " + ratio, ratio >= 3.0);
    }
}
