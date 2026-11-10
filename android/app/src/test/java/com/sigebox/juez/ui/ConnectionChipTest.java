package com.sigebox.juez.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.sigebox.juez.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** DESIGN.md v5 §5.17: chip de conexión en la barra, Conectado (punto que late) y Sin conexión (anillo y texto). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ConnectionChipTest {

    private Activity activity;
    private View bar;
    private ConnectionChip chip;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setTheme(R.style.Theme_Sigebox);
        FrameLayout root = new FrameLayout(activity);
        bar = LayoutInflater.from(activity).inflate(R.layout.view_top_bar, root, false);
        root.addView(bar);
        activity.setContentView(root);
        chip = bar.findViewById(R.id.top_pill);
    }

    @org.junit.After
    public void tearDown() {
        Settings.Global.putFloat(activity.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
    }

    private int px(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }

    private int color(int id) {
        return ContextCompat.getColor(activity, id);
    }

    @Test
    public void laBarraLlevaElChip() {
        assertNotNull("chip en la barra", chip);
        assertEquals("cápsula de 28 dp", px(28), chip.getMinimumHeight());
    }

    @Test
    public void conectadoDiceConectadoConPuntoQueLate() {
        TopBar.bind(bar, true, "Juez 2");

        assertTrue(chip.isConnected());
        assertEquals(activity.getString(R.string.status_connected), chip.text().getText().toString());
        assertEquals(color(R.color.success_ink), chip.text().getCurrentTextColor());
        assertEquals(color(R.color.success_soft),
                ((GradientDrawable) chip.getBackground()).getColor().getDefaultColor());
        View dot = chip.dot();
        assertEquals("punto de 7 dp", px(7), dot.getLayoutParams().width);
        assertEquals("punto lleno", color(R.color.connected),
                ((GradientDrawable) dot.getBackground()).getColor().getDefaultColor());
        assertTrue("el punto late", chip.isPulsing());
        TextView judge = bar.findViewById(R.id.top_judge);
        assertEquals(View.VISIBLE, judge.getVisibility());
        assertEquals("Juez 2", judge.getText().toString());
    }

    @Test
    public void sinConexionDiceSinConexionConAnilloYAbreLaAyuda() {
        TopBar.bind(bar, false, activity.getString(R.string.status_disconnected));

        assertFalse(chip.isConnected());
        assertEquals(activity.getString(R.string.status_disconnected), chip.text().getText().toString());
        assertEquals(color(R.color.surface_2),
                ((GradientDrawable) chip.getBackground()).getColor().getDefaultColor());
        assertNull("anillo, sin relleno", ((GradientDrawable) chip.dot().getBackground()).getColor());
        assertFalse("el anillo no late", chip.isPulsing());
        assertEquals("no repite el estado", View.GONE, bar.findViewById(R.id.top_judge).getVisibility());

        assertTrue("toca para abrir la ayuda", chip.isClickable());
        assertTrue(chip.hasOnClickListeners());
    }

    @Test
    public void sinConexionElToqueLlamaALaAyuda() {
        boolean[] opened = {false};
        chip.setOnHelpListener(v -> opened[0] = true);
        chip.setConnected(false);
        assertTrue(chip.performClick());
        assertTrue("abre la ayuda", opened[0]);
        chip.setConnected(true);
        opened[0] = false;
        chip.performClick();
        assertFalse("conectado no abre la ayuda", opened[0]);
    }

    @Test
    public void alReconectarVuelveALatirYDejaDeSerBoton() {
        TopBar.bind(bar, false, "Juez 2");
        assertFalse(chip.isPulsing());
        assertEquals("el juez sigue visible", View.VISIBLE, bar.findViewById(R.id.top_judge).getVisibility());

        TopBar.bind(bar, true, "Juez 2");
        assertTrue(chip.isPulsing());
        assertFalse("conectado no abre la ayuda", chip.isClickable());
    }

    @Test
    public void sinOyenteNoEsBoton() {
        ContextThemeWrapper themed = new ContextThemeWrapper(activity, R.style.Theme_Sigebox);
        ConnectionChip alone = new ConnectionChip(themed);
        alone.setConnected(false);
        assertEquals(activity.getString(R.string.status_disconnected), alone.text().getText().toString());
        assertFalse(alone.isClickable());
        alone.setConnected(true);
        assertEquals(activity.getString(R.string.status_connected), alone.text().getText().toString());
        assertFalse("sin ventana no late", alone.isPulsing());
    }
}
