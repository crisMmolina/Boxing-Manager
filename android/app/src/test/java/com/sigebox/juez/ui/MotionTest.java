package com.sigebox.juez.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import com.sigebox.juez.R;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** DESIGN.md v5 §3.7: tokens de movimiento, eje compartido entre pantallas y escala de animación del sistema. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class MotionTest {

    private static Context context() {
        return ApplicationProvider.getApplicationContext();
    }

    private static void scale(float value) {
        Settings.Global.putFloat(context().getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, value);
    }

    @After
    public void tearDown() {
        scale(1f);
    }

    @Test
    public void tokensDeMovimiento() {
        var res = context().getResources();
        assertEquals(120, res.getInteger(R.integer.motion_fast));
        assertEquals(180, res.getInteger(R.integer.motion_base));
        assertEquals(220, res.getInteger(R.integer.motion_enter));
        assertEquals(200, res.getInteger(R.integer.motion_number));
        assertEquals(380, res.getInteger(R.integer.motion_check));
        assertEquals(1800, res.getInteger(R.integer.motion_pulse));
    }

    @Test
    public void conEscalaNormalHayAnimaciones() {
        scale(1f);
        assertEquals(1f, Motion.durationScale(context()), 0f);
        assertFalse(Motion.animationsOff(context()));
    }

    @Test
    public void conEscalaCeroLasAnimacionesEstanApagadas() {
        scale(0f);
        assertEquals(0f, Motion.durationScale(context()), 0f);
        assertTrue(Motion.animationsOff(context()));
    }

    @Test
    public void elLatidoDuraLoDelTokenYSeRepite() {
        scale(1f);
        ObjectAnimator pulse = Motion.pulse(new View(context()));
        assertNotNull(pulse);
        assertEquals(1800, pulse.getDuration());
        // Robolectric guarda INFINITE aparte para no animar sin fin en la prueba.
        assertEquals(ValueAnimator.INFINITE, org.robolectric.Shadows.shadowOf(pulse).getActualRepeatCount());
    }

    @Test
    public void conEscalaCeroNoHayLatidoYElChipNoLate() {
        scale(0f);
        assertNull(Motion.pulse(new View(context())));

        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setTheme(R.style.Theme_Sigebox);
        FrameLayout root = new FrameLayout(activity);
        View bar = LayoutInflater.from(activity).inflate(R.layout.view_top_bar, root, false);
        root.addView(bar);
        activity.setContentView(root);
        TopBar.bind(bar, true, "Juez 2");
        ConnectionChip chip = bar.findViewById(R.id.top_pill);
        assertTrue("sigue diciendo Conectado", chip.isConnected());
        assertFalse("sin movimiento", chip.isPulsing());
    }

    @Test
    public void entreActividadesEsEjeCompartidoXDeHasta300ms() {
        scale(1f);
        int[] open = Motion.openTransition(context());
        int[] close = Motion.closeTransition(context());
        assertEquals(R.anim.shared_axis_x_open_enter, open[0]);
        assertEquals(R.anim.shared_axis_x_open_exit, open[1]);
        assertEquals(R.anim.shared_axis_x_close_enter, close[0]);
        assertEquals(R.anim.shared_axis_x_close_exit, close[1]);
        for (int id : new int[] {open[0], open[1], close[0], close[1]}) {
            Animation anim = AnimationUtils.loadAnimation(context(), id);
            assertTrue("≤ 300 ms", anim.computeDurationHint() <= 300);
            assertTrue("se desplaza en X", hasTranslate(anim));
        }
    }

    @Test
    public void conEscalaCeroSoloFundidosCortos() {
        scale(0f);
        int[] open = Motion.openTransition(context());
        int[] close = Motion.closeTransition(context());
        for (int id : new int[] {open[0], open[1], close[0], close[1]}) {
            Animation anim = AnimationUtils.loadAnimation(context(), id);
            assertTrue("≤ 150 ms", anim.computeDurationHint() <= 150);
            assertFalse("sin desplazamiento", hasTranslate(anim));
        }
    }

    private static boolean hasTranslate(Animation anim) {
        if (anim instanceof TranslateAnimation) {
            return true;
        }
        if (anim instanceof AnimationSet) {
            for (Animation child : ((AnimationSet) anim).getAnimations()) {
                if (hasTranslate(child)) {
                    return true;
                }
            }
        }
        return false;
    }
}
