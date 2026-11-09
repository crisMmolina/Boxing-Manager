package com.sigebox.central.ui.log;

import com.sigebox.central.db.Database;
import com.sigebox.core.model.EventoBitacora;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas de la vista de bitácora: filtra por pelea y por tipo, lo más reciente primero. Un
 * filtro {@code null} no filtra; {@link #GLOBAL} pide los eventos sin pelea (arranque, términos).
 */
public final class LogQuery {

    /** Filtro de pelea para los eventos globales. */
    public static final String GLOBAL = "(sin pelea)";

    public static final int DEFAULT_LIMIT = 500;

    private final Database db;

    public LogQuery(Database db) {
        this.db = db;
    }

    public List<EventoBitacora> find(String fightId, String type, int limit) {
        var sql = new StringBuilder("SELECT fight_id, type, detail, time_ms FROM event_log WHERE 1 = 1");
        var params = new ArrayList<String>();
        if (GLOBAL.equals(fightId)) {
            sql.append(" AND fight_id IS NULL");
        } else if (fightId != null) {
            sql.append(" AND fight_id = ?");
            params.add(fightId);
        }
        if (type != null) {
            sql.append(" AND type = ?");
            params.add(type);
        }
        sql.append(" ORDER BY id DESC LIMIT ?");
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement(sql.toString())) {
                int i = 1;
                for (var p : params) {
                    ps.setString(i++, p);
                }
                ps.setInt(i, Math.max(1, limit));
                try (var rs = ps.executeQuery()) {
                    var out = new ArrayList<EventoBitacora>();
                    while (rs.next()) {
                        out.add(new EventoBitacora(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4)));
                    }
                    return out;
                }
            }
        });
    }

    /** Nombre visible de cada pelea (spec 005: el id interno no se muestra). */
    public java.util.Map<String, String> nombres() {
        return db.inTransaction(c -> {
            var out = new java.util.HashMap<String, String>();
            try (var st = c.createStatement();
                    var rs = st.executeQuery("""
                            SELECT f.id, c.position, f.red_corner, f.blue_corner
                            FROM fight f LEFT JOIN funcion_card c ON c.fight_id = f.id""")) {
                while (rs.next()) {
                    var esquinas = rs.getString(3) + " vs " + rs.getString(4);
                    int position = rs.getInt(2);
                    out.put(rs.getString(1), rs.wasNull() ? esquinas : "Pelea " + position + " · " + esquinas);
                }
            }
            return out;
        });
    }

    /** El nombre visible de {@code fightId} (o {@link #GLOBAL}); nunca el id interno. */
    public static String nombre(java.util.Map<String, String> nombres, String fightId) {
        if (fightId == null || GLOBAL.equals(fightId)) {
            return GLOBAL;
        }
        return nombres.getOrDefault(fightId, "Pelea sin nombre");
    }

    /** Peleas con eventos, más {@link #GLOBAL} si hay eventos sin pelea. */
    public List<String> fights() {
        return db.inTransaction(c -> {
            var out = new ArrayList<String>();
            try (var st = c.createStatement();
                    var rs = st.executeQuery("SELECT DISTINCT fight_id FROM event_log ORDER BY fight_id")) {
                while (rs.next()) {
                    var f = rs.getString(1);
                    out.add(f == null ? GLOBAL : f);
                }
            }
            return out;
        });
    }

    public List<String> types() {
        return db.inTransaction(c -> {
            var out = new ArrayList<String>();
            try (var st = c.createStatement();
                    var rs = st.executeQuery("SELECT DISTINCT type FROM event_log ORDER BY type")) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
            return out;
        });
    }
}
