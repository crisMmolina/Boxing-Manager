package com.sigebox.central.ui.terms;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;

/**
 * Aceptación de términos en el primer uso. La aceptación (y el rechazo) quedan en la bitácora como
 * evento global; un cambio de {@link #VERSION} vuelve a pedirlos.
 */
public final class TermsGate {

    /** Versión vigente del texto; subirla obliga a aceptar de nuevo. */
    public static final String VERSION = "v1";

    public static final String ACCEPTED = "TERMS_ACCEPTED";
    public static final String DECLINED = "TERMS_DECLINED";

    private final Database db;
    private final EventLog log;

    public TermsGate(Database db) {
        this.db = db;
        this.log = new EventLog(db);
    }

    /** {@code true} si hay que mostrar los términos antes de cualquier otra pantalla. */
    public boolean mustShow() {
        return !accepted(VERSION);
    }

    /** Id de la primera pantalla: los términos si no están aceptados, si no {@code otherwise}. */
    public String firstScreen(String otherwise) {
        return mustShow() ? TermsScreen.ID : otherwise;
    }

    public void accept() {
        log.append(null, ACCEPTED, VERSION);
    }

    /** Sin aceptar no se continúa: la UI cierra la aplicación después de registrarlo. */
    public void decline() {
        log.append(null, DECLINED, VERSION);
    }

    boolean accepted(String version) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement(
                    "SELECT 1 FROM event_log WHERE fight_id IS NULL AND type = ? AND detail = ? LIMIT 1")) {
                ps.setString(1, ACCEPTED);
                ps.setString(2, version);
                try (var rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }
}
