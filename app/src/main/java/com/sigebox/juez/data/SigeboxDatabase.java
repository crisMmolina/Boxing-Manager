package com.sigebox.juez.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** Base local del juez: bandeja de salida y lugar de juez. Room usa WAL por defecto. */
@Database(entities = {OutboxEntity.class, SessionEntity.class}, version = 1, exportSchema = false)
public abstract class SigeboxDatabase extends RoomDatabase {
    public static final String NAME = "sigebox-juez.db";

    public abstract OutboxDao outboxDao();

    public abstract SessionDao sessionDao();

    public static SigeboxDatabase create(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), SigeboxDatabase.class, NAME).build();
    }
}
