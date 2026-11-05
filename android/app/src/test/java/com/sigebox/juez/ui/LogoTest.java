package com.sigebox.juez.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/** DESIGN.md v4 §4: la carga y el ícono dibujan el logo con los tokens logo_*, no con colores del framework. */
public class LogoTest {

    private static final File DRAWABLE = new File("src/main/res/drawable");

    private static String read(String name) throws IOException {
        return new String(Files.readAllBytes(new File(DRAWABLE, name).toPath()), StandardCharsets.UTF_8);
    }

    private static void assertUsesLogoTokens(String name) throws IOException {
        String text = read(name);
        assertFalse(name + " usa colores del framework", text.contains("@android:color/"));
        for (String token : new String[] {"@color/logo_negro", "@color/logo_rojo", "@color/logo_claro"}) {
            assertTrue(name + " no usa " + token, text.contains(token));
        }
    }

    @Test
    public void laCargaUsaLosTokensDelLogo() throws IOException {
        assertUsesLogoTokens("splash.xml");
    }

    @Test
    public void laBarraUsaLosTokensDelLogo() throws IOException {
        assertUsesLogoTokens("logo.xml");
    }

    @Test
    public void elIconoEsElLogoSobreNegroConFileteDorado() throws IOException {
        assertUsesLogoTokens("ic_launcher_foreground.xml");
        String background = read("ic_launcher_background.xml");
        assertTrue("fondo #111110", background.contains("#111110"));
        assertTrue("filete dorado", background.contains("android:strokeColor=\"#C9A35E\""));
    }
}
