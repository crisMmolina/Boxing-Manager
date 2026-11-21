package com.sigebox.juez.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.sigebox.juez.R;
import com.sigebox.juez.net.CentralSilence;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowToast;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** AND-032: el diálogo «¿La pelea sigue?» con reloj falso, sus dos botones y su silencio con round abierto. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class StillThereDialogTest {

    private final AtomicLong now = new AtomicLong(1_000_000L);
    private final AtomicBoolean scoring = new AtomicBoolean(false);
    private final AtomicInteger leaves = new AtomicInteger();
    private int kept;
    private Activity activity;
    private CentralSilence silence;
    private StillThereDialog still;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setTheme(R.style.Theme_Sigebox);
        silence = new CentralSilence(now::get);
        still = new StillThereDialog(activity, scoring::get, silence, () -> {
            leaves.incrementAndGet();
            return kept;
        }, Runnable::run);
    }

    private void tenMinutesOfSilence() {
        now.addAndGet(CentralSilence.ASK_AFTER_MS);
        still.start();
        ShadowLooper.idleMainLooper(StillThereDialog.CHECK_EVERY_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private static AlertDialog shown() {
        Dialog d = ShadowDialog.getLatestDialog();
        return d instanceof AlertDialog a && a.isShowing() ? a : null;
    }

    @Test
    public void trasDiezMinutosSinLaCentralPreguntaSiLaPeleaSigue() {
        tenMinutesOfSilence();
        AlertDialog dialog = shown();
        assertNotNull("aparece el diálogo", dialog);
        assertEquals("¿La pelea sigue?", ((TextView) dialog.findViewById(androidx.appcompat.R.id.alertTitle)).getText().toString());
        assertEquals("Seguir esperando", dialog.getButton(DialogInterface.BUTTON_POSITIVE).getText().toString());
        assertEquals("Salir de la pelea", dialog.getButton(DialogInterface.BUTTON_NEGATIVE).getText().toString());
        still.stop();
    }

    @Test
    public void conElJuezCalificandoNoPregunta() {
        scoring.set(true);
        tenMinutesOfSilence();
        assertNull("no interrumpe al juez", shown());
        still.stop();
    }

    @Test
    public void seguirEsperandoCierraElDialogoYReiniciaLaCuenta() {
        tenMinutesOfSilence();
        shown().getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();
        assertNull(shown());
        assertFalse("la cuenta empieza de nuevo", silence.shouldAsk(false));
        assertEquals(0, leaves.get());
        still.stop();
    }

    @Test
    public void salirSinPendientesVuelveAUnirme() {
        tenMinutesOfSilence();
        shown().getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        ShadowLooper.idleMainLooper();
        assertEquals(1, leaves.get());
        Intent next = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull(next);
        assertEquals("com.sigebox.juez.join.JoinActivity", next.getComponent().getClassName());
        assertTrue(activity.isFinishing());
        assertNull("sin pendientes no hay aviso", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void salirConPendientesDiceCuantasQuedanGuardadas() {
        kept = 2;
        tenMinutesOfSilence();
        shown().getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        ShadowLooper.idleMainLooper();
        String toast = ShadowToast.getTextOfLatestToast();
        assertNotNull(toast);
        assertTrue(toast, toast.contains("2"));
    }
}
