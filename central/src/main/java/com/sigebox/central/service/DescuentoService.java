package com.sigebox.central.service;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.DescuentoRepository;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.server.ServerEvents;
import com.sigebox.core.model.Descuento;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.Pelea;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Punto menos a una esquina (ADR 0005). Vive solo en la central: se guarda antes de mostrarse, anular marca
 * sin borrar y las dos cosas quedan en la bitácora. No toca tarjetas firmadas ni el protocolo de los jueces.
 */
public final class DescuentoService {

    private final Database db;
    private final ServerEvents events;
    private final LongSupplier clock;
    private final PeleaRepository peleas;
    private final DescuentoRepository descuentos;
    private final EventLog log;

    public DescuentoService(Database db, ServerEvents events, LongSupplier clock) {
        this.db = db;
        this.events = events;
        this.clock = clock;
        this.peleas = new PeleaRepository(db);
        this.descuentos = new DescuentoRepository(db);
        this.log = new EventLog(db);
    }

    /** Si todavía se pueden poner o anular descuentos: la pelea existe y no ha terminado. */
    public synchronized boolean abierta(String fightId) {
        return peleas.find(fightId).filter(DescuentoService::sinTerminar).isPresent();
    }

    /** Quita un punto a {@code esquina} en {@code round}. No hay límite de veces. Devuelve el id del descuento. */
    public synchronized long poner(String fightId, int round, Esquina esquina) {
        var pelea = peleaSinTerminar(fightId);
        if (esquina == null) {
            throw new IllegalArgumentException("falta la esquina");
        }
        if (round < 1 || round > pelea.rounds()) {
            throw new IllegalArgumentException("round fuera de rango: " + round + " (1–" + pelea.rounds() + ")");
        }
        long now = clock.getAsLong();
        long id = db.inTransaction(c -> {
            long nuevo = descuentos.save(fightId, round, esquina, now);
            log.append(fightId, "DESCUENTO", "round " + round + " " + esquina);
            return nuevo;
        });
        events.onDescuentoChanged(fightId);
        return id;
    }

    /** Anula un descuento vigente. El motivo es obligatorio; el renglón se queda marcado. */
    public synchronized void anular(long id, String motivo) {
        var descuento = descuentos.find(id).orElseThrow(() -> new IllegalArgumentException("descuento desconocido"));
        peleaSinTerminar(descuento.fightId());
        if (descuento.anulado()) {
            throw new IllegalStateException("el descuento ya está anulado");
        }
        var razon = TerminationService.clean(motivo, "el motivo");
        if (razon == null) {
            throw new IllegalArgumentException("anular exige un motivo");
        }
        long now = clock.getAsLong();
        db.inTransaction(c -> {
            descuentos.anular(id, razon, now);
            log.append(descuento.fightId(), "DESCUENTO_ANULADO",
                    "round " + descuento.round() + " " + descuento.esquina() + " " + razon);
            return null;
        });
        events.onDescuentoChanged(descuento.fightId());
    }

    /** Los descuentos de la pelea, anulados incluidos, en orden. */
    public synchronized List<Descuento> listar(String fightId) {
        return descuentos.byPelea(fightId);
    }

    private Pelea peleaSinTerminar(String fightId) {
        var pelea = peleas.find(fightId).orElseThrow(() -> new IllegalArgumentException("pelea desconocida"));
        if (!sinTerminar(pelea)) {
            throw new IllegalStateException("la pelea ya terminó");
        }
        return pelea;
    }

    private static boolean sinTerminar(Pelea pelea) {
        return pelea.estado() != EstadoPelea.TERMINADA && pelea.estado() != EstadoPelea.CERRADA;
    }
}
