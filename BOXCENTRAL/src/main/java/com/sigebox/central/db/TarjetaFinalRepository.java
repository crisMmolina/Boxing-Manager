package com.sigebox.central.db;

import com.sigebox.core.model.TarjetaFinal;
import java.util.ArrayList;
import java.util.List;

/** Tabla {@code final_card}. */
public final class TarjetaFinalRepository {

    private final Database db;

    public TarjetaFinalRepository(Database db) {
        this.db = db;
    }

    public void save(TarjetaFinal t) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement(
                    "INSERT OR REPLACE INTO final_card (fight_id, judge, hash, time_ms) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, t.peleaId());
                ps.setInt(2, t.juez());
                ps.setString(3, t.huella());
                ps.setLong(4, t.horaMs());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public List<TarjetaFinal> byPelea(String peleaId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM final_card WHERE fight_id = ? ORDER BY judge")) {
                ps.setString(1, peleaId);
                try (var rs = ps.executeQuery()) {
                    var out = new ArrayList<TarjetaFinal>();
                    while (rs.next()) {
                        out.add(new TarjetaFinal(
                                rs.getString("fight_id"), rs.getInt("judge"), rs.getString("hash"),
                                rs.getLong("time_ms")));
                    }
                    return out;
                }
            }
        });
    }
}
