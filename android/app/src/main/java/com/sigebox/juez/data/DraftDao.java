package com.sigebox.juez.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/** AND-035: el borrador del round abierto. */
@Dao
public interface DraftDao {
    @Query("SELECT * FROM draft WHERE id = 1")
    DraftEntity load();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(DraftEntity draft);

    @Query("DELETE FROM draft")
    void clear();
}
