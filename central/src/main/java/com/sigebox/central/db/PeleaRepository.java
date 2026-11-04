package com.sigebox.central.db;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.Pelea;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Tabla {@code fight}. */
public final class PeleaRepository {

    private final Database db;

    public PeleaRepository(Database db) {
        this.db = db;
    }

    /** Inserta o reemplaza la pelea (la hora de creación se conserva). */
    public void save(Pelea p) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement("""
                    INSERT INTO fight (id, red_corner, blue_corner, rounds, judges, state, date, created_ms)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET red_corner = excluded.red_corner,
                        blue_corner = excluded.blue_corner, rounds = excluded.rounds,
                        judges = excluded.judges, state = excluded.state, date = excluded.date
                    """)) {
                ps.setString(1, p.id());
                ps.setString(2, p.esquinaRoja());
                ps.setString(3, p.esquinaAzul());
                ps.setInt(4, p.rounds());
                ps.setInt(5, p.jueces());
                Sql.setEnum(ps, 6, p.estado());
                ps.setString(7, p.fecha());
                ps.setLong(8, System.currentTimeMillis());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void updateEstado(String id, EstadoPelea estado) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement("UPDATE fight SET state = ? WHERE id = ?")) {
                Sql.setEnum(ps, 1, estado);
                ps.setString(2, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public Optional<Pelea> find(String id) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM fight WHERE id = ?")) {
                ps.setString(1, id);
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.<Pelea>empty();
                }
            }
        });
    }

    /** La pelea más reciente que no está cerrada (la central admite una activa a la vez). */
    public Optional<Pelea> findActive() {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement(
                    "SELECT * FROM fight WHERE state <> ? ORDER BY created_ms DESC, rowid DESC LIMIT 1")) {
                ps.setString(1, EstadoPelea.CERRADA.name());
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.<Pelea>empty();
                }
            }
        });
    }

    private static Pelea map(ResultSet rs) throws SQLException {
        return new Pelea(
                rs.getString("id"),
                rs.getString("red_corner"),
                rs.getString("blue_corner"),
                rs.getInt("rounds"),
                rs.getInt("judges"),
                Sql.getEnum(rs, "state", EstadoPelea.class),
                rs.getString("date"));
    }
}
