package com.sigebox.central.db;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sigebox.core.model.Ganador;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.Resultado;
import com.sigebox.core.model.TipoDecision;
import java.util.List;
import java.util.Optional;

/**
 * Tabla {@code result}. El resultado lo calcula {@code core}; aquí solo se guarda. Totales y
 * conteo van como JSON porque se leen y escriben siempre juntos.
 */
public final class ResultadoRepository {

    private static final Gson GSON = new Gson();

    private final Database db;

    public ResultadoRepository(Database db) {
        this.db = db;
    }

    public void save(Resultado r) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement("""
                    INSERT OR REPLACE INTO result
                        (fight_id, winner, decision_type, reason, totals_json, count_json, computed_ms)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                ps.setString(1, r.peleaId());
                Sql.setEnum(ps, 2, r.ganador());
                Sql.setEnum(ps, 3, r.tipo());
                Sql.setEnum(ps, 4, r.motivo());
                ps.setString(5, GSON.toJson(r.totales()));
                ps.setString(6, GSON.toJson(r.conteo()));
                ps.setLong(7, System.currentTimeMillis());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public Optional<Resultado> find(String peleaId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM result WHERE fight_id = ?")) {
                ps.setString(1, peleaId);
                try (var rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.<Resultado>empty();
                    }
                    List<Resultado.TotalJuez> totales = GSON.fromJson(
                            rs.getString("totals_json"), new TypeToken<List<Resultado.TotalJuez>>() {}.getType());
                    return Optional.of(new Resultado(
                            rs.getString("fight_id"),
                            totales,
                            Sql.getEnum(rs, "winner", Ganador.class),
                            Sql.getEnum(rs, "decision_type", TipoDecision.class),
                            Sql.getEnum(rs, "reason", MotivoTerminacion.class),
                            GSON.fromJson(rs.getString("count_json"), Resultado.Conteo.class)));
                }
            }
        });
    }
}
