package com.sigebox.central.ui.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.ui.theme.FxTest;
import com.sigebox.core.model.EventoBitacora;
import java.nio.file.Path;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Un evento nuevo llega a la tabla solo y no le quita la selección a quien está leyendo. */
class LogScreenRefreshTest {

    @TempDir
    Path dir;

    @BeforeAll
    static void fx() throws Exception {
        FxTest.start();
    }

    @Test
    @SuppressWarnings("unchecked")
    void aNewEventKeepsTheSelectedRow() throws Exception {
        try (var db = Database.open(dir.resolve("sigebox.db"))) {
            var log = new EventLog(db);
            log.append("p-1", "SUBMIT_ROUND", "lugar 1 round 1");
            log.append("p-1", "SUBMIT_ROUND", "lugar 2 round 1");
            var root = FxTest.onFx(() -> LogScreen.build(new LogQuery(db)));
            var scene = FxTest.onFx(() -> new Scene(new Group((Parent) root)));
            var table = FxTest.onFx(() -> (TableView<EventoBitacora>) root.lookup(".table-view"));
            assertNotNull(table);
            var selected = FxTest.onFx(() -> {
                table.getSelectionModel().select(1);
                return table.getSelectionModel().getSelectedItem();
            });

            log.append("p-1", "SUBMIT_ROUND", "lugar 3 round 1");

            long deadline = System.nanoTime() + 1_500_000_000L;
            while (System.nanoTime() < deadline && FxTest.onFx(() -> table.getItems().size()) < 3) {
                Thread.sleep(20);
            }
            assertEquals(3, FxTest.onFx(() -> table.getItems().size()));
            assertEquals(selected, FxTest.onFx(() -> table.getSelectionModel().getSelectedItem()));
            FxTest.onFx(() -> {
                scene.setRoot(new Group());
                return null;
            });
        }
    }
}
