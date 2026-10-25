package com.sigebox.juez.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Un mensaje del juez guardado antes de enviarse; se reenvía hasta recibir el {@code ACK} con su id. */
@Entity(tableName = "outbox")
public class OutboxEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "message_id")
    public String messageId = "";

    @NonNull
    public String type = "";

    /** El JSON exacto que se manda (mismo id en cada reenvío: el servidor es idempotente). */
    @NonNull
    public String payload = "";

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "sent_at")
    public Long sentAt;

    public boolean acked;

    @ColumnInfo(name = "ack_ok")
    public Boolean ackOk;

    @ColumnInfo(name = "ack_status")
    public String ackStatus;

    @ColumnInfo(name = "ack_error")
    public String ackError;
}
