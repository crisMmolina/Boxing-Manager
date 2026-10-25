package com.sigebox.central.db;

import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.Terminacion;
import java.util.Optional;

/** Tabla {@code termination}: a lo más una por pelea. */
public final class TerminacionRepository {

    private final Database db;

    public TerminacionRepository(Database db) {
        this.db = db;
    }

    public void save(Terminacion t) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement("""
                    INSERT OR REPLACE INTO termination
                        (fight_id, reason, other_text, winner_corner, round, time, notes)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                ps.setString(1, t.peleaId());
                Sql.setEnum(ps, 2, t.motivo());
                ps.setString(3, t.textoOtro());
                Sql.setEnum(ps, 4, t.ganadora());
                ps.setInt(5, t.round());
                ps.setString(6, t.tiempo());
                ps.setString(7, t.observaciones());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public Optional<Terminacion> find(String peleaId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM termination WHERE fight_id = ?")) {
                ps.setString(1, peleaId);
                try (var rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.<Terminacion>empty();
                    }
                    return Optional.of(new Terminacion(
                            rs.getString("fight_id"),
                            Sql.getEnum(rs, "reason", MotivoTerminacion.class),
                            rs.getString("other_text"),
                            Sql.getEnum(rs, "winner_corner", Esquina.class),
                            rs.getInt("round"),
                            rs.getString("time"),
                            rs.getString("notes")));
                }
            }
        });
    }
}
