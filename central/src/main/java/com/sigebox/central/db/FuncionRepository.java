package com.sigebox.central.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Tablas {@code funcion}, {@code funcion_card} y {@code funcion_log} (V2, spec 002; nombre en V3). */
public final class FuncionRepository {

    /** Fila guardada de la función; {@code nombre} nulo = sin nombre propio. */
    public record FuncionRow(String id, String nombre, String fecha, int baseRounds, int baseJueces) {}

    /** Fila guardada de la cartelera; {@code rounds}/{@code jueces} nulos = usa la base. */
    public record CardRow(int orden, String roja, String azul, Integer rounds, Integer jueces, boolean saltada,
            String peleaId) {}

    private final Database db;

    public FuncionRepository(Database db) {
        this.db = db;
    }

    public void save(FuncionRow f) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement("""
                    INSERT INTO funcion (id, nombre, date, base_rounds, base_judges, created_ms) VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                ps.setString(1, f.id());
                ps.setString(2, f.nombre());
                ps.setString(3, f.fecha());
                ps.setInt(4, f.baseRounds());
                ps.setInt(5, f.baseJueces());
                ps.setLong(6, System.currentTimeMillis());
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** Cambia los rounds y jueces base del evento (spec 005). */
    public void updateBase(String id, int baseRounds, int baseJueces) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement("UPDATE funcion SET base_rounds = ?, base_judges = ? WHERE id = ?")) {
                ps.setInt(1, baseRounds);
                ps.setInt(2, baseJueces);
                ps.setString(3, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public Optional<FuncionRow> find(String id) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM funcion WHERE id = ?")) {
                ps.setString(1, id);
                try (var rs = ps.executeQuery()) {
                    return rs.next()
                            ? Optional.of(new FuncionRow(rs.getString("id"), rs.getString("nombre"), rs.getString("date"),
                                    rs.getInt("base_rounds"), rs.getInt("base_judges")))
                            : Optional.empty();
                }
            }
        });
    }

    /** Cartelera en orden. */
    public List<CardRow> card(String funcionId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM funcion_card WHERE funcion_id = ? ORDER BY position")) {
                ps.setString(1, funcionId);
                try (var rs = ps.executeQuery()) {
                    var out = new ArrayList<CardRow>();
                    while (rs.next()) {
                        out.add(new CardRow(rs.getInt("position"), rs.getString("red_corner"),
                                rs.getString("blue_corner"), integer(rs, "rounds"), integer(rs, "judges"),
                                rs.getInt("skipped") == 1, rs.getString("fight_id")));
                    }
                    return out;
                }
            }
        });
    }

    /** Reemplaza la cartelera completa en una transacción; {@code orden} se renumera 1..n. */
    public void replaceCard(String funcionId, List<CardRow> rows) {
        db.inTransaction(c -> {
            try (var del = c.prepareStatement("DELETE FROM funcion_card WHERE funcion_id = ?")) {
                del.setString(1, funcionId);
                del.executeUpdate();
            }
            int n = 1;
            for (var r : rows) {
                insert(c, funcionId, n++, r);
            }
            return null;
        });
    }

    public void log(String funcionId, String tipo, String detalle) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement(
                    "INSERT INTO funcion_log (funcion_id, type, detail, time_ms) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, funcionId);
                ps.setString(2, tipo);
                ps.setString(3, detalle);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** Tipos de la bitácora de la función, en orden de llegada. */
    public List<String> bitacora(String funcionId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT type FROM funcion_log WHERE funcion_id = ? ORDER BY id")) {
                ps.setString(1, funcionId);
                try (var rs = ps.executeQuery()) {
                    var out = new ArrayList<String>();
                    while (rs.next()) {
                        out.add(rs.getString(1));
                    }
                    return out;
                }
            }
        });
    }

    private static void insert(Connection c, String funcionId, int orden, CardRow r) throws SQLException {
        try (var ps = c.prepareStatement("""
                INSERT INTO funcion_card (funcion_id, position, red_corner, blue_corner, rounds, judges, skipped, fight_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setString(1, funcionId);
            ps.setInt(2, orden);
            ps.setString(3, r.roja());
            ps.setString(4, r.azul());
            setInteger(ps, 5, r.rounds());
            setInteger(ps, 6, r.jueces());
            ps.setInt(7, r.saltada() ? 1 : 0);
            ps.setString(8, r.peleaId());
            ps.executeUpdate();
        }
    }

    private static Integer integer(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    private static void setInteger(java.sql.PreparedStatement ps, int i, Integer v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.INTEGER);
        } else {
            ps.setInt(i, v);
        }
    }
}
