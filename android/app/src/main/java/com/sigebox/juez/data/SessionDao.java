package com.sigebox.juez.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface SessionDao {
    @Query("SELECT * FROM local_state WHERE id = 1")
    SessionEntity load();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(SessionEntity session);

    @Query("UPDATE local_state SET approved = 1 WHERE id = 1")
    void markApproved();

    @Query("UPDATE local_state SET fight_id = :fightId, seat = :seat WHERE id = 1")
    void switchFight(String fightId, int seat);

    @Query("DELETE FROM local_state")
    void clear();
}
