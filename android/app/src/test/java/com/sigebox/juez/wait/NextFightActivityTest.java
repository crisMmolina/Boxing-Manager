package com.sigebox.juez.wait;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.room.Room;

import com.sigebox.core.protocol.NextFight;
import com.sigebox.juez.R;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.net.ConnectionTestAccess;
import com.sigebox.juez.net.JudgeConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.time.Duration;
import java.util.function.Function;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class NextFightActivityTest {
    private static final String JOIN = "com.sigebox.juez.join.JoinActivity";

    private final Function<Context, JudgeConnection> original = NextFightActivity.connections;
    private Application app;
    private SigeboxDatabase db;
    private JudgeConnection connection;

    @Before
    public void setUp() throws Exception {
        app = RuntimeEnvironment.getApplication();
        db = Room.inMemoryDatabaseBuilder(app, SigeboxDatabase.class).allowMainThreadQueries().build();
        connection = ConnectionTestAccess.newConnection(db);
        NextFightActivity.connections = context -> connection;
    }

    @After
    public void tearDown() {
        NextFightActivity.connections = original;
        db.close();
    }

    @Test
    public void waitsWithoutCornersUntilTheyAreKnown() {
        NextFightActivity activity = open(new Intent(app, NextFightActivity.class)).get();

        assertEquals("Espera la siguiente pelea", title(activity));
        assertEquals(View.GONE, activity.findViewById(R.id.next_fight_corners).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.next_fight_back).getVisibility());
    }

    @Test
    public void showsTheCornersOfANextFightThatAlreadyArrived() {
        ConnectionTestAccess.dispatch(connection, new NextFight("F1", "F2", null, "Rojo", "Azul", 3));

        NextFightActivity activity = open(new Intent(app, NextFightActivity.class)).get();

        assertEquals("Rojo vs Azul", corners(activity));
    }

    @Test
    public void showsTheCornersWhenTheNextFightArrivesWhileWaiting() {
        NextFightActivity activity = open(new Intent(app, NextFightActivity.class)).get();

        ConnectionTestAccess.dispatch(connection, new NextFight("F1", "F2", null, "Rojo", "Azul", 3));
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("Rojo vs Azul", corners(activity));
    }

    @Test
    public void theEndedScreenDoesNotNeedTheConnection() {
        NextFightActivity.connections = context -> {
            throw new AssertionError("la sesión ya se borró");
        };

        NextFightActivity activity = open(ended()).get();

        assertEquals("El evento terminó", title(activity));
        assertEquals(View.GONE, activity.findViewById(R.id.next_fight_loading).getVisibility());
    }

    @Test
    public void theButtonReturnsToJoinWithTheName() {
        NextFightActivity activity = open(ended()).get();

        activity.findViewById(R.id.next_fight_back).performClick();

        assertBackToJoin();
    }

    @Test
    public void returnsToJoinByItself() {
        open(ended());
        assertNull(shadowOf(app).getNextStartedActivity());

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(NextFightActivity.BACK_TO_JOIN_MS));

        assertBackToJoin();
    }

    private void assertBackToJoin() {
        Intent opened = shadowOf(app).getNextStartedActivity();
        assertNotNull(opened);
        assertEquals(JOIN, opened.getComponent().getClassName());
        assertEquals("Ana", opened.getStringExtra(NextFightActivity.EXTRA_JUDGE_NAME));
    }

    private Intent ended() {
        return new Intent(app, NextFightActivity.class).putExtra(NextFightActivity.EXTRA_EVENT_ENDED, true)
                .putExtra(NextFightActivity.EXTRA_JUDGE_NAME, "Ana");
    }

    private static ActivityController<NextFightActivity> open(Intent intent) {
        return Robolectric.buildActivity(NextFightActivity.class, intent).setup();
    }

    private static String title(NextFightActivity activity) {
        return ((TextView) activity.findViewById(R.id.next_fight_title)).getText().toString();
    }

    private static String corners(NextFightActivity activity) {
        TextView view = activity.findViewById(R.id.next_fight_corners);
        assertEquals(View.VISIBLE, view.getVisibility());
        return view.getText().toString();
    }
}
