package com.sigebox.central.db;

import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.VersionTarjeta;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Tabla {@code card_version}: historial de solo agregado de cada tarjeta. */
public final class VersionTarjetaRepository {

    private final Database db;

    public VersionTarjetaRepository(Database db) {
        this.db = db;
    }

    public void append(VersionTarjeta v) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement("""
                    INSERT INTO card_version (fight_id, judge, round, red_before, blue_before,
                        red_after, blue_after, who, reason, time_ms)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                ps.setString(1, v.peleaId());
                ps.setInt(2, v.juez());
                ps.setInt(3, v.round());
                Sql.setInt(ps, 4, v.rojoAnterior());
                Sql.setInt(ps, 5, v.azulAnterior());
                ps.setInt(6, v.rojoNuevo());
                ps.setInt(7, v.azulNuevo());
                Sql.setEnum(ps, 8, v.quien());
                ps.setString(9, v.motivo());
                ps.setLong(10, v.horaMs());
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** Versiones de una tarjeta, de la más vieja a la más nueva. */
    public List<VersionTarjeta> of(String peleaId, int juez, int round) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement(
                    "SELECT * FROM card_version WHERE fight_id = ? AND judge = ? AND round = ? ORDER BY id")) {
                ps.setString(1, peleaId);
                ps.setInt(2, juez);
                ps.setInt(3, round);
                try (var rs = ps.executeQuery()) {
                    var out = new ArrayList<VersionTarjeta>();
                    while (rs.next()) {
                        out.add(map(rs));
                    }
                    return out;
                }
            }
        });
    }

    private static VersionTarjeta map(ResultSet rs) throws SQLException {
        return new VersionTarjeta(
                rs.getString("fight_id"),
                rs.getInt("judge"),
                rs.getInt("round"),
                Sql.getInt(rs, "red_before"),
                Sql.getInt(rs, "blue_before"),
                rs.getInt("red_after"),
                rs.getInt("blue_after"),
                Sql.getEnum(rs, "who", OrigenTarjeta.class),
                rs.getString("reason"),
                rs.getLong("time_ms"));
    }
}
