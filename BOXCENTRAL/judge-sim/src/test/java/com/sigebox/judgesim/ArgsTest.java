package com.sigebox.judgesim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArgsTest {

    @Test
    void sixJudgesAreRejected() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> Args.parse("--judges", "6", "--script", "x.json", "--tokens", "a,b,c,d,e,f"));
        assertTrue(ex.getMessage().contains("--judges"), ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Args.parse("--judges", "0", "--script", "x.json"));
    }

    @Test
    void parsesEverythingWithDefaults() {
        var a = Args.parse("--judges", "3", "--script", "demo_data/pelea_referencia.json", "--tokens", "t1, t2,t3",
                "--drop", "60s");
        assertEquals("localhost", a.host());
        assertEquals(8765, a.port());
        assertEquals(3, a.judges());
        assertEquals(List.of("t1", "t2", "t3"), a.tokens());
        assertEquals(Duration.ofSeconds(60), a.drop());
        assertEquals(Duration.ofMinutes(30), a.timeout());

        var b = Args.parse("--host", "192.168.43.1", "--port", "9000", "--judges", "1", "--script", "s.json",
                "--tokens", "t", "--fight", "f-1", "--timeout", "500ms");
        assertEquals("192.168.43.1", b.host());
        assertEquals(9000, b.port());
        assertEquals("f-1", b.fightId());
        assertEquals(Duration.ZERO, b.drop());
        assertEquals(Duration.ofMillis(500), b.timeout());
    }

    @Test
    void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> Args.parse("--script", "s.json")); // sin --judges
        assertThrows(IllegalArgumentException.class, () -> Args.parse("--judges", "2", "--tokens", "a,b")); // sin --script
        assertThrows(IllegalArgumentException.class,
                () -> Args.parse("--judges", "2", "--script", "s", "--tokens", "a")); // un token de menos
        assertThrows(IllegalArgumentException.class,
                () -> Args.parse("--judges", "1", "--script", "s", "--tokens", "a", "--drop", "60"));
        assertThrows(IllegalArgumentException.class,
                () -> Args.parse("--judges", "1", "--script", "s", "--tokens", "a", "--port", "99999"));
        assertThrows(IllegalArgumentException.class, () -> Args.parse("--judges"));
        assertThrows(IllegalArgumentException.class, () -> Args.parse("--loquesea", "1"));
    }
}
