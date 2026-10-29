package com.sigebox.juez.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.protocol.FightEnded;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Ping;
import com.sigebox.core.protocol.RoundOpened;
import com.sigebox.core.protocol.State;
import com.sigebox.juez.data.Clock;
import com.sigebox.juez.data.Outbox;
import com.sigebox.juez.data.SessionStore;
import com.sigebox.juez.data.SigeboxDatabase;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** AND-014: un listener que llega tarde recibe el último STATE y el fin de pelea que ya llegaron. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ConnectionReplayTest {

    private SigeboxDatabase db;
    private JudgeConnection connection;

    @Before
    public void setUp() throws Exception {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SigeboxDatabase.class)
                .allowMainThreadQueries()
                .build();
        connection = new JudgeConnection(new OkHttpClient(), new Outbox(db.outboxDao(), Clock.SYSTEM),
                new SessionStore(db.sessionDao()), new JudgeSessionTest.TestSigner(), Clock.SYSTEM,
                new Backoff(50, 200), 60_000);
    }

    @After
    public void tearDown() {
        db.close();
    }

    private static State state(int round, EstadoRound status) {
        return new State("F1", 2, "María López", "Roja", "Azul", 4, EstadoPelea.EN_CURSO, round, status,
                List.of(), false);
    }

    private List<Message> listen() {
        List<Message> got = new ArrayList<>();
        connection.addListener(new JudgeListener() {
            @Override
            public void onMessage(Message message) {
                got.add(message);
            }
        });
        return got;
    }

    @Test
    public void fightEndedSinListenersLlegaAlQueSeRegistraDespues() {
        connection.dispatch(new FightEnded("F1", null));
        List<Message> got = listen();
        assertTrue(got.stream().anyMatch(m -> m instanceof FightEnded));
    }

    @Test
    public void elUltimoStateLlegaAlQueSeRegistraDespuesConElRoundAlDia() {
        connection.dispatch(state(1, EstadoRound.ABIERTO));
        connection.dispatch(new RoundOpened("F1", 2));
        List<Message> got = listen();
        State replayed = (State) got.get(0);
        assertEquals(Integer.valueOf(2), replayed.currentRound());
        assertEquals(EstadoRound.ABIERTO, replayed.roundStatus());
    }

    @Test
    public void soloSeGuardaLoQueLlegoAEsteTelefonoYNadaMas() {
        connection.dispatch(new Ping(1L));
        assertTrue("sin STATE ni fin de pelea no hay nada que repetir", listen().isEmpty());
    }

    @Test
    public void losListenersYaRegistradosNoRecibenRepetidos() {
        List<Message> first = listen();
        connection.dispatch(new FightEnded("F1", null));
        listen();
        assertEquals(1, first.size());
    }
}
