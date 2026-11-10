package com.sigebox.juez.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.juez.R;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** DESIGN.md v5 §3.8 y §3.9: tokens nuevos (mismos nombres que la central), radios e íconos del juez. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ThemeV5Test {

    private static final int[] ICONS = {
        R.drawable.ic_plus, R.drawable.ic_minus, R.drawable.ic_check, R.drawable.ic_arrow_right,
        R.drawable.ic_wifi, R.drawable.ic_wifi_off, R.drawable.ic_circle_question_mark, R.drawable.ic_qr_code,
        R.drawable.ic_clock, R.drawable.ic_rotate_ccw, R.drawable.ic_circle_alert, R.drawable.ic_info,
    };

    private static Map<Integer, String> light() {
        Map<Integer, String> t = new LinkedHashMap<>();
        t.put(R.color.nav, "#111110");
        t.put(R.color.nav_ink, "#B9B3A8");
        t.put(R.color.nav_ink_dim, "#8F897E");
        t.put(R.color.nav_active, "#2A2418");
        t.put(R.color.success_soft, "#E3F3E8");
        t.put(R.color.success_ink, "#027A35");
        t.put(R.color.pending_ink, "#5C431E");
        t.put(R.color.ink_3, "#8F897E");
        t.put(R.color.ring_track, "#E9E6DF");
        return t;
    }

    private static Map<Integer, String> dark() {
        Map<Integer, String> t = new LinkedHashMap<>();
        t.put(R.color.nav, "#0A0A09");
        t.put(R.color.nav_ink, "#B9B3A8");
        t.put(R.color.nav_ink_dim, "#8F897E");
        t.put(R.color.nav_active, "#2A2418");
        t.put(R.color.success_soft, "#16301F");
        t.put(R.color.success_ink, "#7FE0A2");
        t.put(R.color.pending_ink, "#E9C978");
        t.put(R.color.ink_3, "#8F897E");
        t.put(R.color.ring_track, "#24221F");
        return t;
    }

    private static void assertTokens(Map<Integer, String> expected) {
        Context context = ApplicationProvider.getApplicationContext();
        for (Map.Entry<Integer, String> e : expected.entrySet()) {
            int color = ContextCompat.getColor(context, e.getKey());
            assertEquals(context.getResources().getResourceEntryName(e.getKey()), e.getValue(),
                    String.format("#%06X", color & 0xFFFFFF));
            assertEquals("opaco: " + e.getValue(), 0xFF, color >>> 24);
        }
    }

    @Test
    public void tokensV5Claro() {
        assertTokens(light());
    }

    @Test
    @Config(qualifiers = "+night")
    public void tokensV5Oscuro() {
        assertTokens(dark());
    }

    @Test
    public void radiosV5() {
        Context context = ApplicationProvider.getApplicationContext();
        float density = context.getResources().getDisplayMetrics().density;
        assertEquals(8 * density, context.getResources().getDimension(R.dimen.r_sm), 0.01f);
        assertEquals(12 * density, context.getResources().getDimension(R.dimen.r_md), 0.01f);
        assertEquals(16 * density, context.getResources().getDimension(R.dimen.r_lg), 0.01f);
        assertEquals(20 * density, context.getResources().getDimension(R.dimen.r_xl), 0.01f);
    }

    @Test
    public void iconosDe24ConElTema() {
        assertIcons();
    }

    @Test
    @Config(qualifiers = "+night")
    public void iconosDe24ConElTemaOscuro() {
        assertIcons();
    }

    private static void assertIcons() {
        Context themed = new ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_Sigebox);
        float density = themed.getResources().getDisplayMetrics().density;
        for (int id : ICONS) {
            Drawable icon = ContextCompat.getDrawable(themed, id);
            String name = themed.getResources().getResourceEntryName(id);
            assertNotNull(name, icon);
            assertEquals(name, Math.round(24 * density), icon.getIntrinsicWidth());
            assertEquals(name, Math.round(24 * density), icon.getIntrinsicHeight());
        }
    }
}
