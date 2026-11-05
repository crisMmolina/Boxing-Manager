package com.sigebox.juez.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Intent;

import androidx.room.Room;

import com.sigebox.core.protocol.EventEnded;
import com.sigebox.core.protocol.FightClosed;
import com.sigebox.core.protocol.NextFight;
import com.sigebox.juez.data.Clock;
import com.sigebox.juez.data.Outbox;
import com.sigebox.juez.data.SessionStore;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.finalcard.AutoFinalCard;
import com.sigebox.juez.wait.NextFightActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** El runtime del proceso: fin de pelea, fin de evento y tarjeta final sin depender de la pantalla abierta. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class JudgeRuntimeTest {
    private Application app;
    private SigeboxDatabase db;
    private SessionStore store;
    private JudgeRuntime runtime;

    @Before
    public void setUp() throws Exception {
        app = RuntimeEnvironment.getApplication();
        db = Room.inMemoryDatabaseBuilder(app, SigeboxDatabase.class).allowMainThreadQueries().build();
        store = new SessionStore(db.sessionDao());
        store.saveJoin("10.0.0.1", 8443, "F1", 2, "token", "Ana");
        store.markApproved();
        resetAutoFinalCard();
        runtime = new JudgeRuntime(app, db, new JudgeSessionTest.TestSigner());
    }

    @After
    public void tearDown() throws Exception {
        resetAutoFinalCard();
        db.close();
    }

    @Test
    public void eventEndedErasesTheVenueSessionAndOpensTheEndedScreenWithTheName() {
        ConnectionTestAccess.dispatch(runtime.connection(), new EventEnded("F1"));

        assertNull(store.load());
        Intent opened = shadowOf(app).getNextStartedActivity();
        assertNotNull(opened);
        assertEquals(NextFightActivity.class.getName(), opened.getComponent().getClassName());
        assertTrue(opened.getBooleanExtra(NextFightActivity.EXTRA_EVENT_ENDED, false));
        assertEquals("Ana", opened.getStringExtra(NextFightActivity.EXTRA_JUDGE_NAME));
    }

    @Test
    public void eventEndedWaitsForTheOutboxBeforeErasingTheSession() {
        new Outbox(db.outboxDao(), Clock.SYSTEM).enqueue(new FightClosed("F1"));

        ConnectionTestAccess.dispatch(runtime.connection(), new EventEnded("F1"));

        assertNotNull(store.load());
        assertNull(shadowOf(app).getNextStartedActivity());
    }

    @Test
    public void fightClosedOfItsFightOpensTheWaitScreen() {
        ConnectionTestAccess.dispatch(runtime.connection(), new FightClosed("F1"));

        Intent opened = shadowOf(app).getNextStartedActivity();
        assertNotNull(opened);
        assertEquals(NextFightActivity.class.getName(), opened.getComponent().getClassName());
        assertFalse(opened.getBooleanExtra(NextFightActivity.EXTRA_EVENT_ENDED, false));
        assertNotNull(store.load());
    }

    @Test
    public void fightClosedOfAnotherFightChangesNothing() {
        ConnectionTestAccess.dispatch(runtime.connection(), new FightClosed("F9"));

        assertNull(shadowOf(app).getNextStartedActivity());
    }

    @Test
    public void nextFightStillOpensTheScoreScreen() {
        ConnectionTestAccess.dispatch(runtime.connection(), new NextFight("F1", "F2", 2, "Rojo", "Azul", 3));

        Intent opened = shadowOf(app).getNextStartedActivity();
        assertNotNull(opened);
        assertEquals(JudgeRuntime.SCORE_ACTIVITY, opened.getComponent().getClassName());
    }

    /** La app pudo volver en otra pantalla: nadie abrió la de puntuación y la tarjeta final ya escucha. */
    @Test
    public void autoFinalCardListensOncePerProcessWithoutTheScoreScreen() throws Exception {
        assertEquals(1, autoFinalCards());

        AutoFinalCard.install(runtime);

        assertEquals(1, autoFinalCards());
    }

    private long autoFinalCards() throws Exception {
        Field field = JudgeConnection.class.getDeclaredField("listeners");
        field.setAccessible(true);
        return ((List<?>) field.get(runtime.connection())).stream().filter(AutoFinalCard.class::isInstance).count();
    }

    /** La guarda es de todo el proceso y las pruebas comparten JVM. */
    private static void resetAutoFinalCard() throws Exception {
        Field field = AutoFinalCard.class.getDeclaredField("installed");
        field.setAccessible(true);
        ((AtomicBoolean) field.get(null)).set(false);
    }
}
