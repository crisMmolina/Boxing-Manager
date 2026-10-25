package com.sigebox.central.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.ui.log.LogQuery;
import com.sigebox.central.ui.terms.TermsGate;
import com.sigebox.central.ui.terms.TermsScreen;
import com.sigebox.core.model.EventoBitacora;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Lógica de términos y bitácora, sin arrancar JavaFX. */
class TermsAndLogTest {

    @TempDir
    Path dir;

    private Database open() {
        return Database.open(dir.resolve("sigebox.db"));
    }

    @Test
    void withoutSavedAcceptanceTheTermsComeFirst() {
        try (var db = open()) {
            var gate = new TermsGate(db);
            assertTrue(gate.mustShow());
            assertEquals(TermsScreen.ID, gate.firstScreen("inicio"));

            gate.decline(); // rechazar queda registrado y no cuenta como aceptación
            assertTrue(gate.mustShow());
            gate.accept();
            assertFalse(gate.mustShow());
            assertEquals("inicio", gate.firstScreen("inicio"));
        }
        try (var db = open()) {
            assertFalse(new TermsGate(db).mustShow()); // sobrevive al reinicio
            var types = new LogQuery(db).find(LogQuery.GLOBAL, null, 10).stream().map(EventoBitacora::tipo).toList();
            assertEquals(List.of(TermsGate.ACCEPTED, TermsGate.DECLINED), types);
        }
    }

    @Test
    void acceptanceOfAnotherVersionDoesNotCount() {
        try (var db = open()) {
            new EventLog(db).append(null, TermsGate.ACCEPTED, "v0");
            new EventLog(db).append("pelea-1", TermsGate.ACCEPTED, TermsGate.VERSION); // no es global
            assertTrue(new TermsGate(db).mustShow());
        }
    }

    @Test
    void termsCoverPrivacyAndBetting() {
        assertTrue(TermsScreen.TEXT.contains("Aviso de privacidad"));
        assertTrue(TermsScreen.TEXT.contains("Prohibición de apuestas"));
        assertTrue(TermsScreen.TEXT.contains("no se envían a internet"));
    }

    @Test
    void logFiltersByFightAndType() {
        try (var db = open()) {
            var log = new EventLog(db);
            log.append("p-1", "ROUND_OPENED", "round 1");
            log.append("p-1", "SUBMIT_ROUND", "lugar 1 round 1 ENVIADA");
            log.append("p-2", "ROUND_OPENED", "round 1");
            log.append(null, "APP_START", null);
            var q = new LogQuery(db);

            assertEquals(4, q.find(null, null, 100).size());
            assertEquals(List.of("SUBMIT_ROUND", "ROUND_OPENED"),
                    q.find("p-1", null, 100).stream().map(EventoBitacora::tipo).toList()); // recientes primero
            assertEquals(2, q.find(null, "ROUND_OPENED", 100).size());
            assertEquals(1, q.find("p-2", "ROUND_OPENED", 100).size());
            assertEquals(List.of("APP_START"), q.find(LogQuery.GLOBAL, null, 100).stream()
                    .map(EventoBitacora::tipo).toList());
            assertEquals(1, q.find(null, null, 1).size());
            assertEquals(List.of(LogQuery.GLOBAL, "p-1", "p-2"), q.fights());
            assertEquals(List.of("APP_START", "ROUND_OPENED", "SUBMIT_ROUND"), q.types());
            assertTrue(q.find("p-1' OR '1'='1", null, 100).isEmpty()); // parámetros, no texto pegado
        }
    }
}
