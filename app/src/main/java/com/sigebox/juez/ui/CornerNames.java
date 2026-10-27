package com.sigebox.juez.ui;

import android.content.Context;
import android.content.SharedPreferences;

import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.State;

/**
 * Nombres de los boxeadores (DESIGN.md v3 §5.2) y número de rounds de la pelea. Solo es una caché de pantalla: llegan en
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
            save(approved.fightId(), approved.redCorner(), approved.blueCorner(), approved.rounds());
            return true;
        }
        if (message instanceof State state) {
            save(state.fightId(), state.redCorner(), state.blueCorner(), state.rounds());
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

    /** Rounds de la pelea, o {@code null} si no han llegado (para "Round 4 de 10" y la barra de progreso). */
    public Integer rounds(String fightId) {
        int rounds = prefs.getInt(fightId + ".rounds", 0);
        return rounds > 0 ? rounds : null;
    }

    private void save(String fightId, String red, String blue, Integer rounds) {
        prefs.edit().putString(fightId + ".red", red).putString(fightId + ".blue", blue)
                .putInt(fightId + ".rounds", rounds == null ? 0 : rounds).apply();
    }
}
