package com.sigebox.central.db;

import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.TarjetaRound;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Tabla {@code round_card}: la calificación vigente de cada juez por round. */
public final class TarjetaRoundRepository {

    private final Database db;

    public TarjetaRoundRepository(Database db) {
        this.db = db;
    }

    /** Guarda la tarjeta vigente; una corrección la reemplaza (el historial va en {@code card_version}). */
    public void save(TarjetaRound t) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement("""
                    INSERT OR REPLACE INTO round_card (fight_id, judge, round, red, blue, state, origin, time_ms)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                ps.setString(1, t.peleaId());
                ps.setInt(2, t.juez());
                ps.setInt(3, t.round());
                ps.setInt(4, t.rojo());
                ps.setInt(5, t.azul());
                Sql.setEnum(ps, 6, t.estado());
                Sql.setEnum(ps, 7, t.origen());
                ps.setLong(8, t.horaMs());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public Optional<TarjetaRound> find(String peleaId, int juez, int round) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement(
                    "SELECT * FROM round_card WHERE fight_id = ? AND judge = ? AND round = ?")) {
                ps.setString(1, peleaId);
                ps.setInt(2, juez);
                ps.setInt(3, round);
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.<TarjetaRound>empty();
                }
            }
        });
    }

    public List<TarjetaRound> byPelea(String peleaId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement(
                    "SELECT * FROM round_card WHERE fight_id = ? ORDER BY round, judge")) {
                ps.setString(1, peleaId);
                try (var rs = ps.executeQuery()) {
                    var out = new ArrayList<TarjetaRound>();
                    while (rs.next()) {
                        out.add(map(rs));
                    }
                    return out;
                }
            }
        });
    }

    private static TarjetaRound map(ResultSet rs) throws SQLException {
        return new TarjetaRound(
                rs.getString("fight_id"),
                rs.getInt("judge"),
                rs.getInt("round"),
                rs.getInt("red"),
                rs.getInt("blue"),
                Sql.getEnum(rs, "state", EstadoTarjeta.class),
                Sql.getEnum(rs, "origin", OrigenTarjeta.class),
                rs.getLong("time_ms"));
    }
}
