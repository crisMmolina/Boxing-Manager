package com.sigebox.central.db;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaRound;
import java.nio.file.Path;

/**
 * Proceso hijo de {@link PersistenceTest}: escribe una pelea con el round 2 abierto y una
 * tarjeta, y muere con {@link Runtime#halt} sin cerrar la conexión (como un cierre forzado).
 */
public final class KillWriter {

    private KillWriter() {
    }

    public static void main(String[] args) {
        var db = Database.open(Path.of(args[0]));
        new PeleaRepository(db).save(new Pelea("p-kill", "Rojo", "Azul", 3, 3, EstadoPelea.EN_CURSO, "2026-10-25"));
        var rounds = new RoundRepository(db);
        rounds.save(new Round("p-kill", 1, EstadoRound.CERRADO, 1_000L, 2_000L, false));
        rounds.save(new Round("p-kill", 2, EstadoRound.ABIERTO, 3_000L, null, false));
        new TarjetaRoundRepository(db)
                .save(new TarjetaRound("p-kill", 1, 2, 10, 9, EstadoTarjeta.ENVIADA, OrigenTarjeta.JUEZ, 3_500L));
        new ProcessedMessages(db).markIfNew("p-kill", 1, "msg-kill");
        System.out.println("WROTE");
        System.out.flush();
        Runtime.getRuntime().halt(137);
    }
}
