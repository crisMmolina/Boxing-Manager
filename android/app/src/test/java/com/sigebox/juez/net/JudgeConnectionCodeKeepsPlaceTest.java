package com.sigebox.juez.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.protocol.Challenge;
import com.sigebox.core.protocol.CodeInfo;
import com.sigebox.core.protocol.CodeLookup;
import com.sigebox.core.protocol.CodeUnknown;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.Hello;
import com.sigebox.core.protocol.Join;
import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.JoinUri;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.juez.data.Clock;
import com.sigebox.juez.data.Outbox;
import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.data.SessionStore;
import com.sigebox.juez.data.SigeboxDatabase;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** AND-034: un código equivocado no borra el lugar guardado ni sus tarjetas sin ACK. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class JudgeConnectionCodeKeepsPlaceTest {

    private static final String GOOD = "482913";

    private final MockWebServer server = new MockWebServer();
    private final BlockingQueue<Message> received = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> heard = new LinkedBlockingQueue<>();
    private final List<WebSocket> serverSockets = new CopyOnWriteArrayList<>();
    private SigeboxDatabase db;
    private SessionStore store;
    private Outbox outbox;
    private JudgeConnection connection;

    /** Central mínima: CHALLENGE al abrir, CODE_INFO solo para {@link #GOOD} (F1, lugar 3) y JOIN_APPROVED al JOIN. */
    private final WebSocketListener central = new WebSocketListener() {
        @Override
        public void onOpen(WebSocket ws, okhttp3.Response response) {
            serverSockets.add(ws);
            ws.send(Messages.encode(new Challenge(DeviceProof.newNonce(new SecureRandom()))));
        }

        @Override
        public void onMessage(WebSocket ws, String text) {
            Message message = Messages.decode(text);
            received.add(message);
            if (message instanceof CodeLookup lookup) {
                ws.send(Messages.encode(GOOD.equals(lookup.code())
                        ? new CodeInfo(GOOD, "F1", 3, "Roja", "Azul")
                        : new CodeUnknown(lookup.code())));
            } else if (message instanceof Join join) {
                ws.send(Messages.encode(new JoinApproved(join.fightId(), join.seat(), join.judgeName(),
                        "Roja", "Azul", 4)));
            }
        }
    };

    @Before
    public void setUp() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(central));
        server.enqueue(new MockResponse().withWebSocketUpgrade(central));
        server.start();
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SigeboxDatabase.class)
                .allowMainThreadQueries()
                .build();
        store = new SessionStore(db.sessionDao());
        outbox = new Outbox(db.outboxDao(), Clock.SYSTEM);
        connection = new JudgeConnection(new OkHttpClient(), outbox, store,
                new JudgeSessionTest.TestSigner(), Clock.SYSTEM, new Backoff(50, 200), 60_000);
        connection.addListener(new JudgeListener() {
            @Override
            public void onState(ConnectionState state) {}

            @Override
            public void onMessage(Message message) {
                heard.add(message);
            }
        });
        // Lugar guardado y aprobado (F1, lugar 3) con una tarjeta sin ACK.
        store.saveJoin(server.getHostName(), server.getPort(), "F1", 3, GOOD, "Ana Ruiz");
        store.markApproved();
    }

    @After
    public void tearDown() throws Exception {
        connection.stop();
        for (WebSocket ws : serverSockets) {
            ws.close(1000, null);
        }
        Thread.sleep(300);
        server.shutdown();
        db.close();
    }

    private Message next(BlockingQueue<Message> queue) throws InterruptedException {
        Message message = queue.poll(5, TimeUnit.SECONDS);
        if (message == null) {
            throw new AssertionError("no llegó nada");
        }
        return message;
    }

    private <T extends Message> T nextOf(BlockingQueue<Message> queue, Class<T> type) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            Message m = queue.poll(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            if (type.isInstance(m)) {
                return type.cast(m);
            }
        }
        throw new AssertionError("no llegó " + type.getSimpleName());
    }

    private void assertPlaceIsF1Seat3() {
        SessionEntity saved = store.load();
        assertNotNull("el lugar guardado sigue ahí", saved);
        assertEquals("F1", saved.fightId);
        assertEquals(3, saved.seat);
        assertEquals(GOOD, saved.token);
    }

    @Test
    public void mientrasSeBuscaElCodigoNoSeBorraElLugarNiSeMandaHello() throws Exception {
        connection.join(new JoinUri(server.getHostName(), server.getPort(), "000000", null), "Ana Ruiz");

        Message first = next(received);
        assertTrue("lo primero es la búsqueda, no un HELLO del lugar viejo: " + first, first instanceof CodeLookup);
        assertPlaceIsF1Seat3();
    }

    @Test
    public void codigoDesconocidoConservaElLugarYSusTarjetasSinAck() throws Exception {
        outbox.enqueue(new SubmitRound("F1", 3, 1, 10, 9));

        connection.join(new JoinUri(server.getHostName(), server.getPort(), "000000", null), "Ana Ruiz");

        nextOf(heard, CodeUnknown.class);
        Thread.sleep(500);
        for (Message m : received) {
            assertFalse("ni HELLO ni JOIN con el código malo: " + m, m instanceof Hello || m instanceof Join);
        }
        assertPlaceIsF1Seat3();
        assertEquals("la tarjeta sin ACK sigue en la Outbox", 1, outbox.pendingCount());
    }

    @Test
    public void codigoDelMismoLugarReenviaLasTarjetasSinAckTrasElJoin() throws Exception {
        SubmitRound card = new SubmitRound("F1", 3, 1, 10, 9);
        outbox.enqueue(card);

        connection.join(new JoinUri(server.getHostName(), server.getPort(), GOOD, null), "Ana Ruiz");

        assertTrue(next(received) instanceof CodeLookup);
        Join join = (Join) next(received);
        assertEquals("F1", join.fightId());
        assertEquals(3, (int) join.seat());
        SubmitRound resent = nextOf(received, SubmitRound.class);
        assertEquals("la misma tarjeta, con el mismo id", card.id(), resent.id());
        assertPlaceIsF1Seat3();
    }
}
