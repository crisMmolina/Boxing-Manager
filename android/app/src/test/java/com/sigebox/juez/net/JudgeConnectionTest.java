package com.sigebox.juez.net;

import static org.junit.Assert.assertEquals;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Extremo a extremo contra un servidor WebSocket local: la central se cae y el juez se reconecta solo. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class JudgeConnectionTest {

    private final MockWebServer server = new MockWebServer();
    private final BlockingQueue<Message> received = new LinkedBlockingQueue<>();
    private final List<WebSocket> serverSockets = new CopyOnWriteArrayList<>();
    private SigeboxDatabase db;
    private JudgeConnection connection;
    /** Si no es nulo, la central no acepta conexiones nuevas hasta que se abra (simula que sigue caída). */
    private volatile CountDownLatch centralDown;

    /** Una "central" mínima: CHALLENGE al abrir, STATE al HELLO, ACK a cada SUBMIT_ROUND. */
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
            if (message instanceof Hello) {
                ws.send(Messages.encode(new State("F1", 2, "María López", "Roja", "Azul", 4,
                        EstadoPelea.EN_CURSO, 3, EstadoRound.ABIERTO, List.of(), false)));
            } else if (message instanceof SubmitRound) {
                ws.send(Messages.encode(new Ack(message.id(), true, EstadoTarjeta.ENVIADA, null)));
            }
        }
    };

    @Before
    public void setUp() throws Exception {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                CountDownLatch down = centralDown;
                if (down != null) {
                    down.await(10, TimeUnit.SECONDS);
                }
                return new MockResponse().withWebSocketUpgrade(central);
            }
        });
        server.start();
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SigeboxDatabase.class)
                .allowMainThreadQueries() // solo para preparar la prueba; la conexión usa su propio hilo
                .build();
        SessionStore store = new SessionStore(db.sessionDao());
        store.saveJoin(server.getHostName(), server.getPort(), "F1", 2, "tok123", "María López");
        store.markApproved();
        connection = new JudgeConnection(new OkHttpClient(), new Outbox(db.outboxDao(), Clock.SYSTEM), store,
                new JudgeSessionTest.TestSigner(), Clock.SYSTEM, new Backoff(50, 200), 60_000);
    }

    @After
    public void tearDown() throws Exception {
        CountDownLatch down = centralDown;
        if (down != null) {
            down.countDown();
        }
        connection.stop();
        for (WebSocket ws : serverSockets) {
            ws.close(1000, null);
        }
        Thread.sleep(300); // deja terminar el cierre de los sockets antes de apagar el servidor
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

    private void awaitPending(int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (connection.current().pendingAcks != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(expected, connection.current().pendingAcks);
    }

    private void awaitStatus(ConnectionState.Status expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (connection.current().status != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
        assertEquals(expected, connection.current().status);
    }

    @Test
    public void reconectaSoloYEntregaLoCalificadoDuranteElCorte() throws Exception {
        connection.start();
        assertEquals(Hello.TYPE, next().type());
        assertEquals("/ws", server.takeRequest().getPath());
        awaitStatus(ConnectionState.Status.CONNECTED);

        // La central se cae y sigue caída hasta que la prueba la levanta. Antes se calificaba justo después de
        // close(): si el cliente aún no procesaba el cierre, el SUBMIT_ROUND salía por el socket viejo, la central
        // lo recibía sin poder mandar el ACK y la bandeja lo reenviaba al reconectar (SUBMIT_ROUND, HELLO,
        // SUBMIT_ROUND). Eso es correcto en la app (al menos una vez); lo inestable era la prueba.
        centralDown = new CountDownLatch(1);
        serverSockets.get(0).close(1001, "central caida");
        awaitStatus(ConnectionState.Status.DISCONNECTED);

        // Mientras, el juez califica: queda en la bandeja.
        SubmitRound submit = new SubmitRound("F1", 2, 3, 10, 9);
        connection.submit(submit);
        awaitPending(1);
        centralDown.countDown();

        assertEquals("se reconecta y vuelve a presentarse", Hello.TYPE, next().type());
        Message delivered = next();
        assertEquals(submit.id(), delivered.id());
        awaitPending(0);
        assertEquals(ConnectionState.Status.CONNECTED, connection.current().status);
        assertTrue(received.isEmpty());
    }
}
