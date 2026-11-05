package com.sigebox.juez.ui;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.juez.R;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** DESIGN.md v4 §3.2: las parejas de la guía pasan AA en claro y en oscuro: 4.5 el texto, 3 los elementos de interfaz. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ContrastV4Test {

    private static final double TEXT = 4.5;
    private static final double UI = 3.0;

    private static final int[][] TEXT_PAIRS = {
        {R.color.ink, R.color.bg}, {R.color.ink, R.color.surface}, {R.color.ink, R.color.surface_2},
        {R.color.ink_2, R.color.bg}, {R.color.ink_2, R.color.surface}, {R.color.ink_2, R.color.surface_2},
        {R.color.on_primary, R.color.primary}, {R.color.ink, R.color.accent_soft},
        {R.color.on_bar, R.color.bar}, {R.color.on_bar_2, R.color.bar}, {R.color.bar_accent, R.color.bar},
        {R.color.on_red, R.color.red}, {R.color.on_blue, R.color.blue},
        {R.color.red_numeral, R.color.surface}, {R.color.red_numeral, R.color.bg},
        {R.color.blue_numeral, R.color.surface}, {R.color.blue_numeral, R.color.bg},
        {R.color.accent, R.color.bg}, {R.color.accent, R.color.surface}, {R.color.accent, R.color.surface_2},
    };

    private static final int[][] UI_PAIRS = {
        {R.color.primary, R.color.bg}, {R.color.primary, R.color.surface},
        {R.color.field_border, R.color.surface},
        {R.color.red, R.color.surface}, {R.color.blue, R.color.surface},
        {R.color.accent, R.color.surface_2},
        {R.color.connected, R.color.surface}, {R.color.disconnected, R.color.surface},
    };

    private static void collectFailures(int[][] pairs, double minimum, List<String> failures) {
        Context context = ApplicationProvider.getApplicationContext();
        for (int[] pair : pairs) {
            double ratio = ColorUtils.calculateContrast(
                    ContextCompat.getColor(context, pair[0]), ContextCompat.getColor(context, pair[1]));
            if (ratio < minimum) {
                failures.add(String.format("%s sobre %s: %.2f < %.1f",
                        context.getResources().getResourceEntryName(pair[0]),
                        context.getResources().getResourceEntryName(pair[1]), ratio, minimum));
            }
        }
    }

    private static void assertAa() {
        List<String> failures = new ArrayList<>();
        collectFailures(TEXT_PAIRS, TEXT, failures);
        collectFailures(UI_PAIRS, UI, failures);
        assertTrue(failures.toString(), failures.isEmpty());
    }

    @Test
    public void lasParejasPasanAaEnClaro() {
        assertAa();
    }

    @Test
    @Config(qualifiers = "+night")
    public void lasParejasPasanAaEnOscuro() {
        assertAa();
    }
}
