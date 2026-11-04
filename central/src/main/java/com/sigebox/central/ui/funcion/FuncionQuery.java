package com.sigebox.central.ui.funcion;

import com.sigebox.central.db.Database;
import java.util.Optional;

/**
 * Cuál es la función en curso: la más reciente. {@code FuncionService} solo busca por id, así que la
 * pantalla lo lee aquí (como {@code LogQuery}); sobrevive a un reinicio de la central.
 */
public final class FuncionQuery {

    private final Database db;

    public FuncionQuery(Database db) {
        this.db = db;
    }

    /** Id de la función más reciente, si hay alguna. */
    public Optional<String> latest() {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT id FROM funcion ORDER BY created_ms DESC, rowid DESC LIMIT 1");
                    var rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.<String>empty();
            }
        });
    }
}
