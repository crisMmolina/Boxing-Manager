package com.sigebox.juez.late;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.sigebox.juez.R;
import com.sigebox.juez.score.CornerScore;
import com.sigebox.juez.ui.RoundRing;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * AND-029 (spec 007, v6 «Esquina»): Round tardío como Puntuar: arena, anillo de rounds, filas de esquina y la
 * píldora dorada para enviar.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, qualifiers = "w390dp-h844dp-xxhdpi")
public class LateV6Test {

    private Context themed;

    @Before
    public void setUp() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        themed = new ContextThemeWrapper(activity, R.style.Theme_Sigebox);
    }

    private View inflate() {
        FrameLayout parent = new FrameLayout(themed);
        View view = LayoutInflater.from(themed).inflate(R.layout.activity_late_score, parent, false);
        parent.addView(view);
        return view;
    }

    private int color(int id) {
        return ContextCompat.getColor(themed, id);
    }

    @Test
    public void tardioSePintaSobreArena() {
        View root = inflate();
        assertTrue(root.getBackground() instanceof ColorDrawable);
        assertEquals(color(R.color.arena), ((ColorDrawable) root.getBackground()).getColor());
    }

    @Test
    public void tardioTieneElAnilloConElRoundTardioComoActual() {
        View root = inflate();
        RoundRing ring = root.findViewById(R.id.late_ring);
        assertNotNull(ring);
        ring.bind(4, 2, false);
        assertEquals(4, ring.segmentCount());
        assertEquals(color(R.color.ring_done), ring.segmentColor(0));
        assertEquals(color(R.color.ring_current), ring.segmentColor(1));
    }

    @Test
    public void lasEsquinasSonFilasDeArena() {
        View root = inflate();
        new CornerScore(root.findViewById(R.id.late_red_card), true, d -> { }).bind(10, true);
        new CornerScore(root.findViewById(R.id.late_blue_card), false, d -> { }).bind(9, true);
        assertEquals(color(R.color.arena_red),
                ((TextView) root.findViewById(R.id.late_red_card).findViewById(R.id.corner_value)).getCurrentTextColor());
        assertEquals(color(R.color.arena_blue),
                ((TextView) root.findViewById(R.id.late_blue_card).findViewById(R.id.corner_value)).getCurrentTextColor());
    }

    @Test
    public void enviarEsLaPildoraDorada() {
        MaterialButton send = inflate().findViewById(R.id.late_send);
        float dp = themed.getResources().getDisplayMetrics().density;
        assertEquals(28 * dp, send.getCornerRadius(), 1f);
        assertEquals(color(R.color.ring_current),
                send.getBackgroundTintList().getColorForState(new int[] {android.R.attr.state_enabled}, 0));
    }

    @Test
    public void losTextosSeLeenSobreArena() {
        View root = inflate();
        assertEquals(color(R.color.on_arena), ((TextView) root.findViewById(R.id.late_title)).getCurrentTextColor());
    }
}
