package com.sigebox.central.service;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.db.TarjetaRoundRepository;
import com.sigebox.central.db.VersionTarjetaRepository;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaRound;
import com.sigebox.core.model.VersionTarjeta;
import com.sigebox.core.validation.ScoreValidator;
import java.util.function.LongSupplier;

/**
 * Corrección de una tarjeta o captura desde la tarjeta de papel, hechas por el organizador. Cada
 * cambio exige motivo, crea una {@link VersionTarjeta} con los valores anteriores y queda en la
 * bitácora. No se permite con la pelea cerrada.
 */
public final class CorrectionService {

    private final Database db;
    private final LongSupplier clock;
    private final PeleaRepository peleas;
    private final RoundRepository rounds;
    private final TarjetaRoundRepository tarjetas;
    private final VersionTarjetaRepository versiones;
    private final EventLog log;

    public CorrectionService(Database db, LongSupplier clock) {
        this.db = db;
        this.clock = clock;
        this.peleas = new PeleaRepository(db);
        this.rounds = new RoundRepository(db);
        this.tarjetas = new TarjetaRoundRepository(db);
        this.versiones = new VersionTarjetaRepository(db);
        this.log = new EventLog(db);
    }

    /** Corrige una tarjeta que ya existe; queda {@code CORREGIDA}. */
    public synchronized TarjetaRound correct(String fightId, int seat, int round, int red, int blue, String motivo) {
        var previous = tarjetas.find(fightId, seat, round)
                .orElseThrow(() -> new IllegalStateException("no hay tarjeta del juez " + seat + " en el round " + round
                        + "; usa la captura desde papel"));
        if (previous.rojo() == red && previous.azul() == blue) {
            throw new IllegalArgumentException("los valores no cambian");
        }
        return write(fightId, seat, round, red, blue, motivo, EstadoTarjeta.CORREGIDA, previous, "CARD_CORRECTED");
    }

    /** Captura desde papel una tarjeta que no llegó (round abierto o cerrado); queda {@code CAPTURADA}. */
    public synchronized TarjetaRound capture(String fightId, int seat, int round, int red, int blue, String motivo) {
        if (tarjetas.find(fightId, seat, round).isPresent()) {
            throw new IllegalStateException("ya hay tarjeta del juez " + seat + " en el round " + round
                    + "; usa la corrección");
        }
        var estado = rounds.find(fightId, round).map(Round::estado).orElse(EstadoRound.PENDIENTE);
        if (estado == EstadoRound.PENDIENTE) {
            throw new IllegalStateException("el round " + round + " no se ha abierto");
        }
        return write(fightId, seat, round, red, blue, motivo, EstadoTarjeta.CAPTURADA, null, "CARD_CAPTURED");
    }

    private TarjetaRound write(String fightId, int seat, int round, int red, int blue, String motivo,
            EstadoTarjeta estado, TarjetaRound previous, String logType) {
        var pelea = peleas.find(fightId).orElseThrow(() -> new IllegalArgumentException("pelea desconocida"));
        if (pelea.estado() == EstadoPelea.CERRADA) {
            throw new IllegalStateException("la pelea ya está cerrada");
        }
        var reason = TerminationService.clean(motivo, "el motivo");
        if (reason == null) {
            throw new IllegalArgumentException("el motivo es obligatorio");
        }
        long now = clock.getAsLong();
        var card = new TarjetaRound(fightId, seat, round, red, blue, estado, OrigenTarjeta.ORGANIZADOR, now);
        var error = ScoreValidator.validar(pelea, card);
        if (error.isPresent()) {
            throw new IllegalArgumentException("tarjeta inválida: " + error.get());
        }
        db.inTransaction(c -> {
            tarjetas.save(card);
            versiones.append(new VersionTarjeta(fightId, seat, round,
                    previous == null ? null : previous.rojo(), previous == null ? null : previous.azul(),
                    red, blue, OrigenTarjeta.ORGANIZADOR, reason, now));
            log.append(fightId, logType, "juez " + seat + " round " + round
                    + (previous == null ? "" : " " + previous.rojo() + "-" + previous.azul() + " →") + " " + red + "-"
                    + blue);
            return null;
        });
        return card;
    }
}
