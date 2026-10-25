package com.sigebox.central.service;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.db.TarjetaRoundRepository;
import com.sigebox.central.server.JudgeHub;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.Ganador;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaRound;
import com.sigebox.core.scoring.DecisionRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

/**
 * Control de rounds de la pelea activa. Persiste cada cambio y solo después lo difunde a los
 * jueces. Los puntos y "Va ganando" ({@link #live}) son solo para la pantalla del operador: nada
 * de eso se difunde a los teléfonos.
 */
public final class RoundService {

    /** Palabra que el organizador escribe para cerrar un round con jueces faltantes. */
    public static final String CONFIRM_WORD = "AVANZAR";

    /** Tipo en la bitácora la primera vez que el operador ve el tablero en vivo de una pelea. */
    public static final String BOARD_VIEWED = "LIVE_BOARD_VIEWED";

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

    /** Juez de la pelea en el tablero en vivo. */
    public record Judge(int seat, String name, boolean connected) {}

    /**
     * Celda juez × round. Round cerrado: sus puntos (o {@code null} si no envió). Round abierto: su
     * casilla, y los puntos en cuanto envía. Round futuro: todo {@code null}.
     */
    public record Cell(Casilla casilla, Integer red, Integer blue) {
        static final Cell EMPTY = new Cell(null, null, null);

        public boolean hasScore() {
            return red != null && blue != null;
        }
    }

    public record Row(int round, EstadoRound estado, List<Cell> cells) {}

    /** Suma de un juez en los rounds cerrados. */
    public record Total(int seat, int red, int blue) {}

    /**
     * "Va ganando" con {@link DecisionRules#fallo} sobre los totales de los rounds cerrados;
     * {@code aFavor} = jueces con la esquina líder (0 en empate).
     */
    public record Leader(Ganador ganador, int aFavor, int jueces) {}

    /**
     * Tablero en vivo del operador: juez × round, totales de los rounds cerrados y líder
     * ({@code null} si no hay rounds cerrados). {@code current} como {@link #currentRound}.
     */
    public record Live(int rounds, int current, List<Judge> judges, List<Row> rows, List<Total> totals,
            Leader leader) {

        public Row row(int round) {
            return rows.get(round - 1);
        }

        /** Jueces que ya enviaron en el round {@code round}. */
        public long sent(int round) {
            return row(round).cells().stream().filter(c -> c.casilla() != null && c.casilla() != Casilla.PENDIENTE)
                    .count();
        }
    }

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
     * Tablero en vivo de la pelea. La primera consulta por pelea queda en la bitácora
     * ({@value #BOARD_VIEWED}); las siguientes (el refresco de la pantalla) no.
     */
    public synchronized Live live(String fightId) {
        var pelea = pelea(fightId);
        Map<Integer, EstadoRound> estados = rounds.byPelea(fightId).stream()
                .collect(Collectors.toMap(Round::numero, Round::estado));
        Map<Integer, LugarJuez> seats = lugares.byPelea(fightId).stream()
                .collect(Collectors.toMap(LugarJuez::numero, Function.identity()));
        Map<String, TarjetaRound> cards = tarjetas.byPelea(fightId).stream()
                .collect(Collectors.toMap(t -> t.juez() + ":" + t.round(), Function.identity()));

        var judges = new ArrayList<Judge>();
        for (int seat = 1; seat <= pelea.jueces(); seat++) {
            var l = seats.get(seat);
            judges.add(new Judge(seat, l == null ? null : l.nombre(),
                    l != null && l.conexion() == EstadoConexion.CONECTADO));
        }
        var rows = new ArrayList<Row>();
        int[] red = new int[pelea.jueces() + 1];
        int[] blue = new int[pelea.jueces() + 1];
        boolean anyClosed = false;
        for (int n = 1; n <= pelea.rounds(); n++) {
            var estado = estados.getOrDefault(n, EstadoRound.PENDIENTE);
            anyClosed |= estado == EstadoRound.CERRADO;
            var cells = new ArrayList<Cell>();
            for (int seat = 1; seat <= pelea.jueces(); seat++) {
                var card = cards.get(seat + ":" + n);
                if (estado == EstadoRound.PENDIENTE) {
                    cells.add(Cell.EMPTY);
                } else if (card == null) {
                    cells.add(new Cell(estado == EstadoRound.ABIERTO ? Casilla.PENDIENTE : null, null, null));
                } else {
                    cells.add(new Cell(casilla(card.estado()), card.rojo(), card.azul()));
                    if (estado == EstadoRound.CERRADO) {
                        red[seat] += card.rojo();
                        blue[seat] += card.azul();
                    }
                }
            }
            rows.add(new Row(n, estado, List.copyOf(cells)));
        }
        var totals = new ArrayList<Total>();
        int porRoja = 0;
        int porAzul = 0;
        for (int seat = 1; seat <= pelea.jueces(); seat++) {
            totals.add(new Total(seat, red[seat], blue[seat]));
            porRoja += red[seat] > blue[seat] ? 1 : 0;
            porAzul += blue[seat] > red[seat] ? 1 : 0;
        }
        Leader leader = null;
        if (anyClosed) {
            var fallo = DecisionRules.fallo(porRoja, porAzul, pelea.jueces());
            int aFavor = switch (fallo.ganador()) {
                case ROJA -> porRoja;
                case AZUL -> porAzul;
                default -> 0;
            };
            leader = new Leader(fallo.ganador(), aFavor, pelea.jueces());
        }
        if (log.byPelea(fightId).stream().noneMatch(e -> BOARD_VIEWED.equals(e.tipo()))) {
            log.append(fightId, BOARD_VIEWED, "tablero en vivo del operador");
        }
        return new Live(pelea.rounds(), currentRound(fightId), List.copyOf(judges), List.copyOf(rows),
                List.copyOf(totals), leader);
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
