package com.sigebox.juez.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(OutboxEntity entity);

    @Query("SELECT * FROM outbox WHERE message_id = :id")
    OutboxEntity get(String id);

    @Query("SELECT * FROM outbox WHERE acked = 0 ORDER BY created_at, rowid")
    List<OutboxEntity> unacked();

    @Query("SELECT * FROM outbox WHERE acked = 0 AND (sent_at IS NULL OR sent_at <= :sentBefore)"
            + " ORDER BY created_at, rowid")
    List<OutboxEntity> unackedSentBefore(long sentBefore);

    @Query("SELECT COUNT(*) FROM outbox WHERE acked = 0")
    int countUnacked();

    @Query("UPDATE outbox SET sent_at = :sentAt WHERE message_id = :id")
    void markSent(String id, long sentAt);

    @Query("UPDATE outbox SET acked = 1, ack_ok = :ok, ack_status = :status, ack_error = :error"
            + " WHERE message_id = :id AND acked = 0")
    int markAcked(String id, boolean ok, String status, String error);
}
