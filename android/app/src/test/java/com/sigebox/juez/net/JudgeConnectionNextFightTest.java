package com.sigebox.juez.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Challenge;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.Hello;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.NextFight;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.juez.data.Clock;
import com.sigebox.juez.data.Outbox;
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

/** AND-013 extremo a extremo con una central de prueba (MockWebServer): el juez pasa solo a la pelea siguiente. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class JudgeConnectionNextFightTest {

    private final MockWebServer server = new MockWebServer();
    private final BlockingQueue<Message> received = new LinkedBlockingQueue<>();
    private final List<WebSocket> serverSockets = new CopyOnWriteArrayList<>();
    private final BlockingQueue<Message> heard = new LinkedBlockingQueue<>();
    private volatile boolean holdAcks;
    private volatile String pendingAckId;
    private SigeboxDatabase db;
    private SessionStore store;
    private JudgeConnection connection;
    private JudgeSessionTest.TestSigner signer;

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
            if (message instanceof Hello hello) {
                ws.send(Messages.encode(new State(hello.fightId(), hello.seat(), "María López", "Roja", "Azul", 4,
                        EstadoPelea.EN_CURSO, 1, EstadoRound.ABIERTO, List.of(), false)));
            } else if (message instanceof SubmitRound) {
                if (holdAcks) {
                    pendingAckId = message.id();
                } else {
                    ws.send(Messages.encode(new Ack(message.id(), true, EstadoTarjeta.ENVIADA, null)));
                }
            }
        }
    };

    @Before
    public void setUp() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(central));
        server.enqueue(new MockResponse().withWebSocketUpgrade(central));
        server.start();
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SigeboxDatabase.class)
                .allowMainThreadQueries().build();
        store = new SessionStore(db.sessionDao());
        store.saveJoin(server.getHostName(), server.getPort(), "F1", 2, "tok123", "María López");
        store.markApproved();
        signer = new JudgeSessionTest.TestSigner();
        connection = new JudgeConnection(new OkHttpClient(), new Outbox(db.outboxDao(), Clock.SYSTEM), store, signer,
                Clock.SYSTEM, new Backoff(50, 200), 60_000);
        connection.addListener(new JudgeListener() {
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

    private Message next() throws InterruptedException {
        Message message = received.poll(5, TimeUnit.SECONDS);
        if (message == null) {
            throw new AssertionError("la central no recibió nada");
        }
        return message;
    }

    @Test
    public void tras_NextFight_reconectaConHelloFirmadoALaPeleaNuevaConLaMismaLlave() throws Exception {
        connection.start();
        Hello first = (Hello) next();
        assertEquals("F1", first.fightId());

        serverSockets.get(0).send(Messages.encode(new NextFight("F1", "F2", 3, "Rojo 2", "Azul 2", 6)));

        Hello second = (Hello) next();
        assertEquals("F2", second.fightId());
        assertEquals(Integer.valueOf(3), second.seat());
        assertEquals(first.deviceId(), second.deviceId());
        assertTrue(DeviceProof.verify(signer.publicKey(),
                DeviceProof.helloText("F2", 3, signer.deviceId(), second.nonce()), second.sig()));
        assertEquals("F2", store.load().fightId);
        assertTrue(store.load().approved);
    }

    @Test
    public void laBandejaDeLaPeleaVieja_seEntregaAntesDeCambiar() throws Exception {
        holdAcks = true;
        connection.start();
        next(); // HELLO F1
        SubmitRound submit = new SubmitRound("F1", 2, 1, 10, 9);
        connection.submit(submit);
        assertEquals(submit.id(), next().id());

        serverSockets.get(0).send(Messages.encode(new NextFight("F1", "F2", 3, "Rojo 2", "Azul 2", 6)));
        // La bandeja se reenvía a la pelea vieja, pero no hay HELLO a F2 mientras falte el ACK.
        Message resent = received.poll(600, TimeUnit.MILLISECONDS);
        assertEquals(submit.id(), resent.id());
        assertNull("sin ACK no cambia de pelea", received.poll(600, TimeUnit.MILLISECONDS));
        assertEquals("F1", store.load().fightId);

        serverSockets.get(0).send(Messages.encode(new Ack(pendingAckId, true, EstadoTarjeta.ENVIADA, null)));
        Hello second = (Hello) next();
        assertEquals("F2", second.fightId());
    }

    @Test
    public void sinLugar_noCambiaDePeleaYLaPantallaRecibeElAviso() throws Exception {
        connection.start();
        next(); // HELLO F1

        serverSockets.get(0).send(Messages.encode(new NextFight("F1", "F2", null, "Rojo 2", "Azul 2", 6)));

        NextFight seen = null;
        long deadline = System.currentTimeMillis() + 5_000;
        while (seen == null && System.currentTimeMillis() < deadline) {
            Message m = heard.poll(100, TimeUnit.MILLISECONDS);
            if (m instanceof NextFight nf) {
                seen = nf;
            }
        }
        assertTrue("llegó NEXT_FIGHT sin lugar", seen != null && seen.seat() == null);
        assertEquals("F1", store.load().fightId);

        // Una pantalla que se abre después también lo recibe (se recuerda hasta el siguiente aviso).
        BlockingQueue<Message> late = new LinkedBlockingQueue<>();
        connection.addListener(new JudgeListener() {
            @Override
            public void onMessage(Message message) {
                late.add(message);
            }
        });
        boolean replayed = false;
        for (Message m : late) {
            replayed |= m instanceof NextFight;
        }
        assertTrue(replayed);

        // Cuando le toque, llega el siguiente NEXT_FIGHT con lugar y el teléfono se presenta a la pelea nueva.
        serverSockets.get(0).send(Messages.encode(new NextFight("F2", "F3", 1, "Rojo 3", "Azul 3", 5)));
        Hello hello = (Hello) next();
        assertEquals("F3", hello.fightId());
        assertEquals(Integer.valueOf(1), hello.seat());
    }
}
