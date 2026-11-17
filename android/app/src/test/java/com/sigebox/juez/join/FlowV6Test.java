package com.sigebox.juez.join;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.material.button.MaterialButton;
import com.sigebox.juez.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * AND-030 (spec 007, v6 «Esquina»): Unirse, Esperando, Siguiente pelea y Términos usan la misma familia que
 * Puntuar: una sola acción principal en píldora llena de 56 dp y lo demás en píldora de contorno.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class FlowV6Test {

    private Context themed;

    @Before
    public void setUp() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        themed = new ContextThemeWrapper(activity, R.style.Theme_Sigebox);
    }

    private MaterialButton button(int layout, int id) {
        FrameLayout parent = new FrameLayout(themed);
        View root = LayoutInflater.from(themed).inflate(layout, parent, false);
        parent.addView(root);
        return root.findViewById(id);
    }

    private float dp(float v) {
        return v * themed.getResources().getDisplayMetrics().density;
    }

    private void assertPill(MaterialButton b, String name) {
        assertTrue(name + " mide " + b.getMinHeight(), b.getMinHeight() >= dp(56) - 1);
        assertEquals(name + " radio completo", dp(28), b.getCornerRadius(), 1f);
    }

    private void assertFilled(MaterialButton b, String name) {
        assertPill(b, name);
        assertEquals(name + " es llena (sin contorno)", 0, b.getStrokeWidth());
    }

    private void assertOutline(MaterialButton b, String name) {
        assertPill(b, name);
        assertTrue(name + " es de contorno", b.getStrokeWidth() >= dp(2) - 1);
    }

    @Test
    public void unirseEscanearEsLaPrincipalYUnirmeLaSecundaria() {
        assertFilled(button(R.layout.activity_join, R.id.join_scan), "join_scan");
        assertOutline(button(R.layout.activity_join, R.id.join_submit), "join_submit");
    }

    @Test
    public void terminosAceptarEsLaPrincipal() {
        assertFilled(button(R.layout.activity_terms, R.id.terms_accept), "terms_accept");
    }

    @Test
    public void esperandoYSiguienteSoloTienenSecundarias() {
        assertOutline(button(R.layout.activity_waiting, R.id.waiting_cancel), "waiting_cancel");
        assertOutline(button(R.layout.activity_next_fight, R.id.next_fight_back), "next_fight_back");
    }
}
