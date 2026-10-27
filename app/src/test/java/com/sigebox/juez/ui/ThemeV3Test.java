package com.sigebox.juez.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.juez.R;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** DESIGN.md v3 §3.1: los tokens del código son los de la guía, en claro y en oscuro, y cada pantalla infla con el tema. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ThemeV3Test {

    private static final int[] LAYOUTS = {
        R.layout.activity_main, R.layout.activity_terms, R.layout.activity_join, R.layout.activity_scan,
        R.layout.activity_waiting, R.layout.activity_score, R.layout.activity_late_score,
        R.layout.activity_final_card, R.layout.view_confirm_sheet, R.layout.view_corner_card,
        R.layout.view_top_bar, R.layout.view_top_bar_brand,
    };

    private static Map<Integer, String> light() {
        Map<Integer, String> t = new LinkedHashMap<>();
        t.put(R.color.bg, "#F7F4EF");
        t.put(R.color.surface, "#FFFFFF");
        t.put(R.color.surface_2, "#EFEAE2");
        t.put(R.color.ink, "#1C1A17");
        t.put(R.color.ink_2, "#55585A");
        t.put(R.color.primary, "#1C1A17");
        t.put(R.color.on_primary, "#FFFFFF");
        t.put(R.color.accent, "#7A5A2A");
        t.put(R.color.red, "#9F2241");
        t.put(R.color.on_red, "#FFFFFF");
        t.put(R.color.red_numeral, "#9F2241");
        t.put(R.color.blue, "#266CB4");
        t.put(R.color.on_blue, "#FFFFFF");
        t.put(R.color.blue_numeral, "#266CB4");
        t.put(R.color.connected, "#027A35");
        t.put(R.color.disconnected, "#55585A");
        t.put(R.color.line, "#D9D2C7");
        return t;
    }

    private static Map<Integer, String> dark() {
        Map<Integer, String> t = new LinkedHashMap<>();
        t.put(R.color.bg, "#14120F");
        t.put(R.color.surface, "#1F1C18");
        t.put(R.color.surface_2, "#2A2621");
        t.put(R.color.ink, "#F3EFE8");
        t.put(R.color.ink_2, "#B8B1A6");
        t.put(R.color.primary, "#F3EFE8");
        t.put(R.color.on_primary, "#1C1A17");
        t.put(R.color.accent, "#BC955C");
        t.put(R.color.red, "#E58A9D");
        t.put(R.color.on_red, "#1C1A17");
        t.put(R.color.red_numeral, "#E58A9D");
        t.put(R.color.blue, "#8CB8EE");
        t.put(R.color.on_blue, "#1C1A17");
        t.put(R.color.blue_numeral, "#8CB8EE");
        t.put(R.color.connected, "#4CC97A");
        t.put(R.color.disconnected, "#B8B1A6");
        t.put(R.color.line, "#3A352E");
        return t;
    }

    private static void assertTokens(Map<Integer, String> expected) {
        Context context = ApplicationProvider.getApplicationContext();
        for (Map.Entry<Integer, String> e : expected.entrySet()) {
            int color = ContextCompat.getColor(context, e.getKey());
            String actual = String.format("#%06X", color & 0xFFFFFF);
            assertEquals(context.getResources().getResourceEntryName(e.getKey()), e.getValue(), actual);
            assertEquals("opaco: " + e.getValue(), 0xFF, color >>> 24);
        }
    }

    private static void assertAllInflate() {
        Context themed = new ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_Sigebox);
        FrameLayout parent = new FrameLayout(themed);
        for (int layout : LAYOUTS) {
            assertNotNull(LayoutInflater.from(themed).inflate(layout, parent, false));
        }
    }

    @Test
    public void tokensClaroCantera() {
        assertTokens(light());
    }

    @Test
    @Config(qualifiers = "+night")
    public void tokensOscuroNocheDeArena() {
        assertTokens(dark());
    }

    @Test
    public void cadaPantallaInflaConElTemaV3EnClaro() {
        assertAllInflate();
    }

    @Test
    @Config(qualifiers = "+night")
    public void cadaPantallaInflaConElTemaV3EnOscuro() {
        assertAllInflate();
    }
}
