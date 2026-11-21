package com.sigebox.juez.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** Base local del juez: bandeja de salida y lugar de juez. Room usa WAL por defecto. */
@Database(entities = {OutboxEntity.class, SessionEntity.class, DraftEntity.class}, version = 2, exportSchema = false)
public abstract class SigeboxDatabase extends RoomDatabase {
    public static final String NAME = "sigebox-juez.db";

    public abstract OutboxDao outboxDao();

    public abstract SessionDao sessionDao();

    /** AND-035: borrador del round abierto. */
    public abstract DraftDao draftDao();

    /** 1 → 2 (AND-035): agrega la tabla del borrador; lugar y Outbox no se tocan. */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `draft` (`id` INTEGER NOT NULL, `fight_id` TEXT NOT NULL, "
                    + "`seat` INTEGER NOT NULL, `round` INTEGER NOT NULL, `red` INTEGER, `blue` INTEGER, "
                    + "PRIMARY KEY(`id`))");
        }
    };

    public static SigeboxDatabase create(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), SigeboxDatabase.class, NAME)
                .addMigrations(MIGRATION_1_2)
                .build();
    }
}
