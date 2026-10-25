package com.sigebox.judgesim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Challenge;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.FightClosed;
import com.sigebox.core.protocol.FightEnded;
import com.sigebox.core.protocol.FinalCardHash;
import com.sigebox.core.protocol.Hello;
import com.sigebox.core.protocol.Join;
import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.JoinPending;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.Ping;
import com.sigebox.core.protocol.Pong;
import com.sigebox.core.protocol.RoundClosed;
import com.sigebox.core.protocol.RoundOpened;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitFinalCard;
import com.sigebox.core.protocol.SubmitRound;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** El juez simulado contra una central falsa que verifica firmas con {@code core}. */
class SimJudgeTest {

    private static final String F = "pelea-1";
    private static final String TOKEN = "t".repeat(22);

    private final AtomicLong now = new AtomicLong(1_000);
    private final LatencyStats stats = new LatencyStats();
    private final List<String> logs = new ArrayList<>();

    static final class FakeTransport implements SimJudge.Transport {
        final List<Message> sent = new ArrayList<>();
        boolean closed;

        @Override
        public void send(String text) {
            sent.add(Messages.decode(text));
        }

        @Override
        public void close() {
            closed = true;
        }

        <T extends Message> List<T> of(Class<T> type) {
            return sent.stream().filter(type::isInstance).map(type::cast).toList();
        }

        Message last() {
            return sent.get(sent.size() - 1);
        }
    }

    private SimJudge judge(Map<Integer, Script.Plan> plans) {
        return new SimJudge(F, 2, TOKEN, "María López", plans, stats, now::get, logs::add);
    }

    private static void deliver(SimJudge j, Message m) {
        j.onMessage(Messages.encode(m));
    }

    /** Conecta, responde el CHALLENGE con un JOIN válido y queda aprobado. */
    private FakeTransport joinAndApprove(SimJudge j) {
        var t = new FakeTransport();
        j.connected(t);
        var challenge = new Challenge(DeviceProof.newNonce(new java.security.SecureRandom()));
        deliver(j, challenge);
        var join = assertInstanceOf(Join.class, t.last());
        assertEquals(TOKEN, join.token());
        assertEquals(challenge.nonce(), join.nonce());
        assertTrue(DeviceProof.verify(join.publicKey(),
                DeviceProof.joinText(F, 2, TOKEN, join.deviceId(), challenge.nonce()), join.sig()));
        deliver(j, new JoinPending(F, 2));
        assertEquals(SimJudge.Phase.PENDING, j.phase());
        deliver(j, new JoinApproved(F, 2, "María López", "Roja", "Azul", 4));
        assertEquals(SimJudge.Phase.APPROVED, j.phase());
        return t;
    }

    @Test
    void submitsOnRoundOpenedAndRetriesWithTheSameIdUntilAck() {
        var j = judge(Map.of(1, new Script.Plan(1, 10, 9, false)));
        var t = joinAndApprove(j);

        deliver(j, new RoundOpened(F, 1));
        var first = t.of(SubmitRound.class);
        assertEquals(1, first.size());
        assertEquals(10, first.get(0).red());

        now.addAndGet(SimJudge.RETRY_MS - 1);
        j.tick();
        assertEquals(1, t.of(SubmitRound.class).size()); // aún no toca reintentar
        now.addAndGet(1);
        j.tick();
        var retries = t.of(SubmitRound.class);
        assertEquals(2, retries.size());
        assertEquals(retries.get(0).id(), retries.get(1).id());

        now.addAndGet(40);
        deliver(j, new Ack(retries.get(0).id(), true, EstadoTarjeta.ENVIADA, null));
        assertEquals(40, stats.percentile(50)); // desde el último envío
        assertEquals(List.of(new State.OwnCard(1, 10, 9, EstadoTarjeta.ENVIADA)), j.ownCards());
        now.addAndGet(SimJudge.RETRY_MS * 3);
        j.tick();
        assertEquals(2, t.of(SubmitRound.class).size()); // con ACK ya no se reenvía
    }

    @Test
    void lateRoundIsSentOnlyWhenTheCentralAsksForIt() {
        var j = judge(Map.of(3, new Script.Plan(3, 9, 10, true)));
        var t = joinAndApprove(j);

        deliver(j, new RoundOpened(F, 3));
        assertTrue(t.of(SubmitRound.class).isEmpty());
        deliver(j, new RoundClosed(F, 3, true, true));
        assertEquals(3, t.of(SubmitRound.class).get(0).round());
    }

    @Test
    void dropThenReconnectUsesHelloAndResendsPending() {
        var j = judge(Map.of(1, new Script.Plan(1, 10, 9, false), 2, new Script.Plan(2, 10, 8, false)));
        var t = joinAndApprove(j);
        deliver(j, new RoundOpened(F, 1));
        var pending = t.of(SubmitRound.class).get(0);

        j.drop();
        assertTrue(t.closed);
        assertEquals(SimJudge.Phase.DISCONNECTED, j.phase());
        j.disconnected(t); // cierre tardío de la conexión vieja: sin efecto
        assertEquals(SimJudge.Phase.DISCONNECTED, j.phase());

        var t2 = new FakeTransport();
        j.connected(t2);
        var challenge = new Challenge(DeviceProof.newNonce(new java.security.SecureRandom()));
        deliver(j, challenge);
        var hello = assertInstanceOf(Hello.class, t2.last());
        assertEquals(challenge.nonce(), hello.nonce());

        deliver(j, new State(F, 2, "María López", "Roja", "Azul", 4, com.sigebox.core.model.EstadoPelea.EN_CURSO,
                1, com.sigebox.core.model.EstadoRound.ABIERTO, List.of(), false));
        var resent = t2.of(SubmitRound.class);
        assertEquals(1, resent.size());
        assertEquals(pending.id(), resent.get(0).id()); // mismo id: la central no duplica
    }

    @Test
    void afterReconnectStateCatchesUpMissedRounds() {
        var j = judge(Map.of(1, new Script.Plan(1, 10, 9, false), 2, new Script.Plan(2, 9, 10, false),
                3, new Script.Plan(3, 10, 8, false)));
        var t = joinAndApprove(j);
        deliver(j, new RoundOpened(F, 1));
        deliver(j, new Ack(t.of(SubmitRound.class).get(0).id(), true, EstadoTarjeta.ENVIADA, null));
        j.drop(); // sin red: se pierden ROUND_CLOSED 1, ROUND_OPENED/CLOSED 2 y ROUND_OPENED 3

        var t2 = new FakeTransport();
        j.connected(t2);
        deliver(j, new Challenge(DeviceProof.newNonce(new java.security.SecureRandom())));
        deliver(j, new State(F, 2, "María López", "Roja", "Azul", 4, com.sigebox.core.model.EstadoPelea.EN_CURSO,
                3, com.sigebox.core.model.EstadoRound.ABIERTO,
                List.of(new State.OwnCard(1, 10, 9, EstadoTarjeta.ENVIADA)), false));
        assertEquals(List.of(2, 3), t2.of(SubmitRound.class).stream().map(SubmitRound::round).toList());
    }

    @Test
    void stateWithTheCardAlreadyStoredStopsTheResend() {
        var j = judge(Map.of(1, new Script.Plan(1, 10, 9, false)));
        var t = joinAndApprove(j);
        deliver(j, new RoundOpened(F, 1));
        j.drop();

        var t2 = new FakeTransport();
        j.connected(t2);
        deliver(j, new Challenge(DeviceProof.newNonce(new java.security.SecureRandom())));
        deliver(j, new State(F, 2, "María López", "Roja", "Azul", 4, com.sigebox.core.model.EstadoPelea.EN_CURSO,
                1, com.sigebox.core.model.EstadoRound.ABIERTO,
                List.of(new State.OwnCard(1, 10, 9, EstadoTarjeta.ENVIADA)), false));
        assertTrue(t2.of(SubmitRound.class).isEmpty());
        assertEquals(1, t.of(SubmitRound.class).size());
    }

    @Test
    void finalCardIsSignedOverOwnCardsAfterFightEnded() {
        var j = judge(Map.of(1, new Script.Plan(1, 10, 9, false)));
        var t = joinAndApprove(j);
        var join = t.of(Join.class).get(0);
        deliver(j, new RoundOpened(F, 1));
        deliver(j, new Ack(t.of(SubmitRound.class).get(0).id(), true, EstadoTarjeta.ENVIADA, null));

        deliver(j, new FightEnded(F, MotivoTerminacion.SIN_DECISION));
        var fin = t.of(SubmitFinalCard.class);
        assertEquals(1, fin.size());
        var cards = List.of(new State.OwnCard(1, 10, 9, EstadoTarjeta.ENVIADA));
        assertEquals(FinalCardHash.of(F, 2, cards), fin.get(0).hash());
        assertTrue(DeviceProof.verify(join.publicKey(), DeviceProof.finalCardText(F, 2, cards), fin.get(0).sig()));

        deliver(j, new Ack(fin.get(0).id(), true, null, null));
        deliver(j, new FightClosed(F));
        assertEquals(SimJudge.Phase.DONE, j.phase());
    }

    @Test
    void finalCardWaitsForPendingRounds() {
        var j = judge(Map.of(1, new Script.Plan(1, 10, 9, false)));
        var t = joinAndApprove(j);
        deliver(j, new RoundOpened(F, 1));
        deliver(j, new FightEnded(F, MotivoTerminacion.KO));
        assertTrue(t.of(SubmitFinalCard.class).isEmpty()); // falta el ACK del round 1

        deliver(j, new Ack(t.of(SubmitRound.class).get(0).id(), true, EstadoTarjeta.ENVIADA, null));
        assertEquals(1, t.of(SubmitFinalCard.class).size());
    }

    @Test
    void pingsWhilePendingAndAnswersPings() {
        var j = judge(Map.of());
        var t = new FakeTransport();
        j.connected(t);
        deliver(j, new Challenge(DeviceProof.newNonce(new java.security.SecureRandom())));
        deliver(j, new JoinPending(F, 2));
        now.addAndGet(SimJudge.PING_MS);
        j.tick();
        assertInstanceOf(Ping.class, t.last());
        deliver(j, new Ping(77L));
        assertEquals(77L, ((Pong) t.last()).ts());
    }

    @Test
    void rejectedAckIsLoggedAndNotRetried() {
        var j = judge(Map.of(1, new Script.Plan(1, 10, 9, false)));
        var t = joinAndApprove(j);
        deliver(j, new RoundOpened(F, 1));
        deliver(j, new Ack(t.of(SubmitRound.class).get(0).id(), false, null, Ack.Error.ROUND_NOT_OPEN));
        assertTrue(j.pendingIds().isEmpty());
        assertTrue(logs.stream().anyMatch(l -> l.contains("ROUND_NOT_OPEN")), logs.toString());
    }
}
