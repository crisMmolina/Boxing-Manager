package com.sigebox.central.ui;

import static org.junit.jupiter.api.Assertions.fail;

import com.sigebox.central.App;
import com.sigebox.central.db.Database;
import com.sigebox.central.server.JudgeHub;
import com.sigebox.central.server.JudgeServer;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.server.ServerEvents;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.RoundService;
import com.sigebox.judgesim.LatencyStats;
import com.sigebox.judgesim.Script;
import com.sigebox.judgesim.SimJudge;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Central real (servidor en un puerto libre, nunca el 8765) con jueces de judge-sim, conectada igual que
 * {@link App}: la función se crea con {@link App#funciones}. Las pruebas de pantalla de la función la usan.
 */
final class JudgeSimRig implements AutoCloseable {

    static final long TICK_MS = 100;
    static final long RECONNECT_MS = 500;

    final Database db;
    final SeatTokens tokens = new SeatTokens();
    final JudgeHub hub;
    final JudgeServer server;
    final FightService fights;
    final FuncionService funciones;
    final RoundService rounds;
    final ResultService results;
    final List<SimJudge> judges = new CopyOnWriteArrayList<>();

    private final HttpClient client = HttpClient.newHttpClient();
    private final LatencyStats stats = new LatencyStats();
    private final long[] nextConnect = new long[16];
    private final ScheduledExecutorService driver = Executors.newSingleThreadScheduledExecutor();

    JudgeSimRig(Path dbFile) {
        db = Database.open(dbFile);
        var holder = new JudgeHub[1];
        hub = new JudgeHub(db, tokens, new ServerEvents() {
            @Override
            public void onJoinRequested(JoinRequest r) {
                driver.execute(() -> holder[0].approve(r.fightId(), r.seat())); // el organizador aprueba al instante
            }
        });
        holder[0] = hub;
        server = JudgeServer.start(hub, 0);
        fights = new FightService(db, tokens);
        funciones = App.funciones(db, fights, hub);
        rounds = RoundService.forHub(db, hub);
        results = ResultService.forHub(db, hub);
        driver.scheduleWithFixedDelay(this::drive, 0, TICK_MS, TimeUnit.MILLISECONDS);
    }

    /** Un juez simulado en el lugar {@code seat} de la pelea, que hace JOIN con un token nuevo. */
    SimJudge join(String fightId, int seat, int rounds) {
        var judge = new SimJudge(fightId, seat, hub.issueToken(fightId, seat), "Juez " + seat, plans(seat, rounds),
                stats, System::currentTimeMillis, line -> { });
        judges.add(judge);
        return judge;
    }

    /** Abre y cierra los rounds {@code 1..to}, esperando las tarjetas de los lugares de la pelea. */
    void play(String fightId, int to) throws InterruptedException {
        for (int n = 1; n <= to; n++) {
            int round = n;
            rounds.openRound(fightId, n);
            await(() -> rounds.board(fightId, round).missing().isEmpty(), 10_000, "tarjetas del round " + n);
            rounds.closeRound(fightId, n, null);
        }
    }

    /** Termina la pelea, junta las tarjetas finales y la cierra. */
    void finish(String fightId) throws InterruptedException {
        results.endFight(fightId);
        await(() -> results.canClose(fightId), 10_000, "tarjetas finales de " + fightId);
        results.close(fightId);
    }

    private void drive() {
        try {
            long now = System.currentTimeMillis();
            for (int i = 0; i < judges.size(); i++) {
                var j = judges.get(i);
                if (j.phase() == SimJudge.Phase.DISCONNECTED && now >= nextConnect[i]) {
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
        var uri = URI.create("ws://localhost:" + server.port() + JudgeServer.PATH);
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

    private static TreeMap<Integer, Script.Plan> plans(int seat, int rounds) {
        var plans = new TreeMap<Integer, Script.Plan>();
        for (int n = 1; n <= rounds; n++) {
            boolean even = (seat + n) % 2 == 0;
            plans.put(n, new Script.Plan(n, even ? 10 : 9, even ? 9 : 10, false));
        }
        return plans;
    }

    static void await(BooleanSupplier condition, long timeoutMs, String what) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail("tiempo agotado esperando " + what);
            }
            Thread.sleep(20);
        }
    }

    @Override
    public void close() {
        driver.shutdownNow();
        server.close();
        hub.close();
        db.close();
    }
}
