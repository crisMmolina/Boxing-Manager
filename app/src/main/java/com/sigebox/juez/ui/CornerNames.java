package com.sigebox.juez.ui;

import android.content.Context;
import android.content.SharedPreferences;

import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.State;

/**
 * Nombres de los boxeadores para las tarjetas de esquina (DESIGN.md v3 §5.2). Solo es una caché de pantalla: llegan en
 * {@code JOIN_APPROVED} o {@code STATE}, a veces antes de que abra la pantalla de puntuación.
 */
public final class CornerNames {
    private static final String PREFS = "sigebox_corner_names";

    private final SharedPreferences prefs;

    public CornerNames(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** @return {@code true} si el mensaje traía nombres */
    public boolean remember(Message message) {
        if (message instanceof JoinApproved approved) {
            save(approved.fightId(), approved.redCorner(), approved.blueCorner());
            return true;
        }
        if (message instanceof State state) {
            save(state.fightId(), state.redCorner(), state.blueCorner());
            return true;
        }
        return false;
    }

    public String red(String fightId) {
        return prefs.getString(fightId + ".red", null);
    }

    public String blue(String fightId) {
        return prefs.getString(fightId + ".blue", null);
    }

    private void save(String fightId, String red, String blue) {
        prefs.edit().putString(fightId + ".red", red).putString(fightId + ".blue", blue).apply();
    }
}
