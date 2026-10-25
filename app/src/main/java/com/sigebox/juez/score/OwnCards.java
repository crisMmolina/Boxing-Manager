package com.sigebox.juez.score;

import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;

import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.core.model.EstadoTarjeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Las tarjetas que este juez envió, leídas de la bandeja de salida (sobrevive a cerrar la app). Por round gana la
 * última versión no rechazada; sin {@code ACK} todavía cuenta como "Enviando…". Nunca hay tarjetas de otros jueces.
 */
public final class OwnCards {

    public static final class Row {
        public final int round;
        public final Integer red;
        public final Integer blue;
        public final ScoreUi.CardStatus status;
        public final String messageId;

        public Row(int round, Integer red, Integer blue, ScoreUi.CardStatus status, String messageId) {
            this.round = round;
            this.red = red;
            this.blue = blue;
            this.status = status;
            this.messageId = messageId;
        }
    }

    private OwnCards() {}

    /** Hace I/O: llamar fuera del hilo principal. */
    public static Map<Integer, Row> load(SupportSQLiteDatabase db, String fightId) {
        Map<Integer, Row> rows = new HashMap<>();
        try (Cursor cursor = db.query("SELECT message_id, payload, acked, ack_ok, ack_status FROM outbox"
                + " WHERE type = ? ORDER BY created_at, rowid", new Object[] {SubmitRound.TYPE})) {
            while (cursor.moveToNext()) {
                Message message;
                try {
                    message = Messages.decode(cursor.getString(1));
                } catch (RuntimeException e) {
                    continue;
                }
                SubmitRound submit = (SubmitRound) message;
                if (!fightId.equals(submit.fightId())) {
                    continue;
                }
                boolean acked = cursor.getInt(2) != 0;
                boolean ok = !cursor.isNull(3) && cursor.getInt(3) != 0;
                if (acked && !ok) {
                    continue;
                }
                ScoreUi.CardStatus status = acked
                        ? ScoreUi.CardStatus.from(cursor.isNull(4) ? null : EstadoTarjeta.valueOf(cursor.getString(4)))
                        : ScoreUi.CardStatus.SENDING;
                if (acked && status == ScoreUi.CardStatus.SENT && rows.containsKey(submit.round())) {
                    status = ScoreUi.CardStatus.CORRECTED;
                }
                rows.put(submit.round(),
                        new Row(submit.round(), submit.red(), submit.blue(), status, cursor.getString(0)));
            }
        }
        return rows;
    }
}
