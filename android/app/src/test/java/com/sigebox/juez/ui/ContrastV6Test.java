package com.sigebox.juez.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.TypedArray;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * AND-027 (spec 007, v6 «Esquina»): la superficie arena (negra en los dos temas) y sus pares pasan AA, y las
 * píldoras miden al menos 56 dp con radio completo. Los tokens se buscan por nombre para que falte uno sea un
 * fallo de la prueba y no de compilación.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ContrastV6Test {

    private static final double TEXT = 4.5;
    private static final double UI = 3.0;

    /** Texto normal sobre arena. */
    private static final String[][] TEXT_PAIRS = {
        {"on_arena", "arena"}, {"on_arena", "arena_2"},
        {"on_arena_2", "arena"}, {"on_arena_2", "arena_2"},
        {"arena_red", "arena_2"}, {"arena_blue", "arena_2"},
        {"arena_red", "arena"}, {"arena_blue", "arena"},
        {"ring_current", "arena"},
        {"on_primary", "primary"},
    };

    /** Elementos de interfaz sobre arena (segmentos del anillo, borde de la píldora de contorno, botones − +). */
    private static final String[][] UI_PAIRS = {
        {"ring_done", "arena"}, {"ring_current", "arena"},
        {"arena_line", "arena"}, {"arena_control", "arena_2"},
    };

    private static int color(Context context, String name, List<String> failures) {
        int id = context.getResources().getIdentifier(name, "color", context.getPackageName());
        if (id == 0) {
            failures.add("falta el token " + name);
            return 0;
        }
        return ContextCompat.getColor(context, id);
    }

    private static void collect(String[][] pairs, double minimum, List<String> failures) {
        Context context = ApplicationProvider.getApplicationContext();
        for (String[] pair : pairs) {
            int before = failures.size();
            int fg = color(context, pair[0], failures);
            int bg = color(context, pair[1], failures);
            if (failures.size() > before) {
                continue;
            }
            double ratio = ColorUtils.calculateContrast(fg, ColorUtils.setAlphaComponent(bg, 255));
            if (ratio < minimum) {
                failures.add(String.format("%s sobre %s: %.2f < %.1f", pair[0], pair[1], ratio, minimum));
            }
        }
    }

    private static void assertAa() {
        List<String> failures = new ArrayList<>();
        collect(TEXT_PAIRS, TEXT, failures);
        collect(UI_PAIRS, UI, failures);
        assertTrue(failures.toString(), failures.isEmpty());
    }

    @Test
    public void lasParejasArenaPasanAaEnClaro() {
        assertAa();
    }

    @Test
    @Config(qualifiers = "+night")
    public void lasParejasArenaPasanAaEnOscuro() {
        assertAa();
    }

    @Test
    public void arenaEsNegraEnLosDosTemas() {
        List<String> failures = new ArrayList<>();
        int arena = color(ApplicationProvider.getApplicationContext(), "arena", failures);
        assertTrue(failures.toString(), failures.isEmpty());
        assertTrue("arena debe ser casi negra", ColorUtils.calculateLuminance(arena) < 0.01);
    }

    @Test
    public void lasPildorasMidenAlMenos56dpYSonRedondas() {
        Context context = ApplicationProvider.getApplicationContext();
        for (String name : new String[] {"Widget.Sigebox.Pill", "Widget.Sigebox.Pill.Outline"}) {
            int style = context.getResources().getIdentifier(name, "style", context.getPackageName());
            if (style == 0) {
                style = context.getResources().getIdentifier(name.replace('.', '_'), "style",
                        context.getPackageName());
            }
            assertTrue("falta el estilo " + name, style != 0);
            TypedArray a = context.obtainStyledAttributes(style, new int[] {android.R.attr.minHeight});
            float min = a.getDimension(0, 0);
            a.recycle();
            float dp = context.getResources().getDisplayMetrics().density;
            assertTrue(name + " mide " + (min / dp) + " dp", min >= 56 * dp);
        }
        int full = context.getResources().getIdentifier("pill_radius", "dimen", context.getPackageName());
        assertTrue("falta pill_radius", full != 0);
        assertEquals(28f * context.getResources().getDisplayMetrics().density,
                context.getResources().getDimension(full), 0.5f);
    }
}
