package com.sigebox.judgesim;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Simulador de jueces por línea de comandos (plan B de la demo y medición de latencia). Se une
 * como cada juez, espera la aprobación en la central y califica según el guion.
 */
public final class JudgeSim {

    private static final long TICK_MS = 100;
    private static final long RECONNECT_MS = 1_000;

    private JudgeSim() {
    }

    public static void main(String[] argv) throws Exception {
        Args args;
        try {
            args = Args.parse(argv);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(Args.USAGE);
            System.exit(2);
            return;
        }
        System.exit(run(args) ? 0 : 1);
    }

    /** Corre la simulación; {@code true} si todos los jueces terminaron con la pelea cerrada. */
    static boolean run(Args args) throws InterruptedException {
        var script = Script.load(args.script());
        var fightId = args.fightId() != null ? args.fightId() : script.pelea().id();
        var stats = new LatencyStats();
        var uri = URI.create("ws://" + args.host() + ":" + args.port() + "/ws");
        var client = HttpClient.newHttpClient();
        var judges = new ArrayList<SimJudge>();
        for (int seat = 1; seat <= args.judges(); seat++) {
            judges.add(new SimJudge(fightId, seat, args.tokens().get(seat - 1), script.nameFor(seat),
                    script.plansFor(seat), stats, System::currentTimeMillis, JudgeSim::log));
        }
        log("pelea " + fightId + " · " + args.judges() + " jueces · " + uri);

        long deadline = System.currentTimeMillis() + args.timeout().toMillis();
        long[] nextConnect = new long[judges.size()];
        long dropUntil = 0;
        boolean dropped = args.drop().isZero();
        while (System.currentTimeMillis() < deadline && !allDone(judges)) {
            long now = System.currentTimeMillis();
            // Corte simulado: el juez 1 se cae tras su primer ACK y vuelve pasado --drop.
            if (!dropped && judges.get(0).acks() > 0) {
                log("juez 1: corte de Wi-Fi simulado por " + args.drop().toSeconds() + " s");
                judges.get(0).drop();
                dropUntil = now + args.drop().toMillis();
                dropped = true;
            }
            for (int i = 0; i < judges.size(); i++) {
                var j = judges.get(i);
                boolean waitingDrop = i == 0 && now < dropUntil;
                if (j.phase() == SimJudge.Phase.DISCONNECTED && !waitingDrop && now >= nextConnect[i]) {
                    nextConnect[i] = now + RECONNECT_MS;
                    connect(client, uri, j);
                }
                j.tick();
            }
            Thread.sleep(TICK_MS);
        }
        log(stats.report());
        boolean ok = allDone(judges);
        if (!ok) {
            log("tiempo agotado antes de FIGHT_CLOSED");
        }
        return ok;
    }

    private static void connect(HttpClient client, URI uri, SimJudge judge) {
        judge.connecting();
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

    private static boolean allDone(List<SimJudge> judges) {
        return judges.stream().allMatch(j -> j.phase() == SimJudge.Phase.DONE);
    }

    private static void log(String line) {
        System.out.println(line);
    }
}
