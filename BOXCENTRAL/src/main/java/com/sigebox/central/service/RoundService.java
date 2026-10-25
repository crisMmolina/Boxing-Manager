package com.sigebox.central.service;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.db.TarjetaRoundRepository;
import com.sigebox.central.server.JudgeHub;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaRound;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

/**
 * Control de rounds de la pelea activa. Persiste cada cambio y solo después lo difunde a los
 * jueces. Nunca entrega totales ni ganador, salvo {@link #partialSummary} con confirmación, que
 * queda en la bitácora.
 */
public final class RoundService {

    /** Palabra que el organizador escribe para cerrar un round con jueces faltantes. */
    public static final String CONFIRM_WORD = "AVANZAR";

    /** Segundos que la UI muestra el resumen parcial antes de ocultarlo. */
    public static final int SUMMARY_VISIBLE_SECONDS = 15;

    /** Lo que la central difunde a los teléfonos; en la app es {@link JudgeHub}. */
    public interface Broadcaster {
        void roundOpened(String fightId, int round);

        void roundClosed(String fightId, int round, boolean forced);
    }

    /** Casilla de un juez en el tablero del round. */
    public enum Casilla { PENDIENTE, ENVIADO, TARDIO, CORREGIDO }

    public record Slot(int seat, String judgeName, Casilla casilla) {}

    /** Tablero de un round: una casilla por lugar, sin valores ni totales. */
    public record Board(int round, EstadoRound estado, List<Slot> slots) {
        public List<Integer> missing() {
            return slots.stream().filter(s -> s.casilla() == Casilla.PENDIENTE).map(Slot::seat).toList();
        }
    }

    /** Total parcial de un juez en los rounds cerrados. */
    public record PartialTotal(int seat, String judgeName, int red, int blue) {}

    /** Cierre con faltantes sin la confirmación escrita. */
    public static final class ConfirmationRequired extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        private final transient List<Integer> missing;

        ConfirmationRequired(List<Integer> missing) {
            super("faltan jueces " + missing + "; escribe " + CONFIRM_WORD + " para cerrar");
            this.missing = List.copyOf(missing);
        }

        public List<Integer> missing() {
            return missing;
        }
    }

    private final Database db;
    private final Broadcaster broadcaster;
    private final LongSupplier clock;
    private final PeleaRepository peleas;
    private final RoundRepository rounds;
    private final LugarJuezRepository lugares;
    private final TarjetaRoundRepository tarjetas;
    private final EventLog log;

    public RoundService(Database db, Broadcaster broadcaster, LongSupplier clock) {
        this.db = db;
        this.broadcaster = broadcaster;
        this.clock = clock;
        this.peleas = new PeleaRepository(db);
        this.rounds = new RoundRepository(db);
        this.lugares = new LugarJuezRepository(db);
        this.tarjetas = new TarjetaRoundRepository(db);
        this.log = new EventLog(db);
    }

    /** Servicio conectado al servidor de jueces. */
    public static RoundService forHub(Database db, JudgeHub hub) {
        return new RoundService(db, new Broadcaster() {
            @Override
            public void roundOpened(String fightId, int round) {
                hub.roundOpened(fightId, round);
            }

            @Override
            public void roundClosed(String fightId, int round, boolean forced) {
                hub.roundClosed(fightId, round, forced);
            }
        }, System::currentTimeMillis);
    }

    /**
     * Abre el round {@code n}: uno a la vez y en orden. El primero pasa la pelea a
     * {@code EN_CURSO}.
     */
    public synchronized void openRound(String fightId, int n) {
        var pelea = pelea(fightId);
        if (pelea.estado() != EstadoPelea.CREADA && pelea.estado() != EstadoPelea.EN_CURSO) {
            throw new IllegalStateException("la pelea no está activa");
        }
        if (n < 1 || n > pelea.rounds()) {
            throw new IllegalArgumentException("round fuera de rango: " + n);
        }
        var all = rounds.byPelea(fightId);
        if (all.stream().anyMatch(r -> r.estado() == EstadoRound.ABIERTO)) {
            throw new IllegalStateException("ya hay un round abierto");
        }
        if (all.stream().anyMatch(r -> r.numero() == n && r.estado() == EstadoRound.CERRADO)) {
            throw new IllegalStateException("el round " + n + " ya se cerró");
        }
        if (n > 1 && all.stream().noneMatch(r -> r.numero() == n - 1 && r.estado() == EstadoRound.CERRADO)) {
            throw new IllegalStateException("primero hay que cerrar el round " + (n - 1));
        }
        db.inTransaction(c -> {
            if (pelea.estado() == EstadoPelea.CREADA) {
                peleas.updateEstado(fightId, EstadoPelea.EN_CURSO);
            }
            rounds.save(new Round(fightId, n, EstadoRound.ABIERTO, clock.getAsLong(), null, false));
            log.append(fightId, "ROUND_OPENED", "round " + n);
            return null;
        });
        broadcaster.roundOpened(fightId, n);
    }

    /**
     * Cierra el round abierto {@code n}. Con jueces faltantes exige {@code confirmation} =
     * {@value #CONFIRM_WORD} y lo registra como cierre forzado.
     *
     * @return los lugares que quedaron faltantes (vacío si todos enviaron)
     */
    public synchronized List<Integer> closeRound(String fightId, int n, String confirmation) {
        var round = rounds.find(fightId, n).orElse(null);
        if (round == null || round.estado() != EstadoRound.ABIERTO) {
            throw new IllegalStateException("el round " + n + " no está abierto");
        }
        var missing = board(fightId, n).missing();
        boolean forced = !missing.isEmpty();
        if (forced && (confirmation == null || !CONFIRM_WORD.equals(confirmation.strip()))) {
            throw new ConfirmationRequired(missing);
        }
        db.inTransaction(c -> {
            rounds.save(new Round(fightId, n, EstadoRound.CERRADO, round.abiertoEnMs(), clock.getAsLong(), forced));
            log.append(fightId, forced ? "ROUND_CLOSED_FORCED" : "ROUND_CLOSED",
                    "round " + n + (forced ? " faltantes " + missing : ""));
            return null;
        });
        broadcaster.roundClosed(fightId, n, forced);
        return missing;
    }

    /** Tablero del round {@code n}: estado de cada juez, sin valores. */
    public synchronized Board board(String fightId, int n) {
        var pelea = pelea(fightId);
        var estado = rounds.find(fightId, n).map(Round::estado).orElse(EstadoRound.PENDIENTE);
        Map<Integer, TarjetaRound> cards = tarjetas.byPelea(fightId).stream()
                .filter(t -> t.round() == n)
                .collect(Collectors.toMap(TarjetaRound::juez, Function.identity()));
        Map<Integer, String> names = lugares.byPelea(fightId).stream()
                .filter(l -> l.nombre() != null)
                .collect(Collectors.toMap(LugarJuez::numero, LugarJuez::nombre));
        var slots = new ArrayList<Slot>();
        for (int seat = 1; seat <= pelea.jueces(); seat++) {
            var card = cards.get(seat);
            slots.add(new Slot(seat, names.get(seat), card == null ? Casilla.PENDIENTE : casilla(card.estado())));
        }
        return new Board(n, estado, List.copyOf(slots));
    }

    /** El round abierto, o el siguiente por abrir (0 si ya se jugaron todos). */
    public synchronized int currentRound(String fightId) {
        var pelea = pelea(fightId);
        var all = rounds.byPelea(fightId);
        var open = all.stream().filter(r -> r.estado() == EstadoRound.ABIERTO).findFirst();
        if (open.isPresent()) {
            return open.get().numero();
        }
        int lastClosed = all.stream().filter(r -> r.estado() == EstadoRound.CERRADO)
                .mapToInt(Round::numero).max().orElse(0);
        return lastClosed < pelea.rounds() ? lastClosed + 1 : 0;
    }

    /**
     * Totales parciales por juez de los rounds cerrados. Solo con {@code confirmed}; cada consulta
     * queda en la bitácora.
     */
    public synchronized List<PartialTotal> partialSummary(String fightId, boolean confirmed) {
        if (!confirmed) {
            throw new IllegalStateException("el resumen parcial requiere confirmación");
        }
        var pelea = pelea(fightId);
        var closed = rounds.byPelea(fightId).stream()
                .filter(r -> r.estado() == EstadoRound.CERRADO)
                .map(Round::numero)
                .collect(Collectors.toSet());
        Map<Integer, String> names = lugares.byPelea(fightId).stream()
                .filter(l -> l.nombre() != null)
                .collect(Collectors.toMap(LugarJuez::numero, LugarJuez::nombre));
        var out = new ArrayList<PartialTotal>();
        for (int seat = 1; seat <= pelea.jueces(); seat++) {
            int s = seat;
            var own = tarjetas.byPelea(fightId).stream()
                    .filter(t -> t.juez() == s && closed.contains(t.round()))
                    .toList();
            out.add(new PartialTotal(seat, names.get(seat),
                    own.stream().mapToInt(TarjetaRound::rojo).sum(),
                    own.stream().mapToInt(TarjetaRound::azul).sum()));
        }
        log.append(fightId, "PARTIAL_SUMMARY_VIEWED", "rounds cerrados " + closed.size());
        return List.copyOf(out);
    }

    private Pelea pelea(String fightId) {
        return peleas.find(fightId).orElseThrow(() -> new IllegalArgumentException("pelea desconocida"));
    }

    private static Casilla casilla(EstadoTarjeta estado) {
        return switch (estado) {
            case ENVIADA -> Casilla.ENVIADO;
            case TARDIA -> Casilla.TARDIO;
            case CORREGIDA, CAPTURADA -> Casilla.CORREGIDO;
        };
    }
}
