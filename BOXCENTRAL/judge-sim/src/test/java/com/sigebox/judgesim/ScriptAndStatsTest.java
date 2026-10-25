package com.sigebox.judgesim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ScriptAndStatsTest {

    /** demo_data/ está en la raíz del repo; las pruebas corren desde BOXCENTRAL/judge-sim. */
    static Path referenceFight() {
        var dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            var candidate = dir.resolve("demo_data/pelea_referencia.json");
            if (Files.exists(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("no se encontró demo_data/pelea_referencia.json");
    }

    @Test
    void loadsTheReferenceFight() {
        var script = Script.load(referenceFight());
        assertEquals("pelea-ref-001", script.pelea().id());
        assertEquals(4, script.pelea().rounds());
        assertEquals("María López", script.nameFor(2));
        assertEquals("Sim 5", script.nameFor(5));

        var juez2 = script.plansFor(2);
        assertEquals(4, juez2.size());
        assertTrue(juez2.get(3).late(), "en el guion el juez 2 envía tarde el round 3");
        assertTrue(script.plansFor(1).values().stream().noneMatch(Script.Plan::late));
    }

    @Test
    void percentilesUseNearestRank() {
        var stats = new LatencyStats();
        assertEquals(-1, stats.percentile(50));
        for (long ms = 1; ms <= 20; ms++) {
            stats.add(ms * 10);
        }
        assertEquals(100, stats.percentile(50));
        assertEquals(190, stats.percentile(95));
        assertEquals(200, stats.percentile(100));
        assertTrue(stats.report().contains("p95=190"), stats.report());
    }
}
