package com.sigebox.juez.wait;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import androidx.room.Room;
import com.sigebox.juez.R;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.net.ConnectionTestAccess;
import com.sigebox.juez.net.JudgeConnection;
import java.util.function.Function;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Spec 006 · AND-024: Siguiente pelea con estado ilustrado (DESIGN v5 §6). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class NextFightV5Test {

    private final Function<Context, JudgeConnection> original = NextFightActivity.connections;
    private Application app;
    private SigeboxDatabase db;

    @Before
    public void setUp() throws Exception {
        app = RuntimeEnvironment.getApplication();
        db = Room.inMemoryDatabaseBuilder(app, SigeboxDatabase.class).allowMainThreadQueries().build();
        JudgeConnection connection = ConnectionTestAccess.newConnection(db);
        NextFightActivity.connections = context -> connection;
    }

    @After
    public void tearDown() {
        NextFightActivity.connections = original;
        db.close();
    }

    private NextFightActivity open(Intent intent) {
        return Robolectric.buildActivity(NextFightActivity.class, intent).setup().get();
    }

    @Test
    public void mientrasEsperaLaInsigniaMuestraElReloj() {
        NextFightActivity activity = open(new Intent(app, NextFightActivity.class));

        View badge = activity.findViewById(R.id.next_fight_badge);
        assertNotNull(badge);
        assertEquals(View.VISIBLE, badge.getVisibility());
        ImageView icon = activity.findViewById(R.id.next_fight_badge_icon);
        assertEquals("clock", icon.getTag());
    }

    @Test
    public void conElEventoTerminadoLaInsigniaMuestraLaPalomita() {
        NextFightActivity.connections = context -> {
            throw new AssertionError("la sesión ya se borró");
        };
        Intent ended = new Intent(app, NextFightActivity.class).putExtra(NextFightActivity.EXTRA_EVENT_ENDED, true);

        NextFightActivity activity = open(ended);

        ImageView icon = activity.findViewById(R.id.next_fight_badge_icon);
        assertEquals("check", icon.getTag());
    }
}
