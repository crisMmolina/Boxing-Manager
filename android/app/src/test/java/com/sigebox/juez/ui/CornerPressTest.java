package com.sigebox.juez.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.button.MaterialButton;
import com.sigebox.juez.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** AND-039: − y + con la curva estándar y un fondo presionado que se distingue (≥ 3:1) del reposo. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class CornerPressTest {

    private Context themed;

    @Before
    public void setUp() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        themed = new ContextThemeWrapper(activity, R.style.Theme_Sigebox);
    }

    @Test
    public void pressUsesStandardCurveWithoutOvershoot() {
        assertFalse(CornerCard.pressInterpolator(themed) instanceof OvershootInterpolator);
    }

    @Test
    public void pressedBackgroundContrastsWithRestLight() {
        assertPressedContrast();
    }

    @Test
    @Config(qualifiers = "+night")
    public void pressedBackgroundContrastsWithRestDark() {
        assertPressedContrast();
    }

    private void assertPressedContrast() {
        for (int layout : new int[] {R.layout.view_corner_card, R.layout.view_corner_row}) {
            boolean arena = layout == R.layout.view_corner_row;
            int under = ContextCompat.getColor(themed, arena ? R.color.arena_2 : R.color.surface);
            for (boolean red : new boolean[] {true, false}) {
                View view = inflate(layout);
                CornerCard card = new CornerCard(view, red, d -> { });
                card.bind(5, true);
                for (int id : new int[] {R.id.corner_minus, R.id.corner_plus}) {
                    MaterialButton b = view.findViewById(id);
                    int rest = b.getBackgroundTintList().getColorForState(
                            new int[] {android.R.attr.state_enabled}, 0);
                    int pressed = b.getBackgroundTintList().getColorForState(
                            new int[] {android.R.attr.state_enabled, android.R.attr.state_pressed}, 0);
                    double ratio = ColorUtils.calculateContrast(
                            ColorUtils.compositeColors(pressed, under), ColorUtils.compositeColors(rest, under));
                    String what = (arena ? "fila" : "tarjeta") + (red ? " roja " : " azul ")
                            + (id == R.id.corner_minus ? "−" : "+") + " " + ratio;
                    assertTrue(what, ratio >= 3.0);
                }
            }
        }
    }

    private View inflate(int layout) {
        FrameLayout parent = new FrameLayout(themed);
        View view = LayoutInflater.from(themed).inflate(layout, parent, false);
        parent.addView(view);
        return view;
    }
}
