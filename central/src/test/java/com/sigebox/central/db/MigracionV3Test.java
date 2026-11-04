package com.sigebox.central.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** V3 (plan 004) solo agrega: una base V2 con datos los conserva y gana el nombre y los descuentos. */
class MigracionV3Test {

    @TempDir
    Path dir;

    @Test
    void unaBaseV2ConDatosLosConservaYGanaNombreYDescuentos() throws Exception {
        var file = dir.resolve("v2.db");
        baseV2(file);

        try (var db = Database.open(file)) {
            assertEquals(Database.MIGRATIONS.size(), db.schemaVersion());
            var repo = new FuncionRepository(db);
            var f = repo.find("F1").orElseThrow();
            assertEquals("2026-05-01", f.fecha());
            assertEquals(10, f.baseRounds());
            assertEquals(3, f.baseJueces());
            assertNull(f.nombre());
            var card = repo.card("F1");
            assertEquals(1, card.size());
            assertEquals("Ana", card.get(0).roja());
            assertEquals("Bea", card.get(0).azul());

            db.inTransaction(c -> {
                try (var st = c.createStatement()) {
                    st.executeUpdate("""
                            INSERT INTO descuento (fight_id, round, esquina, created_ms) VALUES ('P1', 2, 'ROJA', 1)
                            """);
                    try (var rs = st.executeQuery("SELECT puntos, anulado FROM descuento WHERE fight_id = 'P1'")) {
                        rs.next();
                        assertEquals(1, rs.getInt("puntos"));
                        assertEquals(0, rs.getInt("anulado"));
                    }
                }
                return null;
            });
        }
    }

    @Test
    void elDescuentoSoloAceptaRoundEsquinaYPuntosValidos() throws Exception {
        try (var db = Database.open(dir.resolve("nueva.db"))) {
            for (var valores : new String[] {"'P1', 13, 'ROJA', 1, 1", "'P1', 1, 'VERDE', 1, 1", "'P1', 1, 'AZUL', 2, 1"}) {
                assertThrows(RuntimeException.class, () -> db.inTransaction(c -> {
                    try (var st = c.createStatement()) {
                        st.executeUpdate("INSERT INTO descuento (fight_id, round, esquina, puntos, created_ms) VALUES ("
                                + valores + ")");
                    }
                    return null;
                }), valores);
            }
        }
    }

    /** Deja en disco una base como la dejaba la versión con V1 y V2, con una función y su cartelera. */
    private static void baseV2(Path file) throws SQLException, IOException {
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file); var st = c.createStatement()) {
            aplicar(c, "V1__init.sql");
            aplicar(c, "V2__funcion.sql");
            st.executeUpdate("""
                    CREATE TABLE schema_version (
                        version INTEGER PRIMARY KEY, name TEXT NOT NULL, applied_ms INTEGER NOT NULL)
                    """);
            st.executeUpdate("INSERT INTO schema_version VALUES (1, 'V1__init.sql', 1), (2, 'V2__funcion.sql', 1)");
            st.executeUpdate("""
                    INSERT INTO funcion (id, date, base_rounds, base_judges, created_ms)
                    VALUES ('F1', '2026-05-01', 10, 3, 1)
                    """);
            st.executeUpdate("""
                    INSERT INTO funcion_card (funcion_id, position, red_corner, blue_corner, skipped)
                    VALUES ('F1', 1, 'Ana', 'Bea', 0)
                    """);
        }
    }

    private static void aplicar(Connection c, String name) throws SQLException, IOException {
        try (var in = Database.class.getResourceAsStream("/db/" + name); var st = c.createStatement()) {
            for (var sql : Database.statements(new String(in.readAllBytes(), StandardCharsets.UTF_8))) {
                st.executeUpdate(sql);
            }
        }
    }
}
