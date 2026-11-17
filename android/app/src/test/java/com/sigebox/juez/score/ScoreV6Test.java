package com.sigebox.juez.score;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.juez.R;
import com.sigebox.juez.ui.RoundRing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * AND-028 (spec 007, v6 «Esquina»): Puntuar sobre arena, anillo de rounds con el round actual al centro, una fila
 * por esquina (número grande, etiqueta, − y + de 56 dp) y "Enviar round" como píldora en el tercio inferior.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, qualifiers = "w390dp-h844dp-xxhdpi")
public class ScoreV6Test {

    private Context themed;

    @Before
    public void setUp() {
        scale(1f);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        themed = new ContextThemeWrapper(activity, R.style.Theme_Sigebox);
    }

    @After
    public void tearDown() {
        scale(1f);
    }

    private static void scale(float value) {
        Context app = ApplicationProvider.getApplicationContext();
        Settings.Global.putFloat(app.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, value);
    }

    private View inflate(int layout) {
        FrameLayout parent = new FrameLayout(themed);
        View view = LayoutInflater.from(themed).inflate(layout, parent, false);
        parent.addView(view);
        return view;
    }

    private int color(int id) {
        return ContextCompat.getColor(themed, id);
    }

    private float dp(float value) {
        return value * themed.getResources().getDisplayMetrics().density;
    }

    /** Puntuar con el contenido visible, medido a 390 × 844 dp. */
    private View laidOut() {
        View root = inflate(R.layout.activity_score);
        root.findViewById(R.id.score_skeleton).setVisibility(View.GONE);
        root.findViewById(R.id.score_content).setVisibility(View.VISIBLE);
        int w = Math.round(dp(390));
        int h = Math.round(dp(844));
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
        return root;
    }

    private static int top(View view, View ancestor) {
        int y = 0;
        View v = view;
        while (v != ancestor && v != null) {
            y += v.getTop();
            v = (View) v.getParent();
        }
        return y;
    }

    // --- Arena ---

    @Test
    public void puntuarSePintaSobreArena() {
        View root = inflate(R.layout.activity_score);
        assertTrue(root.getBackground() instanceof ColorDrawable);
        assertEquals(color(R.color.arena), ((ColorDrawable) root.getBackground()).getColor());
    }

    @Test
    @Config(qualifiers = "+night")
    public void enOscuroTambienEsArena() {
        View root = inflate(R.layout.activity_score);
        assertEquals(color(R.color.arena), ((ColorDrawable) root.getBackground()).getColor());
    }

    // --- Anillo de rounds ---

    @Test
    public void elAnilloTieneUnSegmentoPorRoundConSusColores() {
        View root = inflate(R.layout.activity_score);
        RoundRing ring = root.findViewById(R.id.score_ring);
        assertNotNull(ring);
        ring.bind(12, 3, false);
        assertEquals(12, ring.segmentCount());
        assertEquals(color(R.color.ring_done), ring.segmentColor(0));
        assertEquals(color(R.color.ring_done), ring.segmentColor(1));
        assertEquals(color(R.color.ring_current), ring.segmentColor(2));
        for (int i = 3; i < 12; i++) {
            assertEquals(color(R.color.ring_pending), ring.segmentColor(i));
        }
    }

    @Test
    public void conElRoundEnviadoElActualCuentaComoCerrado() {
        RoundRing ring = inflate(R.layout.activity_score).findViewById(R.id.score_ring);
        ring.bind(4, 3, true);
        assertEquals(color(R.color.ring_done), ring.segmentColor(2));
        assertEquals(color(R.color.ring_pending), ring.segmentColor(3));
    }

    @Test
    public void alCentroVanElRoundYDeCuantos() {
        View root = inflate(R.layout.activity_score);
        RoundRing.bindCenter(root, 12, 3);
        assertEquals("3", ((TextView) root.findViewById(R.id.score_ring_number)).getText().toString());
        assertEquals("de 12", ((TextView) root.findViewById(R.id.score_ring_of)).getText().toString());
        assertTrue("el anillo dice el round a TalkBack",
                root.findViewById(R.id.score_ring_box).getContentDescription().toString().contains("Round 3 de 12"));
    }

    @Test
    public void elAnilloAnimaElRoundNuevo() {
        RoundRing ring = inflate(R.layout.activity_score).findViewById(R.id.score_ring);
        ring.bind(12, 2, false);
        ring.bind(12, 3, false);
        assertNotNull(ring.lastAnimation());
    }

    @Test
    public void conReducirMovimientoElAnilloCambiaSinAnimacion() {
        scale(0f);
        RoundRing ring = inflate(R.layout.activity_score).findViewById(R.id.score_ring);
        ring.bind(12, 2, false);
        ring.bind(12, 3, false);
        assertNull(ring.lastAnimation());
        assertEquals(1f, ring.currentProgress(), 0f);
    }

    // --- Filas de esquina ---

    @Test
    public void cadaEsquinaEsUnaFilaConNumeroGrandeYBotonesDe56dp() {
        View root = laidOut();
        for (int id : new int[] {R.id.score_red_card, R.id.score_blue_card}) {
            View row = root.findViewById(id);
            TextView value = row.findViewById(R.id.corner_value);
            float sp = value.getTextSize() / themed.getResources().getDisplayMetrics().scaledDensity;
            assertTrue("número de " + sp + " sp", sp >= 96f);
            for (int button : new int[] {R.id.corner_minus, R.id.corner_plus}) {
                View b = row.findViewById(button);
                assertTrue("botón de " + b.getWidth() + " px", b.getWidth() >= dp(56) - 1 && b.getHeight() >= dp(56) - 1);
            }
        }
    }

    @Test
    public void rojaVaArribaDeAzul() {
        View root = laidOut();
        assertTrue(top(root.findViewById(R.id.score_red_card), root)
                < top(root.findViewById(R.id.score_blue_card), root));
    }

    @Test
    public void elNumeroDeCadaEsquinaUsaSuColorDeArena() {
        View root = inflate(R.layout.activity_score);
        new CornerScore(root.findViewById(R.id.score_red_card), true, d -> { }).bind(10, true);
        new CornerScore(root.findViewById(R.id.score_blue_card), false, d -> { }).bind(9, true);
        assertEquals(color(R.color.arena_red),
                ((TextView) root.findViewById(R.id.score_red_card).findViewById(R.id.corner_value)).getCurrentTextColor());
        assertEquals(color(R.color.arena_blue),
                ((TextView) root.findViewById(R.id.score_blue_card).findViewById(R.id.corner_value)).getCurrentTextColor());
    }

    // --- Enviar ---

    @Test
    public void enviarEsUnaPildoraEnElTercioInferior() {
        View root = laidOut();
        View send = root.findViewById(R.id.score_send);
        assertTrue("alto " + send.getHeight(), send.getHeight() >= dp(56) - 1);
        int h = root.getHeight();
        assertTrue("arriba de " + (2 * h / 3) + ": " + top(send, root), top(send, root) >= 2 * h / 3);
        TypedValue radius = new TypedValue();
        assertEquals(dp(28), ((com.google.android.material.button.MaterialButton) send).getCornerRadius(), 1f);
    }
}
