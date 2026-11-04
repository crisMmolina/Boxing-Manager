package com.sigebox.juez.finalcard;

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
import com.sigebox.core.protocol.SubmitFinalCard;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.juez.data.Clock;
import com.sigebox.juez.data.Outbox;
import com.sigebox.juez.data.SessionStore;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.net.Backoff;
import com.sigebox.juez.net.DeviceSigner;
import com.sigebox.juez.net.JudgeConnection;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

/**
 * El teléfono estaba desconectado cuando terminó la pelea, con el round 3 sin {@code ACK}: al reconectar, la bandeja
 * reenvía el round 3 y, con su {@code ACK} y el {@code STATE} terminado con {@code finalCardSent = false}, el
 * teléfono manda su tarjeta final sola; si la central se cae antes del {@code ACK},
 * la bandeja la reenvía (la misma, no otra) al volver.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class AutoFinalCardReconnectTest {

    private final MockWebServer server = new MockWebServer();
    private final BlockingQueue<SubmitFinalCard> finals = new LinkedBlockingQueue<>();
    private final List<WebSocket> serverSockets = new CopyOnWriteArrayList<>();
    /** La primera tarjeta final se pierde: la central se cae sin mandar el ACK. */
    private final AtomicBoolean dropFirst = new AtomicBoolean(true);
    private volatile boolean finalAcked;
    private SigeboxDatabase db;
    private JudgeConnection connection;
    private TestSigner signer;

    private final WebSocketListener central = new WebSocketListener() {
        @Override
        public void onOpen(WebSocket ws, okhttp3.Response response) {
            serverSockets.add(ws);
            ws.send(Messages.encode(new Challenge(DeviceProof.newNonce(new SecureRandom()))));
        }

        @Override
        public void onMessage(WebSocket ws, String text) {
            Message message = Messages.decode(text);
            if (message instanceof Hello) {
                ws.send(Messages.encode(new State("F1", 2, "María López", "Roja", "Azul", 3,
                        EstadoPelea.TERMINADA, 3, EstadoRound.CERRADO, List.of(), finalAcked)));
            } else if (message instanceof SubmitRound) {
                ws.send(Messages.encode(new Ack(message.id(), true, EstadoTarjeta.ENVIADA, null)));
            } else if (message instanceof SubmitFinalCard card) {
                finals.add(card);
                if (dropFirst.getAndSet(false)) {
                    ws.cancel();
                } else {
                    finalAcked = true;
                    ws.send(Messages.encode(new Ack(card.id(), true, null, null)));
                }
            }
        }
    };

    @Before
    public void setUp() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(central));
        server.enqueue(new MockResponse().withWebSocketUpgrade(central));
        server.enqueue(new MockResponse().withWebSocketUpgrade(central));
        server.start();
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SigeboxDatabase.class)
                .allowMainThreadQueries() // solo para preparar la prueba; la conexión usa su propio hilo
                .build();
        Outbox outbox = new Outbox(db.outboxDao(), Clock.SYSTEM);
        for (int round = 1; round <= 3; round++) {
            SubmitRound submit = new SubmitRound("F1", 2, round, 10, 9);
            outbox.enqueue(submit);
            if (round < 3) {
                // El round 3 se calificó sin conexión: queda en la bandeja sin ACK.
                outbox.onAck(new Ack(submit.id(), true, EstadoTarjeta.ENVIADA, null));
            }
        }
        SessionStore store = new SessionStore(db.sessionDao());
        store.saveJoin(server.getHostName(), server.getPort(), "F1", 2, "tok123", "María López");
        store.markApproved();
        signer = new TestSigner();
        connection = new JudgeConnection(new OkHttpClient(), outbox, store, signer, Clock.SYSTEM,
                new Backoff(50, 200), 60_000);
        connection.addListener(new AutoFinalCard(() -> db.getOpenHelper().getReadableDatabase(), store::load, signer::sign,
                connection::submit, Executors.newSingleThreadExecutor()));
    }

    @After
    public void tearDown() throws Exception {
        connection.stop();
        for (WebSocket ws : serverSockets) {
            ws.close(1000, null);
        }
        Thread.sleep(300); // deja terminar el cierre de los sockets antes de apagar el servidor
        server.shutdown();
        db.close();
    }

    private SubmitFinalCard next() throws InterruptedException {
        SubmitFinalCard card = finals.poll(5, TimeUnit.SECONDS);
        if (card == null) {
            throw new AssertionError("la central no recibió la tarjeta final");
        }
        return card;
    }

    @Test
    public void alReconectarMandaElUltimoRoundYLuegoLaTarjetaFinalFirmadaUnaSolaVez() throws Exception {
        connection.start();

        SubmitFinalCard first = next();
        List<State.OwnCard> own = List.of(new State.OwnCard(1, 10, 9, EstadoTarjeta.ENVIADA),
                new State.OwnCard(2, 10, 9, EstadoTarjeta.ENVIADA), new State.OwnCard(3, 10, 9, EstadoTarjeta.ENVIADA));
        assertTrue("firmada sin acción del juez", DeviceProof.verify(signer.publicKey(),
                DeviceProof.finalCardText("F1", 2, own), first.sig()));

        SubmitFinalCard resent = next();
        assertEquals("la bandeja reenvía la misma tarjeta, no arma otra", first.id(), resent.id());

        Thread.sleep(500);
        assertEquals("con el ACK ya no se manda otra", 0, finals.size());
    }

    private static final class TestSigner implements DeviceSigner {
        private final KeyPair keys;

        TestSigner() throws Exception {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            keys = gen.generateKeyPair();
        }

        @Override
        public String deviceId() {
            return DeviceProof.fingerprint(publicKey());
        }

        @Override
        public String publicKey() {
            return DeviceProof.encodePublicKey(keys.getPublic());
        }

        @Override
        public String sign(String text) {
            try {
                Signature signature = Signature.getInstance("SHA256withECDSA");
                signature.initSign(keys.getPrivate());
                signature.update(text.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(signature.sign());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
