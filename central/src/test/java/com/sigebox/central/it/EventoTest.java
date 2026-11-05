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
import com.sigebox.central.service.DescuentoService;
import com.sigebox.central.service.FightService;
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Esquinas;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.RoundService;
import com.sigebox.core.model.Descuento;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.Ganador;
import com.sigebox.core.model.Resultado;
import com.sigebox.core.model.TarjetaRound;
import com.sigebox.core.model.TipoDecision;
import com.sigebox.core.protocol.EventEnded;
import com.sigebox.core.protocol.Messages;
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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * QA-004: el evento completo con servidor real y 3 jueces de judge-sim que entran solo con el código.
 * Dos peleas de 3 rounds, 2 descuentos (1 anulado), tarjetas finales automáticas y resultado igual
 * al cálculo a mano; paso a la pelea 2 sin emparejar de nuevo y {@code EVENT_ENDED} al cerrar la
 * última; corte de 60 s de un juez en la pelea 2 sin perder puntajes; y último {@code ACK} →
 * Resultado listo ≤ 1 s (p95), medido aparte sobre una cartelera larga porque 2 muestras no dan p95.
 */
class EventoTest {

    private static final int ROUNDS = 3;
    private static final int JUECES = 3;
    private static final long TICK_MS = 100;
    private static final long RECONNECT_MS = 500;
    private static final long CUT_MS = 60_000;
    private static final long READY_MAX_MS = 1_000;
    private static final int SAMPLES = FuncionService.MAX_PELEAS;

    /** Puntos {rojo, azul} por juez y round. judge-sim repite su guion en cada pelea. */
    private static final int[][][] CARDS = {
        {{10, 9}, {10, 9}, {10, 9}},
        {{10, 9}, {10, 9}, {9, 10}},
        {{9, 10}, {9, 10}, {10, 9}},
    };

    @TempDir
    Path dir;

    private final HttpClient client = HttpClient.newHttpClient();
    private final List<SimJudge> judges = new CopyOnWriteArrayList<>();
    private final LatencyStats stats = new LatencyStats();
    private final LatencyStats ackToReady = new LatencyStats();
    private final LatencyStats roundToReady = new LatencyStats();
    private final long[] nextConnect = new long[JUECES];
    private final AtomicInteger joins = new AtomicInteger();
    private final Set<Integer> eventEnded = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> finalAcks = new ConcurrentHashMap<>();
    private final Map<String, Long> lastFinalAck = new ConcurrentHashMap<>();
    private final ScheduledExecutorService driver = Executors.newSingleThreadScheduledExecutor();
    private volatile int port;
    private volatile long dropUntil;

    private Database db;
    private JudgeHub hub;
    private JudgeServer server;
    private FightService fights;
    private FuncionService funciones;
    private RoundService rounds;
    private ResultService results;
    private DescuentoService descuentos;

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
    void fullEventWithCodesDiscountsAndACutJudge() throws Exception {
        start();
        var funcion = funciones.crear(ROUNDS, JUECES,
                List.of(new Esquinas("Rojo 1", "Azul 1"), new Esquinas("Rojo 2", "Azul 2")));

        // ---- pelea 1: los jueces entran solo con el código ----
        var f1 = funciones.siguiente(funcion.id()).id();
        joinWithCodes(f1, ROUNDS);
        assertEquals(JUECES, joins.get(), "un JOIN por juez");

        playRound(f1, 1);
        rounds.openRound(f1, 2);
        long vigente = descuentos.poner(f1, 2, Esquina.ROJA);
        awaitCards(f1, 2);
        rounds.closeRound(f1, 2, null);
        rounds.openRound(f1, 3);
        long anulado = descuentos.poner(f1, 3, Esquina.AZUL);
        descuentos.anular(anulado, "puesto por error");
        awaitCards(f1, 3);
        var r1 = closeLastRound(f1);

        // A mano: el descuento de Rojo en el round 2 sale de las 3 tarjetas; sin él sería Rojo dividida.
        assertTotals(r1, new int[][] {{29, 27}, {28, 28}, {27, 29}});
        assertEquals(List.of(Ganador.ROJA, Ganador.EMPATE, Ganador.AZUL),
                r1.totales().stream().map(Resultado.TotalJuez::ganador).toList());
        assertEquals(Ganador.EMPATE, r1.ganador());
        assertEquals(TipoDecision.DIVIDIDA, r1.tipo());
        assertEquals(new Resultado.Conteo(9, 0, 0, 0), r1.conteo());
        assertEquals(new Resultado.RoundJuez(2, 9, 9), r1.totales().get(0).rounds().get(1));
        assertEquals(new Resultado.RoundJuez(3, 10, 9), r1.totales().get(0).rounds().get(2), "el anulado no resta");
        var puestos = descuentos.listar(f1);
        assertEquals(2, puestos.size(), "el anulado no se borra");
        assertEquals(List.of(anulado), puestos.stream().filter(Descuento::anulado).map(Descuento::id).toList());
        assertEquals(List.of(vigente), puestos.stream().filter(d -> !d.anulado()).map(Descuento::id).toList());

        // ---- paso a la pelea 2 sin emparejar de nuevo ----
        var f2 = funciones.siguiente(funcion.id()).id();
        awaitHandover(f2);
        assertEquals(JUECES, joins.get(), "el cambio de pelea no pide JOIN");
        assertTrue(eventEnded.isEmpty(), "EVENT_ENDED al cerrar la pelea 1: " + eventEnded);

        // ---- pelea 2: el juez 1 se corta 60 s desde antes del round 2 ----
        playRound(f2, 1);
        long cutAt = System.currentTimeMillis();
        dropUntil = cutAt + CUT_MS;
        judges.get(0).drop();
        await(() -> hub.connectedSeats(f2).equals(List.of(2, 3)), 5_000, "el corte del juez 1");

        rounds.openRound(f2, 2);
        await(() -> rounds.board(f2, 2).missing().equals(List.of(1)), 10_000, "los jueces 2 y 3 en el round 2");
        rounds.closeRound(f2, 2, RoundService.CONFIRM_WORD);
        rounds.openRound(f2, 3);
        await(() -> rounds.board(f2, 3).missing().equals(List.of(1)), 10_000, "los jueces 2 y 3 en el round 3");

        await(() -> hub.connectedSeats(f2).equals(List.of(1, 2, 3)), CUT_MS + 15_000, "la vuelta del juez 1");
        long cutMs = System.currentTimeMillis() - cutAt;
        assertTrue(cutMs >= CUT_MS, "corte de " + cutMs + " ms");
        awaitCards(f2, 3);
        await(() -> cards(f2).size() == JUECES * ROUNDS, 10_000, "las tarjetas atrasadas del juez 1");
        var r2 = closeLastRound(f2);

        // A mano: las mismas tarjetas sin descuentos. Ningún puntaje del corte se perdió.
        assertTotals(r2, new int[][] {{30, 27}, {29, 28}, {28, 29}});
        assertEquals(Ganador.ROJA, r2.ganador());
        assertEquals(TipoDecision.DIVIDIDA, r2.tipo());
        assertEquals(new Resultado.Conteo(8, 1, 0, 0), r2.conteo());
        assertTrue(r2.totales().stream().allMatch(t -> t.descuentos().isEmpty()), "descuento de otra pelea");
        checkCards(f1);
        checkCards(f2);
        var tardias = cards(f2).stream().filter(c -> c.estado() == EstadoTarjeta.TARDIA).toList();
        assertEquals(1, tardias.size(), tardias.toString());
        assertEquals(1, tardias.get(0).juez());
        assertEquals(2, tardias.get(0).round());

        // ---- fin del evento ----
        await(() -> eventEnded.size() == JUECES, 5_000, "EVENT_ENDED a los 3 jueces");
        assertEquals(JUECES, joins.get(), "ni el corte ni el fin piden JOIN");
        var rows = funciones.cartelera(funcion.id());
        assertTrue(rows.stream().allMatch(r -> r.estado() == FuncionService.Estado.CERRADA), rows.toString());
        assertTrue(ackToReady.percentile(95) <= READY_MAX_MS, ackToReady.report());
        System.out.println("QA-004 corte = " + cutMs + " ms; ultimo ACK -> Resultado listo: " + ackToReady.report()
                + "; envio -> ACK: " + stats.report());
    }

    /** Una cartelera llena de peleas de un round, para que el p95 salga de muestras y no de 2 peleas. */
    @Test
    void resultIsReadyWithinOneSecondOfTheLastAck() throws Exception {
        start();
        var cartelera = new ArrayList<Esquinas>();
        for (int i = 1; i <= SAMPLES; i++) {
            cartelera.add(new Esquinas("Rojo " + i, "Azul " + i));
        }
        var funcion = funciones.crear(1, JUECES, cartelera);
        var fight = funciones.siguiente(funcion.id()).id();
        joinWithCodes(fight, 1);
        for (int i = 1; i <= SAMPLES; i++) {
            rounds.openRound(fight, 1);
            awaitCards(fight, 1);
            closeLastRound(fight);
            if (i < SAMPLES) {
                fight = funciones.siguiente(funcion.id()).id();
                awaitHandover(fight);
            }
        }
        assertEquals(SAMPLES, ackToReady.count());
        await(() -> eventEnded.size() == JUECES, 5_000, "EVENT_ENDED a los 3 jueces");
        System.out.println("QA-004 ultimo ACK -> Resultado listo: " + ackToReady.report()
                + "; cierre del ultimo round -> Resultado listo: " + roundToReady.report());
        assertTrue(ackToReady.percentile(95) <= READY_MAX_MS, ackToReady.report());
        assertTrue(roundToReady.percentile(95) <= READY_MAX_MS, roundToReady.report());
    }

    private void joinWithCodes(String fightId, int planRounds) throws InterruptedException {
        for (int seat = 1; seat <= JUECES; seat++) {
            var code = fights.invite(fightId, seat, "localhost", port).code();
            judges.add(SimJudge.withCode(code, seat, "Juez " + seat, plans(seat, planRounds), stats,
                    System::currentTimeMillis, line -> { }));
        }
        driver.scheduleWithFixedDelay(this::drive, 0, TICK_MS, TimeUnit.MILLISECONDS);
        await(() -> judges.stream().allMatch(j -> j.phase() == SimJudge.Phase.APPROVED)
                && hub.connectedSeats(fightId).equals(List.of(1, 2, 3)), 20_000, "aprobación");
    }

    private void awaitHandover(String fightId) throws InterruptedException {
        await(() -> judges.stream().allMatch(j -> j.phase() == SimJudge.Phase.APPROVED
                && fightId.equals(j.fightId())) && hub.connectedSeats(fightId).equals(List.of(1, 2, 3)), 10_000,
                "los jueces en " + fightId);
    }

    private void awaitCards(String fightId, int round) throws InterruptedException {
        await(() -> rounds.board(fightId, round).missing().isEmpty(), 10_000, "tarjetas del round " + round);
    }

    private void playRound(String fightId, int n) throws InterruptedException {
        rounds.openRound(fightId, n);
        awaitCards(fightId, n);
        rounds.closeRound(fightId, n, null);
    }

    /**
     * Cierra el último round y nada más: la pelea termina sola, las tarjetas finales llegan sin que
     * nadie las pida y el resultado se cierra. El último {@code ACK} se toma en el servidor, justo
     * después de mandarlo; "listo" es cuando {@code close} devuelve el resultado.
     */
    private Resultado closeLastRound(String fightId) throws InterruptedException {
        int last = rounds.live(fightId).rounds();
        long closing = System.nanoTime();
        rounds.closeRound(fightId, last, null);
        while (!results.canClose(fightId)) {
            if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - closing) > 10_000) {
                fail("tiempo agotado esperando tarjetas finales de " + fightId);
            }
            Thread.sleep(1);
        }
        var resultado = results.close(fightId);
        long ready = System.nanoTime();
        await(() -> finalAcks.getOrDefault(fightId, 0) == JUECES, 5_000, "ACK de las tarjetas finales");
        ackToReady.add(Math.max(0, TimeUnit.NANOSECONDS.toMillis(ready - lastFinalAck.get(fightId))));
        roundToReady.add(TimeUnit.NANOSECONDS.toMillis(ready - closing));
        return resultado;
    }

    private static void assertTotals(Resultado resultado, int[][] expected) {
        assertEquals(expected.length, resultado.totales().size());
        for (int i = 0; i < expected.length; i++) {
            var total = resultado.totales().get(i);
            assertEquals(i + 1, total.juez());
            assertEquals(expected[i][0], total.rojo(), "rojo del juez " + (i + 1));
            assertEquals(expected[i][1], total.azul(), "azul del juez " + (i + 1));
        }
    }

    private List<TarjetaRound> cards(String fightId) {
        return new TarjetaRoundRepository(db).byPelea(fightId);
    }

    /** Las tarjetas guardadas son lo que marcó cada juez: una por juez y round, sin descontar. */
    private void checkCards(String fightId) {
        var keys = new HashSet<String>();
        for (var c : cards(fightId)) {
            assertTrue(keys.add(c.juez() + ":" + c.round()), "duplicada " + c);
            assertEquals(CARDS[c.juez() - 1][c.round() - 1][0], c.rojo(), c.toString());
            assertEquals(CARDS[c.juez() - 1][c.round() - 1][1], c.azul(), c.toString());
        }
        assertEquals(JUECES * ROUNDS, keys.size());
    }

    private static Map<Integer, Script.Plan> plans(int seat, int planRounds) {
        var plans = new TreeMap<Integer, Script.Plan>();
        for (int n = 1; n <= planRounds; n++) {
            var card = CARDS[seat - 1][n - 1];
            plans.put(n, new Script.Plan(n, card[0], card[1], false));
        }
        return plans;
    }

    private void start() {
        var tokens = new SeatTokens();
        db = Database.open(dir.resolve("sigebox.db"));
        var holder = new JudgeHub[1];
        var events = new ServerEvents() {
            @Override
            public void onJoinRequested(JoinRequest r) {
                joins.incrementAndGet();
                // El organizador aprueba al instante (fuera del hilo del hub).
                driver.execute(() -> holder[0].approve(r.fightId(), r.seat()));
            }

            @Override
            public void onFinalCard(String fightId, int seat) {
                lastFinalAck.put(fightId, System.nanoTime());
                finalAcks.merge(fightId, 1, Integer::sum);
            }
        };
        hub = new JudgeHub(db, tokens, events);
        holder[0] = hub;
        server = JudgeServer.start(hub, 0);
        port = server.port();
        fights = new FightService(db, tokens);
        funciones = new FuncionService(db, fights);
        funciones.alSiguiente(hub::nextFight); // como lo conecta la app
        rounds = RoundService.forHub(db, hub);
        results = ResultService.forHub(db, hub);
        descuentos = new DescuentoService(db, events, System::currentTimeMillis);
    }

    /** Lo que hace el bucle de JudgeSim: reconectar y tick. El juez 1 no vuelve hasta que pase el corte. */
    private void drive() {
        try {
            long now = System.currentTimeMillis();
            for (int i = 0; i < judges.size(); i++) {
                var j = judges.get(i);
                boolean cut = i == 0 && now < dropUntil;
                if (j.phase() == SimJudge.Phase.DISCONNECTED && !cut && now >= nextConnect[i]) {
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
                    if (Messages.decode(text) instanceof EventEnded) {
                        eventEnded.add(judge.seat()); // judge-sim no lo atiende: se observa aquí
                    }
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
