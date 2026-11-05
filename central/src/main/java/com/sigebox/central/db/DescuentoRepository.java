package com.sigebox.central.db;

import com.sigebox.core.model.Descuento;
import com.sigebox.core.model.Esquina;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Tabla {@code descuento}: un renglón por punto quitado. Anular marca el renglón, nunca lo borra (ADR 0005). */
public final class DescuentoRepository {

    private final Database db;

    public DescuentoRepository(Database db) {
        this.db = db;
    }

    /** Guarda un descuento vigente y devuelve su id. */
    public long save(String fightId, int round, Esquina esquina, long createdMs) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("""
                    INSERT INTO descuento (fight_id, round, esquina, puntos, created_ms)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, fightId);
                ps.setInt(2, round);
                Sql.setEnum(ps, 3, esquina);
                ps.setInt(4, Descuento.PUNTOS);
                ps.setLong(5, createdMs);
                ps.executeUpdate();
                try (var keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
    }

    public Optional<Descuento> find(long id) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM descuento WHERE id = ?")) {
                ps.setLong(1, id);
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(read(rs)) : Optional.<Descuento>empty();
                }
            }
        });
    }

    /** Los descuentos de la pelea, anulados incluidos, en el orden en que se pusieron. */
    public List<Descuento> byPelea(String fightId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM descuento WHERE fight_id = ? ORDER BY id")) {
                ps.setString(1, fightId);
                try (var rs = ps.executeQuery()) {
                    var all = new ArrayList<Descuento>();
                    while (rs.next()) {
                        all.add(read(rs));
                    }
                    return List.copyOf(all);
                }
            }
        });
    }

    /** Marca anulado el descuento con su motivo; el renglón se queda. */
    public void anular(long id, String motivo, long anuladoMs) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement("""
                    UPDATE descuento SET anulado = 1, motivo_anulacion = ?, anulado_ms = ? WHERE id = ?
                    """)) {
                ps.setString(1, motivo);
                ps.setLong(2, anuladoMs);
                ps.setLong(3, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private static Descuento read(ResultSet rs) throws SQLException {
        return new Descuento(
                rs.getLong("id"),
                rs.getString("fight_id"),
                rs.getInt("round"),
                Sql.getEnum(rs, "esquina", Esquina.class),
                rs.getInt("puntos"),
                rs.getInt("anulado") != 0,
                rs.getString("motivo_anulacion"));
    }
}
