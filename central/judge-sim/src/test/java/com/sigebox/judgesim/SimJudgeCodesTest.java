package com.sigebox.judgesim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.core.protocol.Challenge;
import com.sigebox.core.protocol.CodeInfo;
import com.sigebox.core.protocol.CodeLookup;
import com.sigebox.core.protocol.CodeUnknown;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.Hello;
import com.sigebox.core.protocol.Join;
import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.JoinPending;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.judgesim.SimJudgeTest.FakeTransport;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** {@code judge-sim --codes}: se une solo con el código de 6 dígitos (ADR 0004). */
class SimJudgeCodesTest {

    private final AtomicLong now = new AtomicLong(1_000);
    private final List<String> logs = new ArrayList<>();

    private static void deliver(SimJudge j, Message m) {
        j.onMessage(Messages.encode(m));
    }

    private static Challenge challenge() {
        return new Challenge(DeviceProof.newNonce(new SecureRandom()));
    }

    @Test
    void conCodigoBuscaElLugarYDespuesMandaElJoinFirmadoConEseCodigo() throws Exception {
        var j = SimJudge.withCode("482913", 1, "Ana", Map.of(), new LatencyStats(), now::get, logs::add);
        var t = new FakeTransport();
        j.connected(t);
        var ch = challenge();
        deliver(j, ch);
        var lookup = assertInstanceOf(CodeLookup.class, t.last());
        assertEquals("482913", lookup.code());
        assertNull(j.fightId());

        deliver(j, new CodeInfo("482913", "K7Q2MX", 3, "Roja", "Azul"));
        var join = assertInstanceOf(Join.class, t.last());
        assertEquals(List.of("K7Q2MX", 3, "482913"), List.of(join.fightId(), join.seat(), join.token()));
        assertEquals(ch.nonce(), join.nonce());
        assertTrue(DeviceProof.verify(join.publicKey(),
                DeviceProof.joinText("K7Q2MX", 3, "482913", join.deviceId(), ch.nonce()), join.sig()));
        assertEquals(3, j.seat());

        // Reconecta antes de la aprobación: ya conoce su lugar y manda el JOIN directo.
        deliver(j, new JoinPending("K7Q2MX", 3));
        j.disconnected(t);
        var again = new FakeTransport();
        j.connected(again);
        deliver(j, challenge());
        assertInstanceOf(Join.class, again.last());

        deliver(j, new JoinApproved("K7Q2MX", 3, "Ana", "Roja", "Azul", 3));
        j.disconnected(again);
        var back = new FakeTransport();
        j.connected(back);
        deliver(j, challenge());
        assertInstanceOf(Hello.class, back.last());
    }

    @Test
    void codigoDesconocidoTerminaAlJuez() {
        var j = SimJudge.withCode("000000", 1, "Ana", Map.of(), new LatencyStats(), now::get, logs::add);
        var t = new FakeTransport();
        j.connected(t);
        deliver(j, challenge());
        deliver(j, new CodeUnknown("000000"));
        assertEquals(SimJudge.Phase.DONE, j.phase());
        assertTrue(logs.stream().anyMatch(l -> l.contains("código")), logs.toString());
    }

    @Test
    void argsConCodigosNoPidenJuecesNiPelea() {
        var a = Args.parse("--codes", "111111, 222222,333333", "--host", "192.168.43.10", "--script", "s.json");
        assertEquals(List.of("111111", "222222", "333333"), a.codes());
        assertEquals(3, a.judges());
        assertEquals("192.168.43.10", a.host());
        assertNull(a.fightId());
        assertEquals(List.of(), a.tokens());

        assertThrows(IllegalArgumentException.class,
                () -> Args.parse("--codes", "12345", "--script", "s.json")); // no son 6 dígitos
        assertThrows(IllegalArgumentException.class,
                () -> Args.parse("--codes", "111111", "--tokens", "t", "--judges", "1", "--script", "s.json"));
        assertThrows(IllegalArgumentException.class,
                () -> Args.parse("--codes", "111111,222222", "--judges", "3", "--script", "s.json"));
    }
}
