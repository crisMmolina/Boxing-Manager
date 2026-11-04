package com.sigebox.juez.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.sigebox.juez.R;
import com.sigebox.juez.ui.TopBar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ConnectionHelpTest {

    private Activity activity;
    private View bar;

    @Before
    public void setUp() {
        ConnectionHelp.hostSource = context -> "192.168.43.10";
        ConnectionHelp.executor = Runnable::run;
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setTheme(R.style.Theme_Sigebox);
        FrameLayout root = new FrameLayout(activity);
        bar = LayoutInflater.from(activity).inflate(R.layout.view_top_bar, root, false);
        root.addView(bar);
        activity.setContentView(root);
    }

    @After
    public void tearDown() {
        ConnectionHelp.reset();
    }

    @Test
    public void sinConexionElSignoSeVeDestacadoYAbreLaGuia() {
        TopBar.bind(bar, false, activity.getString(R.string.status_disconnected));

        View help = bar.findViewById(R.id.top_help);
        assertNotNull("el ? está en la barra", help);
        assertEquals(View.VISIBLE, help.getVisibility());
        assertTrue("destacado sin conexión", help.isSelected());
        assertEquals(activity.getString(R.string.help_a11y), help.getContentDescription());

        help.performClick();
        ShadowLooper.idleMainLooper();

        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull("abre la guía", dialog);
        assertTrue(dialog.isShowing());
        assertEquals(activity.getString(R.string.help_title),
                ((TextView) dialog.findViewById(R.id.help_title)).getText().toString());
        assertEquals(activity.getString(R.string.help_host, "192.168.43.10"),
                ((TextView) dialog.findViewById(R.id.help_host)).getText().toString());
        assertEquals(activity.getString(R.string.help_state, activity.getString(R.string.status_disconnected)),
                ((TextView) dialog.findViewById(R.id.help_state)).getText().toString());
        String steps = ((TextView) dialog.findViewById(R.id.help_phone_steps)).getText().toString();
        assertTrue(steps, steps.contains("Revisa el Wi‑Fi."));
        assertTrue(steps, steps.contains("¿Sigue sin conectar?"));
        String central = ((TextView) dialog.findViewById(R.id.help_central_steps)).getText().toString();
        assertTrue(central, central.contains("BoxCentral WS 8765"));
        assertFalse("sin enlaces a internet", (steps + central).contains("http"));

        dialog.findViewById(R.id.help_done).performClick();
        assertFalse("Listo cierra", dialog.isShowing());
    }

    @Test
    public void conConexionElSignoSigueSinDestacar() {
        TopBar.bind(bar, true, "Juez 2");
        TopBar.bind(bar, true, "Juez 2");

        View help = bar.findViewById(R.id.top_help);
        assertEquals(View.VISIBLE, help.getVisibility());
        assertFalse(help.isSelected());
        assertEquals("un solo ?", 1, countHelp((android.view.ViewGroup) bar));

        help.performClick();
        assertEquals(activity.getString(R.string.help_state, activity.getString(R.string.status_connected)),
                ((TextView) ShadowDialog.getLatestDialog().findViewById(R.id.help_state)).getText().toString());
    }

    @Test
    public void sinLugarGuardadoLoDice() {
        ConnectionHelp.hostSource = context -> null;
        TopBar.bind(bar, false, activity.getString(R.string.status_disconnected));
        bar.findViewById(R.id.top_help).performClick();
        ShadowLooper.idleMainLooper();
        assertEquals(activity.getString(R.string.help_host_none),
                ((TextView) ShadowDialog.getLatestDialog().findViewById(R.id.help_host)).getText().toString());
    }

    private static int countHelp(android.view.ViewGroup group) {
        int n = 0;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (group.getChildAt(i).getId() == R.id.top_help) {
                n++;
            }
        }
        return n;
    }
}
