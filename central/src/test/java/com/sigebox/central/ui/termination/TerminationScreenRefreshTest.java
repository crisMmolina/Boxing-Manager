package com.sigebox.central.ui.termination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.service.TerminationService;
import com.sigebox.central.ui.theme.FxTest;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Al aparecer la pelea se habilita "Terminar pelea" sin tocar lo que ya se escribió en el formulario. */
class TerminationScreenRefreshTest {

    @TempDir
    Path dir;

    @BeforeAll
    static void fx() throws Exception {
        FxTest.start();
    }

    private static Button terminar(Node root) {
        return root.lookupAll(".button").stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> "Terminar pelea".equals(b.getText())).findFirst().orElseThrow();
    }

    @Test
    void aFightEnablesTheButtonAndKeepsTheForm() throws Exception {
        try (var db = Database.open(dir.resolve("sigebox.db"))) {
            var fight = new AtomicReference<String>();
            var service = new TerminationService(db, (f, reason) -> { }, () -> 5_000L);
            var root = FxTest.onFx(() -> TerminationScreen.build(service, fight::get));
            var scene = FxTest.onFx(() -> new Scene(new Group((Parent) root)));
            assertTrue(FxTest.onFx(() -> terminar(root).isDisable()));
            var notes = FxTest.onFx(() -> {
                var area = (TextArea) root.lookup(".text-area");
                area.setText("Corte en la ceja");
                return area;
            });

            fight.set("pelea-1");

            long deadline = System.nanoTime() + 1_500_000_000L;
            while (System.nanoTime() < deadline && FxTest.onFx(() -> terminar(root).isDisable())) {
                Thread.sleep(20);
            }
            assertFalse(FxTest.onFx(() -> terminar(root).isDisable()));
            assertEquals("Corte en la ceja", FxTest.onFx(notes::getText));
            FxTest.onFx(() -> {
                scene.setRoot(new Group());
                return null;
            });
        }
    }
}
