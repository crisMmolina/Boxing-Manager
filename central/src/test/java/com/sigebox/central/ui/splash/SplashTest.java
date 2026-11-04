package com.sigebox.central.ui.splash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.central.ui.theme.Theme;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Pantalla de carga: logo, al menos 600 ms, el error en español y "Reintentar" (spec 004 §Arranque). */
class SplashTest {

    @AfterEach
    void followSystemMotion() {
        Theme.setReducedMotion(null);
    }

    private static boolean hasText(Node root, String text) {
        if (root instanceof Label l && text.equals(l.getText())) {
            return true;
        }
        if (root instanceof Parent p) {
            for (var c : p.getChildrenUnmodifiable()) {
                if (hasText(c, text)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void showsTheLogoAndWaitsAtLeast600msEvenIfLoadingIsInstant() throws Exception {
        var ready = new CountDownLatch(1);
        var started = new AtomicLong();
        var elapsed = new AtomicLong();
        var value = new AtomicReference<String>();
        var splash = FxTest.onFx(() -> {
            var s = new Splash<String>(() -> "base", v -> {
                value.set(v);
                elapsed.set(System.nanoTime() - started.get());
                ready.countDown();
            });
            assertTrue(s.getStyleClass().contains("splash"));
            assertNotNull(s.lookup(".logo"), "logo de guantes");
            assertTrue(hasText(s, "SIGEBOX"));
            assertTrue(hasText(s, Splash.LOADING));
            started.set(System.nanoTime());
            s.start();
            return s;
        });
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        assertEquals("base", value.get());
        long ms = TimeUnit.NANOSECONDS.toMillis(elapsed.get());
        assertTrue(ms >= Splash.MIN.toMillis() - 20, "mínimo 600 ms, fueron " + ms);
        assertTrue(ms < Splash.MAX.toMillis(), "máximo 3 s sin error, fueron " + ms);
        assertNull(FxTest.onFx(splash::errorText));
    }

    @Test
    void anErrorStaysOnScreenInSpanishAndRetryLoadsAgain() throws Exception {
        var attempts = new AtomicInteger();
        var ready = new CountDownLatch(1);
        var errors = new ArrayList<String>();
        var splash = FxTest.onFx(() -> {
            var s = new Splash<String>(() -> {
                if (attempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("SQLITE_CANTOPEN: unable to open database file");
                }
                return "ok";
            }, v -> ready.countDown());
            s.start();
            return s;
        });
        String error = null;
        for (int i = 0; i < 50 && error == null; i++) {
            Thread.sleep(100);
            error = FxTest.onFx(splash::errorText);
        }
        assertEquals(Splash.ERROR, error);
        assertFalse(error.contains("SQLITE"), "sin códigos internos");
        assertTrue(error.startsWith("No se pudo abrir"), error);
        FxTest.onFx(() -> {
            assertTrue(splash.retryButton().isVisible());
            assertTrue(splash.retryButton().getStyleClass().contains("primary"));
            assertEquals("Reintentar", splash.retryButton().getText());
            errors.add(splash.errorText());
            splash.retryButton().fire();
            return null;
        });
        assertTrue(ready.await(5, TimeUnit.SECONDS), "Reintentar vuelve a cargar");
        assertEquals(2, attempts.get());
        assertNull(FxTest.onFx(splash::errorText));
    }

    @Test
    void theLogoSwingsAndStaysStillWithReducedMotion() throws Exception {
        FxTest.onFx(() -> {
            Theme.setReducedMotion(false);
            var moving = new Splash<String>(() -> "x", v -> { });
            assertNotNull(moving.swing(), "balanceo desde los cordones");
            moving.stopMotion();
            Theme.setReducedMotion(true);
            var still = new Splash<String>(() -> "x", v -> { });
            assertNull(still.swing(), "con movimiento reducido el logo queda quieto");
            return null;
        });
    }
}
