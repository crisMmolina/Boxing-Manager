package com.sigebox.juez.score;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.protocol.FightEnded;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.State;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.net.JudgeConnection;
import com.sigebox.juez.net.JudgeListener;
import com.sigebox.juez.net.ConnectionTestAccess;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** AND-014: la pantalla de puntuar se pone al día con lo que llegó mientras no escuchaba. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ScoreReplayTest {

    private SigeboxDatabase db;
    private JudgeConnection connection;
    private final List<Message> submitted = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SigeboxDatabase.class)
                .allowMainThreadQueries()
                .build();
        connection = ConnectionTestAccess.newConnection(db);
    }

    @After
    public void tearDown() {
        db.close();
    }

    /** Lo que hace ScoreActivity en onStart: registrarse y pasar cada mensaje al ViewModel. */
    private ScoreViewModel screenStarts() {
        ScoreViewModel vm = new ScoreViewModel(submitted::add);
        vm.setSession("F1", 2, "María López");
        connection.addListener(new JudgeListener() {
            @Override
            public void onMessage(Message message) {
                vm.onMessage(message);
            }
        });
        return vm;
    }

    @Test
    public void conLaCapturaTardiaAbiertaLlegaElFinDePeleaYAlVolverSeVe() {
        // La pantalla de puntuar está detenida (tardío arriba) y no escucha.
        ConnectionTestAccess.dispatch(connection, new FightEnded("F1", null));
        ScoreViewModel vm = screenStarts();
        assertTrue(vm.ui().getValue().fightEnded);
    }

    @Test
    public void alReabrirLaAppUnStateQueLlegoAntesDeOnStartSeAplica() {
        ConnectionTestAccess.dispatch(connection, new State("F1", 2, "María López", "Roja", "Azul", 4,
                EstadoPelea.EN_CURSO, 3, EstadoRound.ABIERTO, List.of(), false));
        ScoreViewModel vm = screenStarts();
        assertEquals(3, vm.ui().getValue().round);
        assertEquals(Integer.valueOf(4), vm.ui().getValue().rounds);
        assertTrue(vm.ui().getValue().roundOpen);
    }
}
