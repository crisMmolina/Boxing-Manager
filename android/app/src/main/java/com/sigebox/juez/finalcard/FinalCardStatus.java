package com.sigebox.juez.finalcard;

import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;

import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.SubmitFinalCard;

/**
 * Si la tarjeta final de esta pelea ya se envió, leído de la bandeja de salida (AND-015): sobrevive a recrear la
 * pantalla y a un cierre forzado, a diferencia de un campo en memoria. Cuenta el último envío de la pelea.
 */
public final class FinalCardStatus {

    public enum State {
        /** Nunca se envió, o la central la rechazó con un error que permite reintentar. */
        NOT_SENT,
        /** En la bandeja de salida sin {@code ACK}: se reenvía sola. */
        SENDING,
        /** {@code ACK} correcto o {@code ALREADY_FINAL}. */
        SENT
    }

    public final State state;
    /** Id del envío pendiente, para reconocer su {@code ACK}; {@code null} si no hay. */
    public final String messageId;

    private FinalCardStatus(State state, String messageId) {
        this.state = state;
        this.messageId = messageId;
    }

    /** Hace I/O: llamar fuera del hilo principal. */
    public static FinalCardStatus load(SupportSQLiteDatabase db, String fightId) {
        FinalCardStatus status = new FinalCardStatus(State.NOT_SENT, null);
        try (Cursor cursor = db.query("SELECT message_id, payload, acked, ack_ok, ack_error FROM outbox"
                + " WHERE type = ? ORDER BY created_at, rowid", new Object[] {SubmitFinalCard.TYPE})) {
            while (cursor.moveToNext()) {
                Message message;
                try {
                    message = Messages.decode(cursor.getString(1));
                } catch (RuntimeException e) {
                    continue;
                }
                if (!fightId.equals(((SubmitFinalCard) message).fightId())) {
                    continue;
                }
                status = fromRow(cursor);
            }
        }
        return status;
    }

    private static FinalCardStatus fromRow(Cursor cursor) {
        String id = cursor.getString(0);
        if (cursor.getInt(2) == 0) {
            return new FinalCardStatus(State.SENDING, id);
        }
        boolean ok = !cursor.isNull(3) && cursor.getInt(3) != 0;
        String error = cursor.isNull(4) ? null : cursor.getString(4);
        if (ok || Ack.Error.ALREADY_FINAL.name().equals(error)) {
            return new FinalCardStatus(State.SENT, null);
        }
        return new FinalCardStatus(State.NOT_SENT, null);
    }
}
