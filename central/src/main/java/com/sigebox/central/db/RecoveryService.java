package com.sigebox.central.db;

import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaRound;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Reconstruye la pelea activa al arrancar, tras un cierre normal o forzado. */
public final class RecoveryService {

    /**
     * Estado recuperado.
     *
     * @param roundActual el round abierto; si no hay, el último cerrado; vacío si no empezó
     */
    public record Recuperacion(
            Pelea pelea, Optional<Round> roundActual, List<Round> rounds, List<LugarJuez> lugares,
            List<TarjetaRound> tarjetas) {}

    private final PeleaRepository peleas;
    private final RoundRepository rounds;
    private final LugarJuezRepository lugares;
    private final TarjetaRoundRepository tarjetas;

    public RecoveryService(Database db) {
        this.peleas = new PeleaRepository(db);
        this.rounds = new RoundRepository(db);
        this.lugares = new LugarJuezRepository(db);
        this.tarjetas = new TarjetaRoundRepository(db);
    }

    /** La pelea no cerrada más reciente con su round actual, o vacío si no hay ninguna. */
    public Optional<Recuperacion> load() {
        return peleas.findActive().map(p -> {
            var rs = rounds.byPelea(p.id());
            return new Recuperacion(p, roundActual(rs), rs, lugares.byPelea(p.id()), tarjetas.byPelea(p.id()));
        });
    }

    static Optional<Round> roundActual(List<Round> rs) {
        var abierto = rs.stream().filter(r -> r.estado() == EstadoRound.ABIERTO).findFirst();
        if (abierto.isPresent()) {
            return abierto;
        }
        return rs.stream()
                .filter(r -> r.estado() == EstadoRound.CERRADO)
                .max(Comparator.comparingInt(Round::numero));
    }
}
