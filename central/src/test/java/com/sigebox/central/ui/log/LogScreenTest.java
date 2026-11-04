package com.sigebox.central.ui.log;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** El filtro de la bitácora muestra etiquetas en español y sigue filtrando por el código guardado. */
class LogScreenTest {

    @TempDir
    Path dir;

    @Test
    void elFiltroMuestraLaEtiquetaYFiltraPorElCodigo() {
        assertEquals("Todos", LogScreen.typeLabel("Todos"));
        assertEquals("Todos", LogScreen.typeLabel(null));
        assertEquals("Tarjeta de round", LogScreen.typeLabel("SUBMIT_ROUND"));
        assertEquals("APP_START", LogScreen.typeLabel("APP_START"));

        try (var db = Database.open(dir.resolve("sigebox.db"))) {
            var log = new EventLog(db);
            log.append("p-1", "SUBMIT_ROUND", "lugar 2 round 3 TARDIA");
            log.append("p-1", "ROUND_OPENED", "round 3");
            var q = new LogQuery(db);
            assertEquals("SUBMIT_ROUND", q.types().get(1)); // el valor del filtro es el código
            var rows = q.find(null, "SUBMIT_ROUND", 10);
            assertEquals(1, rows.size());
            assertEquals("lugar 2 round 3 TARDIA", rows.get(0).detalle()); // la base guarda el código
        }
    }
}
