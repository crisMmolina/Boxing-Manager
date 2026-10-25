package com.sigebox.central.db;

import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.Round;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Tabla {@code round}. */
public final class RoundRepository {

    private final Database db;

    public RoundRepository(Database db) {
        this.db = db;
    }

    public void save(Round r) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement("""
                    INSERT OR REPLACE INTO round (fight_id, number, state, opened_ms, closed_ms, forced)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                ps.setString(1, r.peleaId());
                ps.setInt(2, r.numero());
                Sql.setEnum(ps, 3, r.estado());
                Sql.setLong(ps, 4, r.abiertoEnMs());
                Sql.setLong(ps, 5, r.cerradoEnMs());
                ps.setInt(6, r.cierreForzado() ? 1 : 0);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public Optional<Round> find(String peleaId, int numero) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM round WHERE fight_id = ? AND number = ?")) {
                ps.setString(1, peleaId);
                ps.setInt(2, numero);
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.<Round>empty();
                }
            }
        });
    }

    public List<Round> byPelea(String peleaId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM round WHERE fight_id = ? ORDER BY number")) {
                ps.setString(1, peleaId);
                try (var rs = ps.executeQuery()) {
                    var out = new ArrayList<Round>();
                    while (rs.next()) {
                        out.add(map(rs));
                    }
                    return out;
                }
            }
        });
    }

    private static Round map(ResultSet rs) throws SQLException {
        return new Round(
                rs.getString("fight_id"),
                rs.getInt("number"),
                Sql.getEnum(rs, "state", EstadoRound.class),
                Sql.getLong(rs, "opened_ms"),
                Sql.getLong(rs, "closed_ms"),
                rs.getInt("forced") != 0);
    }
}
