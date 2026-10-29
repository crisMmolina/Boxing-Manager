package com.sigebox.central.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.TarjetaRoundRepository;
import com.sigebox.central.server.JudgeHub;
import com.sigebox.central.server.JudgeServer;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.server.ServerEvents;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.RoundService;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.TarjetaRound;
import com.sigebox.judgesim.LatencyStats;
import com.sigebox.judgesim.Script;
import com.sigebox.judgesim.SimJudge;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * QA-001: servidor real + 5 jueces de judge-sim × 12 rounds. El juez 1 pierde la red 60 s y el
 * servidor se reinicia a mitad de pelea; al final están las 60 tarjetas (120 calificaciones, rojo
 * y azul) sin duplicados, y el p95 de {@code SUBMIT_ROUND} → {@code ACK} es ≤ 1 s.
 */
class LiveFightTest {

    private static final int JUDGES = 5;
    private static final int ROUNDS = 12;
    private static final long DROP_MS = 60_000;
    private static final int RESTART_AFTER_ROUND = 6;
    private static final long TICK_MS = 100;
    private static final long RECONNECT_MS = 1_000;

    @TempDir
    Path dir;

    private final HttpClient client = HttpClient.newHttpClient();
    private final List<SimJudge> judges = new ArrayList<>();
    private final LatencyStats stats = new LatencyStats();
    private final long[] nextConnect = new long[JUDGES];
    private volatile long dropUntil;
    private volatile int port;
    private final java.util.concurrent.ScheduledExecutorService driver = Executors.newSingleThreadScheduledExecutor();

    private Database db;
    private JudgeHub hub;
    private JudgeServer server;
    private RoundService rounds;

    @AfterEach
    void tearDown() {
        driver.shutdownNow();
        if (server != null) {
            server.close();
        }
        if (hub != null) {
            hub.close();
        }
        if (db != null) {
            db.close();
        }
    }

    @Test
    void cutOfSixtySecondsAndServerRestartLoseNoScore() throws Exception {
        var file = dir.resolve("sigebox.db");
        var tokens = new SeatTokens();
        start(file, tokens, 0);
        var fight = new FightService(db, tokens).create("Rojo", "Azul", ROUNDS, JUDGES);
        var f = fight.id();

        for (int seat = 1; seat <= JUDGES; seat++) {
            var plans = new TreeMap<Integer, Script.Plan>();
            for (int n = 1; n <= ROUNDS; n++) {
                plans.put(n, new Script.Plan(n, red(seat, n), blue(seat, n), false));
            }
            judges.add(new SimJudge(f, seat, hub.issueToken(f, seat), "Juez " + seat, plans, stats,
                    System::currentTimeMillis, line -> { }));
        }
        driver.scheduleWithFixedDelay(this::drive, 0, TICK_MS, TimeUnit.MILLISECONDS);
        await(() -> judges.stream().allMatch(j -> j.phase() == SimJudge.Phase.APPROVED), 20_000, "aprobación");

        rounds = RoundService.forHub(db, hub);
        for (int n = 1; n <= ROUNDS; n++) {
            int round = n;
            if (n == 2) {
                // Corte de Wi-Fi del juez 1 justo antes del round 2: se lo pierde y los siguientes.
                dropUntil = System.currentTimeMillis() + DROP_MS;
                judges.get(0).drop();
                await(() -> hub.connectedSeats(f).size() == JUDGES - 1, 5_000, "el corte del juez 1");
            }
            rounds.openRound(f, n);
            boolean cut = System.currentTimeMillis() < dropUntil;
            await(() -> rounds.board(f, round).missing().equals(cut ? List.of(1) : List.of()), 10_000,
                    "tarjetas del round " + n);
            rounds.closeRound(f, n, cut ? RoundService.CONFIRM_WORD : null);
            if (n == RESTART_AFTER_ROUND) {
                // Reinicio a mitad de pelea en el mismo puerto: los jueces vuelven con HELLO.
                server.close();
                hub.close();
                db.close();
                start(file, tokens, port);
                rounds = RoundService.forHub(db, hub);
                await(() -> hub.connectedSeats(f).size() == JUDGES - (System.currentTimeMillis() < dropUntil ? 1 : 0),
                        15_000, "reconexión tras el reinicio");
            }
        }

        // El juez 1 vuelve tras 60 s y manda como tardías las que se perdió.
        await(() -> new TarjetaRoundRepository(db).byPelea(f).size() == JUDGES * ROUNDS,
                DROP_MS + 30_000, "las " + JUDGES * ROUNDS + " tarjetas");
        await(() -> judges.stream().allMatch(j -> j.ownCards().size() == ROUNDS), 10_000, "STATE en los jueces");

        var cards = new TarjetaRoundRepository(db).byPelea(f);
        var keys = new HashSet<String>();
        for (TarjetaRound c : cards) {
            assertTrue(keys.add(c.juez() + ":" + c.round()), "duplicada " + c);
            assertEquals(red(c.juez(), c.round()), c.rojo(), c.toString());
            assertEquals(blue(c.juez(), c.round()), c.azul(), c.toString());
        }
        assertEquals(JUDGES * ROUNDS * 2, cards.size() * 2, "calificaciones rojo y azul");
        assertTrue(cards.stream().anyMatch(c -> c.juez() == 1 && c.estado() == EstadoTarjeta.TARDIA),
                "el juez 1 recupera rounds como tardíos");

        long p95 = stats.percentile(95);
        System.out.println("QA-001 " + stats.report());
        assertTrue(p95 <= 1_000, "p95 SUBMIT_ROUND → ACK = " + p95 + " ms");
    }

    private void start(Path file, SeatTokens tokens, int onPort) {
        db = Database.open(file);
        var holder = new JudgeHub[1];
        hub = new JudgeHub(db, tokens, new ServerEvents() {
            @Override
            public void onJoinRequested(JoinRequest r) {
                // El organizador aprueba al instante (fuera del hilo del hub).
                driver.execute(() -> holder[0].approve(r.fightId(), r.seat()));
            }
        });
        holder[0] = hub;
        server = JudgeServer.start(hub, onPort);
        port = server.port();
    }

    /** Lo que hace el bucle de JudgeSim: reconectar cada 1 s (salvo el corte) y tick. */
    private void drive() {
        try {
            long now = System.currentTimeMillis();
            for (int i = 0; i < judges.size(); i++) {
                var j = judges.get(i);
                boolean waitingDrop = i == 0 && now < dropUntil;
                if (j.phase() == SimJudge.Phase.DISCONNECTED && !waitingDrop && now >= nextConnect[i]) {
                    nextConnect[i] = now + RECONNECT_MS;
                    connect(j);
                }
                j.tick();
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void connect(SimJudge judge) {
        judge.connecting();
        var uri = URI.create("ws://localhost:" + port + JudgeServer.PATH);
        client.newWebSocketBuilder().buildAsync(uri, new WebSocket.Listener() {
            private final StringBuilder partial = new StringBuilder();
            private SimJudge.Transport transport;

            @Override
            public void onOpen(WebSocket ws) {
                transport = new SimJudge.Transport() {
                    @Override
                    public void send(String text) {
                        ws.sendText(text, true);
                    }

                    @Override
                    public void close() {
                        ws.abort();
                    }
                };
                judge.connected(transport);
                ws.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                partial.append(data);
                if (last) {
                    var text = partial.toString();
                    partial.setLength(0);
                    judge.onMessage(text);
                }
                ws.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
                judge.disconnected(transport);
                return null;
            }

            @Override
            public void onError(WebSocket ws, Throwable error) {
                judge.disconnected(transport);
            }
        }).orTimeout(5, TimeUnit.SECONDS).exceptionally(e -> {
            judge.connectFailed();
            return null;
        });
    }

    private static int red(int seat, int round) {
        return (seat + round) % 2 == 0 ? 10 : 9;
    }

    private static int blue(int seat, int round) {
        return (seat + round) % 2 == 0 ? 9 : 10;
    }

    private static void await(BooleanSupplier condition, long timeoutMs, String what) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail("tiempo agotado esperando " + what);
            }
            Thread.sleep(50);
        }
    }
}
