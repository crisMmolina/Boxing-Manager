package com.sigebox.central.ui.theme;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;

/** Arranca JavaFX una vez y corre trabajo en su hilo (para pruebas que construyen nodos). */
public final class FxTest {

    private FxTest() {
    }

    public static void start() throws InterruptedException {
        var ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyStarted) {
            ready.countDown();
        }
        Platform.setImplicitExit(false);
        ready.await(10, TimeUnit.SECONDS);
    }

    public static <T> T onFx(Callable<T> work) throws Exception {
        start();
        var result = new AtomicReference<T>();
        var error = new AtomicReference<Exception>();
        var done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(work.call());
            } catch (Exception e) {
                error.set(e);
            } finally {
                done.countDown();
            }
        });
        if (!done.await(20, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX no respondió");
        }
        if (error.get() != null) {
            throw error.get();
        }
        return result.get();
    }
}
