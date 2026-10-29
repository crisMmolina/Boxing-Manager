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

    /** Inserta o actualiza el lugar; la llave pública registrada no se toca. */
    public void save(LugarJuez l) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement("""
                    INSERT INTO judge_seat
                        (fight_id, number, name, device, credential_hash, connection, approved_by, approved_ms)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (fight_id, number) DO UPDATE SET name = excluded.name,
                        device = excluded.device, credential_hash = excluded.credential_hash,
                        connection = excluded.connection, approved_by = excluded.approved_by,
                        approved_ms = excluded.approved_ms
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

    /**
     * Registra la llave pública del {@code JOIN} (texto de {@code DeviceProof.encodePublicKey}).
     * {@code LugarJuez} no la lleva; el servidor la necesita para verificar cada {@code HELLO}.
     * El lugar debe existir.
     */
    public void savePublicKey(String peleaId, int numero, String publicKey) {
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement(
                    "UPDATE judge_seat SET public_key = ? WHERE fight_id = ? AND number = ?")) {
                ps.setString(1, publicKey);
                ps.setString(2, peleaId);
                ps.setInt(3, numero);
                if (ps.executeUpdate() != 1) {
                    throw new DbException("no existe el lugar " + numero + " de " + peleaId, null);
                }
            }
            return null;
        });
    }

    public Optional<String> publicKey(String peleaId, int numero) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement(
                    "SELECT public_key FROM judge_seat WHERE fight_id = ? AND number = ?")) {
                ps.setString(1, peleaId);
                ps.setInt(2, numero);
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.<String>empty();
                }
            }
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
