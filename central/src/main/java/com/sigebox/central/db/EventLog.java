package com.sigebox.central.db;

import com.sigebox.core.model.EventoBitacora;
import java.util.ArrayList;
import java.util.List;

/** Tabla {@code event_log}: bitácora de solo agregado (repudio, auditoría). */
public final class EventLog {

    private final Database db;

    public EventLog(Database db) {
        this.db = db;
    }

    /** Agrega un evento con la hora actual. {@code peleaId} puede ser null para eventos globales. */
    public void append(String peleaId, String tipo, String detalle) {
        append(new EventoBitacora(peleaId, tipo, detalle, System.currentTimeMillis()));
    }

    public void append(EventoBitacora e) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement(
                    "INSERT INTO event_log (fight_id, type, detail, time_ms) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, e.peleaId());
                ps.setString(2, e.tipo());
                ps.setString(3, e.detalle());
                ps.setLong(4, e.horaMs());
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** Eventos de una pelea en orden de llegada. */
    public List<EventoBitacora> byPelea(String peleaId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM event_log WHERE fight_id = ? ORDER BY id")) {
                ps.setString(1, peleaId);
                try (var rs = ps.executeQuery()) {
                    var out = new ArrayList<EventoBitacora>();
                    while (rs.next()) {
                        out.add(new EventoBitacora(
                                rs.getString("fight_id"), rs.getString("type"), rs.getString("detail"),
                                rs.getLong("time_ms")));
                    }
                    return out;
                }
            }
        });
    }
}
