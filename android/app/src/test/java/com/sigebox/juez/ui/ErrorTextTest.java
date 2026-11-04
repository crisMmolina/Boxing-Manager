package com.sigebox.juez.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.protocol.Ack;
import com.sigebox.juez.R;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Spec 003 §Español en las interfaces: el juez lee una frase con qué hacer, nunca un código. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ErrorTextTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void cadaErrorTieneUnaFraseEnEspanolConQueHacer() {
        Set<String> seen = new HashSet<>();
        for (Ack.Error error : Ack.Error.values()) {
            String text = context.getString(ErrorText.of(error));
            assertTrue(error + " sin frase", text.length() > 20);
            assertFalse(error + " muestra el código", text.contains(error.name()));
            assertFalse(error + " no es una sola frase", text.contains(". "));
            assertTrue(error + " repite otra frase", seen.add(text));
        }
    }

    @Test
    public void lasEsperasDicenEnUnaFraseQueHacer() {
        List<Integer> waits = List.of(R.string.waiting_approval_todo, R.string.not_seated_support,
                R.string.late_body, R.string.score_waiting_start, R.string.score_waiting_next);
        for (int id : waits) {
            String text = context.getString(id);
            assertFalse(text + " no es una sola frase", text.contains(". "));
            assertTrue(text + " no dice qué hacer",
                    text.startsWith("Espera") || text.startsWith("Califica") || text.startsWith("Pide"));
        }
        String open = context.getString(R.string.score_waiting_open, 3);
        assertTrue(open.startsWith("Espera") && !open.contains(". "));
    }

    @Test
    public void ningunaPantallaMuestraElNombreDeUnError() throws Exception {
        Path src = Paths.get("src/main/java/com/sigebox/juez");
        try (Stream<Path> files = Files.walk(src)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith("Activity.java")
                    || p.toString().endsWith("View.java"))::iterator) {
                String code = Files.readString(file);
                assertFalse(file + " usa error.name()", code.matches("(?s).*[eE]rror(\\(\\))?\\.name\\(\\).*"));
            }
        }
    }
}
