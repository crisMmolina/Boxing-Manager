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
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.RoundService;
import com.sigebox.core.model.TarjetaRound;
import com.sigebox.judgesim.LatencyStats;
import com.sigebox.judgesim.Script;
import com.sigebox.judgesim.SimJudge;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * QA-003: servidor real + 5 jueces de judge-sim en una función de 3 peleas (la segunda con 3 jueces
 * y la tercera con 5). Todos califican las 3 sin {@code JOIN} nuevo salvo los lugares agregados, el
 * cambio de pelea con 5 jueces tarda ≤ 5 s y un reinicio del servidor a mitad de la cartelera no
 * pierde ni la cartelera ni las tarjetas.
 */
class FuncionFlowTest {

    private static final int ROUNDS = 3;
    private static final long TICK_MS = 100;
    private static final long RECONNECT_MS = 500;
    private static final long CHANGE_MAX_MS = 5_000;

    @TempDir
    Path dir;

    private final HttpClient client = HttpClient.newHttpClient();
    private final List<SimJudge> judges = new CopyOnWriteArrayList<>();
    private final LatencyStats stats = new LatencyStats();
    private final long[] nextConnect = new long[16];
    private final AtomicInteger joins = new AtomicInteger();
    private volatile int port;
    private final java.util.concurrent.ScheduledExecutorService driver = Executors.newSingleThreadScheduledExecutor();

    private Database db;
    private JudgeHub hub;
    private JudgeServer server;
    private FuncionService funciones;
    private RoundService rounds;
    private ResultService results;

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
    void threeFightsRunWithoutRepairingAndSurviveRestart() throws Exception {
        var file = dir.resolve("sigebox.db");
        var tokens = new SeatTokens();
        start(file, tokens, 0);
        var funcion = funciones.crear(ROUNDS, 5, List.of(new Esquinas("Rojo 1", "Azul 1"),
                new Esquinas("Rojo 2", "Azul 2"), new Esquinas("Rojo 3", "Azul 3")));
        funciones.cambiar(funcion.id(), 2, null, 3);

        // ---- pelea 1: 5 jueces, JOIN + aprobación una sola vez ----
        var f1 = funciones.siguiente(funcion.id()).id();
        for (int seat = 1; seat <= 5; seat++) {
            judges.add(new SimJudge(f1, seat, hub.issueToken(f1, seat), "Juez " + seat, plans(seat), stats,
                    System::currentTimeMillis, line -> { }));
        }
        driver.scheduleWithFixedDelay(this::drive, 0, TICK_MS, TimeUnit.MILLISECONDS);
        await(() -> judges.stream().allMatch(j -> j.phase() == SimJudge.Phase.APPROVED), 20_000, "aprobación");
        assertEquals(5, joins.get(), "un JOIN por juez en la pelea 1");
        play(f1, 1, ROUNDS);
        finish(f1);

        // ---- cambio 1 → 2 (5 jueces conectados; la pelea 2 tiene 3 lugares) ----
        long t0 = System.nanoTime();
        var f2 = funciones.siguiente(funcion.id()).id();
        await(() -> judges.subList(0, 3).stream().allMatch(j -> j.phase() == SimJudge.Phase.APPROVED
                && j.fightId().equals(f2)) && hub.connectedSeats(f2).equals(List.of(1, 2, 3))
                && judges.subList(3, 5).stream().allMatch(j -> j.fightId().equals(f2)
                        && j.phase() == SimJudge.Phase.DONE), CHANGE_MAX_MS, "los jueces en la pelea 2");
        long change12 = (System.nanoTime() - t0) / 1_000_000;
        assertTrue(change12 <= CHANGE_MAX_MS, "cambio 1 → 2 = " + change12 + " ms");
        assertEquals(5, joins.get(), "el cambio de pelea no pide JOIN");

        // ---- pelea 2: round 1, reinicio del servidor a mitad de la cartelera ----
        play(f2, 1, 1);
        var cartelera = funciones.cartelera(funcion.id());
        var tarjetasAntes = cards(f1, f2);
        server.close();
        hub.close();
        db.close();
        start(file, tokens, port);
        assertEquals(cartelera, funciones.cartelera(funcion.id()), "la cartelera sobrevive al reinicio");
        assertEquals(tarjetasAntes, cards(f1, f2), "las tarjetas sobreviven al reinicio");
        await(() -> hub.connectedSeats(f2).equals(List.of(1, 2, 3)), 15_000, "reconexión tras el reinicio");
        assertEquals(5, joins.get(), "el reinicio no pide JOIN");
        play(f2, 2, ROUNDS);
        finish(f2);

        // ---- cambio 2 → 3: 3 jueces siguen y 2 lugares nuevos hacen JOIN ----
        t0 = System.nanoTime();
        var f3 = funciones.siguiente(funcion.id()).id();
        for (int seat = 4; seat <= 5; seat++) {
            judges.add(new SimJudge(f3, seat, hub.issueToken(f3, seat), "Juez " + seat + "b", plans(seat), stats,
                    System::currentTimeMillis, line -> { }));
        }
        await(() -> hub.connectedSeats(f3).equals(List.of(1, 2, 3, 4, 5)) && judges.stream()
                .filter(j -> j.fightId().equals(f3)).allMatch(j -> j.phase() == SimJudge.Phase.APPROVED),
                CHANGE_MAX_MS, "los 5 jueces en la pelea 3");
        long change23 = (System.nanoTime() - t0) / 1_000_000;
        assertEquals(5 + 2, joins.get(), "solo los 2 lugares agregados hacen JOIN");
        play(f3, 1, ROUNDS);
        finish(f3);

        // ---- todo lo calificado ----
        checkCards(f1, 5);
        checkCards(f2, 3);
        checkCards(f3, 5);
        var rows = funciones.cartelera(funcion.id());
        assertEquals(3, rows.size());
        assertTrue(rows.stream().allMatch(r -> r.estado() == FuncionService.Estado.CERRADA), rows.toString());
        System.out.println("QA-003 cambio 1->2 (5 jueces conectados) = " + change12 + " ms; cambio 2->3 (5 jueces) = "
                + change23 + " ms; p95 ACK = " + stats.percentile(95) + " ms");
    }

    /** Abre y cierra los rounds {@code from..to}, esperando las tarjetas de los lugares de la pelea. */
    private void play(String fightId, int from, int to) throws InterruptedException {
        for (int n = from; n <= to; n++) {
            int round = n;
            rounds.openRound(fightId, n);
            await(() -> rounds.board(fightId, round).missing().isEmpty(), 10_000, "tarjetas del round " + n);
            rounds.closeRound(fightId, n, null);
        }
    }

    /** Termina la pelea, junta las tarjetas finales y la cierra. */
    private void finish(String fightId) throws InterruptedException {
        results.endFight(fightId);
        await(() -> results.canClose(fightId), 10_000, "tarjetas finales de " + fightId);
        results.close(fightId);
    }

    private List<TarjetaRound> cards(String... fights) {
        var all = new java.util.ArrayList<TarjetaRound>();
        for (var f : fights) {
            all.addAll(new TarjetaRoundRepository(db).byPelea(f));
        }
        all.sort(java.util.Comparator.comparing(TarjetaRound::peleaId).thenComparingInt(TarjetaRound::juez)
                .thenComparingInt(TarjetaRound::round));
        return all;
    }

    private void checkCards(String fightId, int seats) throws InterruptedException {
        await(() -> new TarjetaRoundRepository(db).byPelea(fightId).size() == seats * ROUNDS, 10_000,
                "las tarjetas de " + fightId);
        var cards = new TarjetaRoundRepository(db).byPelea(fightId);
        var keys = new HashSet<String>();
        for (var c : cards) {
            assertTrue(keys.add(c.juez() + ":" + c.round()), "duplicada " + c);
            assertTrue(c.juez() >= 1 && c.juez() <= seats, "lugar fuera de la pelea " + c);
            assertEquals(red(c.juez(), c.round()), c.rojo(), c.toString());
            assertEquals(blue(c.juez(), c.round()), c.azul(), c.toString());
        }
        assertEquals(seats * ROUNDS, keys.size());
    }

    private static TreeMap<Integer, Script.Plan> plans(int seat) {
        var plans = new TreeMap<Integer, Script.Plan>();
        for (int n = 1; n <= ROUNDS; n++) {
            plans.put(n, new Script.Plan(n, red(seat, n), blue(seat, n), false));
        }
        return plans;
    }

    private void start(Path file, SeatTokens tokens, int onPort) {
        db = Database.open(file);
        var holder = new JudgeHub[1];
        hub = new JudgeHub(db, tokens, new ServerEvents() {
            @Override
            public void onJoinRequested(JoinRequest r) {
                joins.incrementAndGet();
                // El organizador aprueba al instante (fuera del hilo del hub).
                driver.execute(() -> holder[0].approve(r.fightId(), r.seat()));
            }
        });
        holder[0] = hub;
        server = JudgeServer.start(hub, onPort);
        port = server.port();
        funciones = new FuncionService(db, new FightService(db, tokens));
        funciones.alSiguiente(hub::nextFight); // como lo conecta la app
        rounds = RoundService.forHub(db, hub);
        results = ResultService.forHub(db, hub);
    }

    /** Lo que hace el bucle de JudgeSim: reconectar y tick. */
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
            Thread.sleep(20);
        }
    }
}
