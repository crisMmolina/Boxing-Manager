package com.sigebox.central.db;

import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.LugarJuez;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Tabla {@code judge_seat}. */
public final class LugarJuezRepository {

    private final Database db;

    public LugarJuezRepository(Database db) {
        this.db = db;
    }

    public void save(LugarJuez l) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement("""
                    INSERT OR REPLACE INTO judge_seat
                        (fight_id, number, name, device, credential_hash, connection, approved_by, approved_ms)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                ps.setString(1, l.peleaId());
                ps.setInt(2, l.numero());
                ps.setString(3, l.nombre());
                ps.setString(4, l.dispositivo());
                ps.setString(5, l.huellaCredencial());
                Sql.setEnum(ps, 6, l.conexion());
                ps.setString(7, l.aprobadoPor());
                Sql.setLong(ps, 8, l.aprobadoEnMs());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public Optional<LugarJuez> find(String peleaId, int numero) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM judge_seat WHERE fight_id = ? AND number = ?")) {
                ps.setString(1, peleaId);
                ps.setInt(2, numero);
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.<LugarJuez>empty();
                }
            }
        });
    }

    public List<LugarJuez> byPelea(String peleaId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM judge_seat WHERE fight_id = ? ORDER BY number")) {
                ps.setString(1, peleaId);
                try (var rs = ps.executeQuery()) {
                    var out = new ArrayList<LugarJuez>();
                    while (rs.next()) {
                        out.add(map(rs));
                    }
                    return out;
                }
            }
        });
    }

    private static LugarJuez map(ResultSet rs) throws SQLException {
        return new LugarJuez(
                rs.getString("fight_id"),
                rs.getInt("number"),
                rs.getString("name"),
                rs.getString("device"),
                rs.getString("credential_hash"),
                Sql.getEnum(rs, "connection", EstadoConexion.class),
                rs.getString("approved_by"),
                Sql.getLong(rs, "approved_ms"));
    }
}
