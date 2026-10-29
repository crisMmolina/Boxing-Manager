package com.sigebox.juez.ui;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

/** DESIGN.md v3 §3: "Ninguna pantalla usa valores sueltos": los colores solo viven en colors.xml. */
public class NoLooseColorsTest {

    private static final Pattern HEX = Pattern.compile("\"#[0-9A-Fa-f]{3,8}\"");
    private static final File RES = new File("src/main/res");

    @Test
    public void ningunColorSueltoFueraDeColorsXml() throws IOException {
        assertTrue("se corre desde app/: " + RES.getAbsolutePath(), RES.isDirectory());
        List<String> found = new ArrayList<>();
        for (String dir : new String[] {"layout", "drawable", "color", "values", "values-night"}) {
            File[] files = new File(RES, dir).listFiles((d, name) -> name.endsWith(".xml"));
            if (files == null) {
                continue;
            }
            for (File file : files) {
                String name = file.getName();
                // Los tokens viven en colors.xml; el ícono del lanzador es su propio arte.
                if (name.equals("colors.xml") || name.startsWith("ic_launcher")) {
                    continue;
                }
                String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                Matcher m = HEX.matcher(text);
                while (m.find()) {
                    found.add(dir + "/" + name + ": " + m.group());
                }
            }
        }
        assertTrue("colores sueltos: " + found, found.isEmpty());
    }

    @Test
    public void ningunColorSueltoEnElCodigo() throws IOException {
        List<String> found = new ArrayList<>();
        Pattern code = Pattern.compile("0x[0-9A-Fa-f]{8}|Color\\.parseColor|Color\\.rgb\\(");
        try (var paths = Files.walk(new File("src/main/java").toPath())) {
            for (var path : (Iterable<java.nio.file.Path>) paths::iterator) {
                if (!path.toString().endsWith(".java")) {
                    continue;
                }
                String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                Matcher m = code.matcher(text);
                while (m.find()) {
                    found.add(path.getFileName() + ": " + m.group());
                }
            }
        }
        assertTrue("colores sueltos en Java: " + found, found.isEmpty());
    }
}
