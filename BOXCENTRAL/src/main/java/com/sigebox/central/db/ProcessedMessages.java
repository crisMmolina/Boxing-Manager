package com.sigebox.central.db;

/**
 * Tabla {@code processed_message}: idempotencia por {@code id} de mensaje. El servidor marca el
 * mensaje y guarda su efecto en la misma {@link Database#inTransaction transacción}; un reenvío
 * tras perder el ACK se reconoce y solo se vuelve a confirmar.
 */
public final class ProcessedMessages {

    private final Database db;

    public ProcessedMessages(Database db) {
        this.db = db;
    }

    /** Marca el mensaje como procesado. {@code true} si es nuevo, {@code false} si ya se había visto. */
    public boolean markIfNew(String messageId, String peleaId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement(
                    "INSERT OR IGNORE INTO processed_message (id, fight_id, processed_ms) VALUES (?, ?, ?)")) {
                ps.setString(1, messageId);
                ps.setString(2, peleaId);
                ps.setLong(3, System.currentTimeMillis());
                return ps.executeUpdate() == 1;
            }
        });
    }

    public boolean seen(String messageId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT 1 FROM processed_message WHERE id = ?")) {
                ps.setString(1, messageId);
                try (var rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }
}
