package com.sigebox.central.db;

/**
 * Tabla {@code processed_message}: idempotencia por (pelea, lugar de la sesión, {@code id}), como
 * pide el contrato (core/schemas/README.md). El servidor marca el mensaje y guarda su efecto en
 * la misma {@link Database#inTransaction transacción}; un reenvío tras perder el ACK se reconoce
 * y solo se vuelve a confirmar.
 */
public final class ProcessedMessages {

    private final Database db;

    public ProcessedMessages(Database db) {
        this.db = db;
    }

    /** Marca el mensaje como procesado. {@code true} si es nuevo, {@code false} si ya se había visto. */
    public boolean markIfNew(String peleaId, int seat, String messageId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement(
                    "INSERT OR IGNORE INTO processed_message (fight_id, seat, id, processed_ms) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, peleaId);
                ps.setInt(2, seat);
                ps.setString(3, messageId);
                ps.setLong(4, System.currentTimeMillis());
                return ps.executeUpdate() == 1;
            }
        });
    }

    public boolean seen(String peleaId, int seat, String messageId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement(
                    "SELECT 1 FROM processed_message WHERE fight_id = ? AND seat = ? AND id = ?")) {
                ps.setString(1, peleaId);
                ps.setInt(2, seat);
                ps.setString(3, messageId);
                try (var rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }
}
