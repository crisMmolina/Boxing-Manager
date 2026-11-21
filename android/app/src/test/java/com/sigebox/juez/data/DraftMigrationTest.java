package com.sigebox.juez.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** AND-035: la base pasa de la versión 1 a la 2 (tabla de borrador) sin perder lugar ni Outbox. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class DraftMigrationTest {

    private static final String NAME = "migracion-prueba.db";
    private final Context context = ApplicationProvider.getApplicationContext();
    private SigeboxDatabase db;

    @After
    public void tearDown() {
        if (db != null) {
            db.close();
        }
        context.deleteDatabase(NAME);
    }

    /** Una base versión 1 tal como la dejó la app anterior, con un lugar y una tarjeta sin ACK. */
    private void versionOne() {
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(NAME), null);
        old.execSQL("CREATE TABLE IF NOT EXISTS `outbox` (`message_id` TEXT NOT NULL, `type` TEXT NOT NULL, "
                + "`payload` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `sent_at` INTEGER, `acked` INTEGER NOT NULL, "
                + "`ack_ok` INTEGER, `ack_status` TEXT, `ack_error` TEXT, PRIMARY KEY(`message_id`))");
        old.execSQL("CREATE TABLE IF NOT EXISTS `local_state` (`id` INTEGER NOT NULL, `host` TEXT NOT NULL, "
                + "`port` INTEGER NOT NULL, `fight_id` TEXT NOT NULL, `seat` INTEGER NOT NULL, `token` TEXT NOT NULL, "
                + "`judge_name` TEXT NOT NULL, `cert_fingerprint` TEXT, `approved` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        old.execSQL("INSERT INTO local_state VALUES (1, '192.168.43.1', 8765, 'F1', 2, '482913', 'Ana', NULL, 1)");
        old.execSQL("INSERT INTO outbox VALUES ('m1', 'SUBMIT_ROUND', '{}', 1, NULL, 0, NULL, NULL, NULL)");
        old.setVersion(1);
        old.close();
    }

    @Test
    public void migrarConservaLugarYOutboxYAgregaElBorrador() {
        versionOne();
        db = Room.databaseBuilder(context, SigeboxDatabase.class, NAME)
                .addMigrations(SigeboxDatabase.MIGRATION_1_2)
                .allowMainThreadQueries()
                .build();

        SessionEntity session = db.sessionDao().load();
        assertNotNull("el lugar sigue", session);
        assertEquals("F1", session.fightId);
        assertEquals(1, new Outbox(db.outboxDao(), Clock.SYSTEM).pendingCount());

        assertNull(db.draftDao().load());
        DraftEntity draft = new DraftEntity();
        draft.fightId = "F1";
        draft.seat = 2;
        draft.round = 3;
        draft.red = 9;
        draft.blue = null;
        db.draftDao().save(draft);
        DraftEntity back = db.draftDao().load();
        assertEquals(Integer.valueOf(9), back.red);
        assertNull("una esquina sin tocar queda vacía", back.blue);
        db.draftDao().clear();
        assertNull(db.draftDao().load());
    }
}
