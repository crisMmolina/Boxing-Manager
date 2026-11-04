package com.sigebox.juez.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.protocol.Challenge;
import com.sigebox.core.protocol.CodeInfo;
import com.sigebox.core.protocol.CodeLookup;
import com.sigebox.core.protocol.CodeUnknown;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.Join;
import com.sigebox.core.protocol.JoinPending;
import com.sigebox.core.protocol.JoinUri;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
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

/** Unirse con el código de lugar: CODE_LOOKUP → CODE_INFO → JOIN firmado de siempre (ADR 0004). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class JudgeConnectionCodeTest {

    private static final String GOOD = "482913";

    private final MockWebServer server = new MockWebServer();
    private final BlockingQueue<Message> received = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> heard = new LinkedBlockingQueue<>();
    private final List<WebSocket> serverSockets = new CopyOnWriteArrayList<>();
    private final List<String> nonces = new CopyOnWriteArrayList<>();
    private SigeboxDatabase db;
    private SessionStore store;
    private JudgeConnection connection;

    /** Central mínima: CHALLENGE al abrir, CODE_INFO solo para {@link #GOOD} y JOIN_PENDING al JOIN. */
    private final WebSocketListener central = new WebSocketListener() {
        @Override
        public void onOpen(WebSocket ws, okhttp3.Response response) {
            serverSockets.add(ws);
            String nonce = DeviceProof.newNonce(new SecureRandom());
            nonces.add(nonce);
            ws.send(Messages.encode(new Challenge(nonce)));
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
                ws.send(Messages.encode(new JoinPending(join.fightId(), join.seat())));
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
        connection = new JudgeConnection(new OkHttpClient(), new Outbox(db.outboxDao(), Clock.SYSTEM), store,
                new JudgeSessionTest.TestSigner(), Clock.SYSTEM, new Backoff(50, 200), 60_000);
        connection.addListener(new JudgeListener() {
            @Override
            public void onState(ConnectionState state) {}

            @Override
            public void onMessage(Message message) {
                heard.add(message);
            }
        });
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

    @Test
    public void conCodigoBuscaElLugarYMandaElJoinFirmadoDeSiempre() throws Exception {
        connection.join(new JoinUri(server.getHostName(), server.getPort(), GOOD, null), "Ana Ruiz");

        CodeLookup lookup = (CodeLookup) next(received);
        assertEquals(GOOD, lookup.code());

        Join join = (Join) next(received);
        assertEquals("F1", join.fightId());
        assertEquals(3, (int) join.seat());
        assertEquals(GOOD, join.token());
        assertEquals("Ana Ruiz", join.judgeName());
        assertEquals(nonces.get(0), join.nonce());
        assertTrue("firmado sobre el CHALLENGE", DeviceProof.verify(join.publicKey(),
                DeviceProof.joinText("F1", 3, GOOD, join.deviceId(), join.nonce()), join.sig()));

        CodeInfo info = nextOf(heard, CodeInfo.class);
        assertEquals(3, (int) info.seat());
        SessionEntity saved = store.load();
        assertEquals("F1", saved.fightId);
        assertEquals(3, saved.seat);
        assertEquals(GOOD, saved.token);
        assertEquals(server.getHostName(), saved.host);
    }

    @Test
    public void codigoDesconocidoSeReintentaUnaVezYLuegoSeAvisaSinJoin() throws Exception {
        connection.join(new JoinUri(server.getHostName(), server.getPort(), "000000", null), "Ana");

        assertEquals("000000", ((CodeLookup) next(received)).code());
        assertEquals("reintento por si la central estaba ocupada", "000000", ((CodeLookup) next(received)).code());
        CodeUnknown unknown = nextOf(heard, CodeUnknown.class);
        assertEquals("000000", unknown.code());

        Thread.sleep(1_500);
        assertNull("sin JOIN ni más búsquedas", received.poll());
        assertNull("no se guarda ningún lugar", store.load());
    }
}
