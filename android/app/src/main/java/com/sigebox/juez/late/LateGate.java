package com.sigebox.juez.late;

import com.sigebox.juez.score.ScoreUi;

/** Reglas de la captura tardía: no se sale hasta enviar y se vibra una sola vez al abrir. */
public final class LateGate {
    private boolean vibrated;

    /** Enviado = guardado en la bandeja de salida; desde ahí se reenvía solo hasta el ACK. */
    public boolean canLeave(ScoreUi ui) {
        // Si la central rechaza el tardío (p. ej. pelea ya cerrada), el juez no queda atrapado.
        return ui != null && (ui.card != null || ui.lastError != null);
    }

    /** La central ya confirmó la calificación tardía. */
    public boolean isConfirmed(ScoreUi ui) {
        return ui != null && ui.card != null && ui.card.status != ScoreUi.CardStatus.SENDING;
    }

    public boolean shouldVibrate() {
        if (vibrated) {
            return false;
        }
        vibrated = true;
        return true;
    }
}
