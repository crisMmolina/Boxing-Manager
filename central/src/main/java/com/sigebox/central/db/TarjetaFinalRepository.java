package com.sigebox.central.db;

import com.sigebox.core.model.TarjetaFinal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Tabla {@code final_card}. */
public final class TarjetaFinalRepository {

    private final Database db;

    public TarjetaFinalRepository(Database db) {
        this.db = db;
    }

    public void save(TarjetaFinal t) {
        save(t, null);
    }

    /**
     * Guarda la tarjeta final con la firma del juez sobre {@code DeviceProof.finalCardText}
     * ({@code TarjetaFinal} no la lleva; se conserva por no repudio). Una firma null no borra una ya
     * guardada.
     */
    public void save(TarjetaFinal t, String signature) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement("""
                    INSERT INTO final_card (fight_id, judge, hash, signature, time_ms) VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (fight_id, judge) DO UPDATE SET hash = excluded.hash,
                        signature = COALESCE(excluded.signature, final_card.signature), time_ms = excluded.time_ms
                    """)) {
                ps.setString(1, t.peleaId());
                ps.setInt(2, t.juez());
                ps.setString(3, t.huella());
                ps.setString(4, signature);
                ps.setLong(5, t.horaMs());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public Optional<String> signature(String peleaId, int juez) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT signature FROM final_card WHERE fight_id = ? AND judge = ?")) {
                ps.setString(1, peleaId);
                ps.setInt(2, juez);
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.<String>empty();
                }
            }
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
