package com.sigebox.central.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.Ganador;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Resultado;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaFinal;
import com.sigebox.core.model.TarjetaRound;
import com.sigebox.core.model.Terminacion;
import com.sigebox.core.model.TipoDecision;
import com.sigebox.core.model.VersionTarjeta;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Pruebas contra un archivo SQLite real en una carpeta temporal. */
class PersistenceTest {

    private static final String P = "p-1";

    @TempDir
    Path dir;

    private Database open() {
        return Database.open(dir.resolve("sigebox.db"));
    }

    private static Pelea pelea(EstadoPelea estado) {
        return new Pelea(P, "Rojo", "Azul", 3, 3, estado, "2026-10-25");
    }

    @Test
    void opensInWalWithMigrationsApplied() {
        try (var db = open()) {
            assertEquals("wal", db.pragma("journal_mode"));
            assertEquals("2", db.pragma("synchronous")); // FULL
            assertEquals("1", db.pragma("foreign_keys"));
            assertEquals(Database.MIGRATIONS.size(), db.schemaVersion());
        }
        try (var db = open()) {
            assertEquals(Database.MIGRATIONS.size(), db.schemaVersion()); // reabrir no reaplica
        }
    }

    @Test
    void tarjetaRoundSurvivesCloseAndReopen() {
        var tarjeta = new TarjetaRound(P, 2, 1, 10, 9, EstadoTarjeta.ENVIADA, OrigenTarjeta.JUEZ, 1_234L);
        try (var db = open()) {
            new PeleaRepository(db).save(pelea(EstadoPelea.EN_CURSO));
            new TarjetaRoundRepository(db).save(tarjeta);
        }
        try (var db = open()) {
            assertEquals(tarjeta, new TarjetaRoundRepository(db).find(P, 2, 1).orElseThrow());
        }
    }

    @Test
    void correctionReplacesCardAndKeepsHistory() {
        try (var db = open()) {
            new PeleaRepository(db).save(pelea(EstadoPelea.EN_CURSO));
            var cards = new TarjetaRoundRepository(db);
            var versions = new VersionTarjetaRepository(db);
            cards.save(new TarjetaRound(P, 1, 1, 10, 9, EstadoTarjeta.ENVIADA, OrigenTarjeta.JUEZ, 1L));
            versions.append(new VersionTarjeta(P, 1, 1, null, null, 10, 9, OrigenTarjeta.JUEZ, null, 1L));
            cards.save(new TarjetaRound(P, 1, 1, 10, 8, EstadoTarjeta.CORREGIDA, OrigenTarjeta.ORGANIZADOR, 2L));
            versions.append(new VersionTarjeta(P, 1, 1, 10, 9, 10, 8, OrigenTarjeta.ORGANIZADOR, "papel", 2L));

            assertEquals(8, cards.find(P, 1, 1).orElseThrow().azul());
            assertEquals(1, cards.byPelea(P).size());
            var history = versions.of(P, 1, 1);
            assertEquals(2, history.size());
            assertEquals(null, history.get(0).rojoAnterior());
            assertEquals("papel", history.get(1).motivo());
        }
    }

    @Test
    void scoresOutOfRangeAreRejectedByTheSchema() {
        try (var db = open()) {
            new PeleaRepository(db).save(pelea(EstadoPelea.EN_CURSO));
            var cards = new TarjetaRoundRepository(db);
            assertThrows(DbException.class, () -> cards.save(
                    new TarjetaRound(P, 1, 1, 11, 9, EstadoTarjeta.ENVIADA, OrigenTarjeta.JUEZ, 1L)));
            assertTrue(cards.byPelea(P).isEmpty());
        }
    }

    @Test
    void failedTransactionLeavesNothingBehind() {
        try (var db = open()) {
            new PeleaRepository(db).save(pelea(EstadoPelea.EN_CURSO));
            var processed = new ProcessedMessages(db);
            var cards = new TarjetaRoundRepository(db);
            assertThrows(DbException.class, () -> db.inTransaction(c -> {
                processed.markIfNew("m-1", P);
                cards.save(new TarjetaRound(P, 1, 1, 10, 99, EstadoTarjeta.ENVIADA, OrigenTarjeta.JUEZ, 1L));
                return null;
            }));
            assertFalse(processed.seen("m-1")); // el mensaje no quedó marcado sin su tarjeta
        }
    }

    @Test
    void processedMessagesAreIdempotent() {
        try (var db = open()) {
            var processed = new ProcessedMessages(db);
            assertTrue(processed.markIfNew("m-1", P));
            assertFalse(processed.markIfNew("m-1", P));
        }
        try (var db = open()) {
            assertFalse(new ProcessedMessages(db).markIfNew("m-1", P));
        }
    }

    @Test
    void eventLogAppendsInOrder() {
        try (var db = open()) {
            var log = new EventLog(db);
            log.append(P, "ROUND_OPENED", "1");
            log.append(P, "SUBMIT_ROUND", "juez 2");
            log.append(null, "APP_START", null);
            var events = log.byPelea(P);
            assertEquals(List.of("ROUND_OPENED", "SUBMIT_ROUND"), events.stream().map(e -> e.tipo()).toList());
        }
    }

    @Test
    void everyEntityRoundTrips() {
        try (var db = open()) {
            new PeleaRepository(db).save(pelea(EstadoPelea.TERMINADA));
            var lugar = new LugarJuez(P, 2, "María López", "Galaxy", "abc", EstadoConexion.CONECTADO, "org", 5L);
            new LugarJuezRepository(db).save(lugar);
            var round = new Round(P, 3, EstadoRound.CERRADO, 1L, 2L, true);
            new RoundRepository(db).save(round);
            var fin = new TarjetaFinal(P, 2, "sha256:ff", 9L);
            new TarjetaFinalRepository(db).save(fin);
            var term = new Terminacion(P, MotivoTerminacion.KO, null, Esquina.ROJA, 3, "1:42", "obs");
            new TerminacionRepository(db).save(term);
            var res = new Resultado(P, List.of(new Resultado.TotalJuez(2, "María López", 30, 27, Ganador.ROJA)),
                    Ganador.ROJA, TipoDecision.UNANIME, MotivoTerminacion.KO, new Resultado.Conteo(9, 0, 0, 1));
            new ResultadoRepository(db).save(res);

            assertEquals(pelea(EstadoPelea.TERMINADA), new PeleaRepository(db).find(P).orElseThrow());
            assertEquals(lugar, new LugarJuezRepository(db).find(P, 2).orElseThrow());
            assertEquals(round, new RoundRepository(db).find(P, 3).orElseThrow());
            assertEquals(List.of(fin), new TarjetaFinalRepository(db).byPelea(P));
            assertEquals(term, new TerminacionRepository(db).find(P).orElseThrow());
            assertEquals(new Gson().toJson(res), new Gson().toJson(new ResultadoRepository(db).find(P).orElseThrow()));
        }
    }

    @Test
    void recoveryPicksActiveFightAndOpenRound() {
        try (var db = open()) {
            var peleas = new PeleaRepository(db);
            peleas.save(new Pelea("vieja", "A", "B", 3, 1, EstadoPelea.CERRADA, "2026-10-24"));
            peleas.save(pelea(EstadoPelea.EN_CURSO));
            var rounds = new RoundRepository(db);
            rounds.save(new Round(P, 1, EstadoRound.CERRADO, 1L, 2L, false));
            rounds.save(new Round(P, 2, EstadoRound.CERRADO, 3L, 4L, true));
            rounds.save(new Round(P, 3, EstadoRound.PENDIENTE, null, null, false));

            var rec = new RecoveryService(db).load().orElseThrow();
            assertEquals(P, rec.pelea().id());
            assertEquals(2, rec.roundActual().orElseThrow().numero()); // ninguno abierto: el último cerrado
            assertEquals(3, rec.rounds().size());

            rounds.save(new Round(P, 3, EstadoRound.ABIERTO, 5L, null, false));
            assertEquals(3, new RecoveryService(db).load().orElseThrow().roundActual().orElseThrow().numero());

            peleas.updateEstado(P, EstadoPelea.CERRADA);
            assertTrue(new RecoveryService(db).load().isEmpty());
        }
    }

    @Test
    void writeThenKillThenReopenRecoversEverything() throws Exception {
        var file = dir.resolve("kill.db");
        var proc = new ProcessBuilder(javaExe(), "-cp", childClasspath(), KillWriter.class.getName(), file.toString())
                .redirectErrorStream(true)
                .start();
        assertTrue(proc.waitFor(60, TimeUnit.SECONDS), "el proceso hijo no terminó");
        var out = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(137, proc.exitValue(), out);
        assertTrue(out.contains("WROTE"), out);

        try (var db = Database.open(file)) {
            var rec = new RecoveryService(db).load().orElseThrow();
            assertEquals("p-kill", rec.pelea().id());
            assertEquals(2, rec.roundActual().orElseThrow().numero());
            assertEquals(EstadoRound.ABIERTO, rec.roundActual().orElseThrow().estado());
            assertEquals(1, rec.tarjetas().size());
            assertEquals(9, rec.tarjetas().get(0).azul());
            assertTrue(new ProcessedMessages(db).seen("msg-kill"));
        }
    }

    private static String javaExe() {
        return ProcessHandle.current().info().command().orElse("java");
    }

    /** Classpath del hijo armado desde las clases que usa, sin depender de cómo arranca Gradle. */
    private static String childClasspath() throws Exception {
        var entries = new LinkedHashSet<String>();
        for (var c : new ArrayList<Class<?>>(List.of(
                KillWriter.class, Database.class, Pelea.class, Gson.class, org.sqlite.JDBC.class))) {
            entries.add(new File(c.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath());
        }
        entries.addAll(List.of(System.getProperty("java.class.path").split(File.pathSeparator)));
        return String.join(File.pathSeparator, entries);
    }
}
