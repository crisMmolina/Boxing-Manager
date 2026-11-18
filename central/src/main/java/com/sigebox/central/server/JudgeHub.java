package com.sigebox.central.server;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.FuncionRepository;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.ProcessedMessages;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.db.TarjetaFinalRepository;
import com.sigebox.central.db.TarjetaRoundRepository;
import com.sigebox.central.db.TerminacionRepository;
import com.sigebox.central.db.VersionTarjetaRepository;
import com.sigebox.central.service.FightService;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaFinal;
import com.sigebox.core.model.TarjetaRound;
import com.sigebox.core.model.VersionTarjeta;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Challenge;
import com.sigebox.core.protocol.DeviceProof;
import com.sigebox.core.protocol.CodeInfo;
import com.sigebox.core.protocol.CodeLookup;
import com.sigebox.core.protocol.CodeUnknown;
import com.sigebox.core.protocol.EventEnded;
import com.sigebox.core.protocol.FightClosed;
import com.sigebox.core.protocol.FightEnded;
import com.sigebox.core.protocol.FinalCardHash;
import com.sigebox.core.protocol.Hello;
import com.sigebox.core.protocol.Join;
import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.JoinPending;
import com.sigebox.core.protocol.JoinRejected;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.Messages;
import com.sigebox.core.protocol.NextFight;
import com.sigebox.core.protocol.Ping;
import com.sigebox.core.protocol.Pong;
import com.sigebox.core.protocol.ProtocolException;
import com.sigebox.core.protocol.RoundClosed;
import com.sigebox.core.protocol.RoundOpened;
import com.sigebox.core.protocol.State;
import com.sigebox.core.protocol.SubmitFinalCard;
import com.sigebox.core.protocol.SubmitRound;
import com.sigebox.core.validation.ScoreValidator;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Lógica del protocolo de jueces (core/schemas/README.md), sin depender del transporte.
 *
 * <ul>
 *   <li>Cada conexión recibe un {@code CHALLENGE} y debe autenticarse con un {@code JOIN} o
 *       {@code HELLO} firmado sobre ese nonce antes de que venza {@code authTimeout}.
 *   <li>{@code fightId} y lugar salen siempre de la sesión, nunca del mensaje.
 *   <li>Antes del {@code JOIN}, {@code CODE_LOOKUP} dice a qué lugar libre pertenece un código (ADR 0004): a
 *       lo más una búsqueda por segundo y la conexión se cierra al {@value #MAX_UNKNOWN_CODES}.º
 *       {@code CODE_UNKNOWN}.
 *   <li>El {@code ACK} sale después de persistir; un repetido (lugar, {@code id}) recibe el mismo.
 *   <li>Solo difunde: quien cambia el estado de rounds o pelea lo persiste antes de llamar a
 *       {@link #roundOpened}, {@link #roundClosed}, {@link #fightEnded} o {@link #fightClosed}.
 * </ul>
 *
 * <p>Todos los métodos públicos son {@code synchronized}: el tráfico es de 1–5 jueces.
 */
public final class JudgeHub implements AutoCloseable, FightService.Puerta {

    /** Mismo límite que {@code Messages.decode}. */
    public static final int MAX_MESSAGE_CHARS = 16 * 1024;

    /** Cuánto se espera, tras avisar {@code NEXT_FIGHT}, a que el teléfono entregue su bandeja y cierre. */
    static final Duration HANDOFF_GRACE = Duration.ofSeconds(15);
    public static final Duration DEFAULT_AUTH_TIMEOUT = Duration.ofSeconds(10);

    /** Una búsqueda de código por segundo y conexión; la que llega antes no se consulta y cuenta como desconocida. */
    static final long LOOKUP_INTERVAL_MS = 1_000;
    /** {@code CODE_UNKNOWN} por conexión antes de cerrarla. */
    public static final int MAX_UNKNOWN_CODES = 5;
    /**
     * Búsquedas que se consultan por segundo en toda la central: abrir muchas conexiones no multiplica los intentos
     * (revisión de seguridad de DSK-033). Las que sobran no se consultan y cuentan como desconocidas.
     */
    static final int GLOBAL_LOOKUPS_PER_SECOND = 5;
    /**
     * Conexiones abiertas sin {@code JOIN}/{@code HELLO} válido. Al llegar otra se cierra la más vieja sin autenticar:
     * quien acapara conexiones no deja fuera el {@code HELLO} de un juez, que se autentica en cuanto abre.
     */
    static final int MAX_UNAUTHENTICATED = 16;
    /** El bloqueo por códigos se anota a lo más una vez por minuto, para no inundar la bitácora. */
    static final long LIMIT_LOG_INTERVAL_MS = 60_000;

    private static final String APPROVED_BY = "central";

    private static final class Session {
        final Conn conn;
        final String nonce;
        boolean nonceUsed;
        boolean authenticated;
        boolean approved;
        /** Conexión a una pelea cerrada solo para entregar lo pendiente antes de pasar a la siguiente. */
        boolean handoff;
        int lookups;
        long lastLookupMs;
        int unknownCodes;
        String fightId;
        int seat;

        Session(Conn conn, String nonce) {
            this.conn = conn;
            this.nonce = nonce;
        }
    }

    private record SeatKey(String fightId, int seat) {}

    /**
     * Otro teléfono que pidió quedarse con un lugar ya aprobado usando su código de vuelta (DSK-055). Espera en
     * memoria, sin tocar el registro del lugar, hasta que el operador lo cambie o lo rechace.
     */
    private record Candidate(Session session, String judgeName, String deviceId, String publicKey,
            String fingerprint) {}

    private final Map<SeatKey, Candidate> candidates = new HashMap<>();

    private final Database db;
    private final SeatTokens tokens;
    private final ServerEvents events;
    private final Duration authTimeout;
    private final LongSupplier clock;
    private final SecureRandom random = new SecureRandom();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        var t = new Thread(r, "judge-hub-timer");
        t.setDaemon(true);
        return t;
    });

    private final PeleaRepository peleas;
    private final LugarJuezRepository lugares;
    private final FuncionRepository funciones;
    private final RoundRepository rounds;
    private final TarjetaRoundRepository tarjetas;
    private final VersionTarjetaRepository versiones;
    private final TarjetaFinalRepository finales;
    private final TerminacionRepository terminaciones;
    private final ProcessedMessages processed;
    private final EventLog log;

    private final Map<String, Session> sessions = new HashMap<>();
    /** Ids de las sesiones sin autenticar, de la más vieja a la más nueva. */
    private final Set<String> unauthenticated = new LinkedHashSet<>();
    /** Momentos de las búsquedas consultadas en el último segundo (toda la central). */
    private final ArrayDeque<Long> recentLookups = new ArrayDeque<>();
    private long lastLimitLogMs;
    private boolean limitLogged;
    private final Map<SeatKey, Session> holders = new HashMap<>();

    public JudgeHub(Database db, SeatTokens tokens, ServerEvents events) {
        this(db, tokens, events, DEFAULT_AUTH_TIMEOUT, System::currentTimeMillis);
    }

    public JudgeHub(Database db, SeatTokens tokens, ServerEvents events, Duration authTimeout, LongSupplier clock) {
        this.db = db;
        this.tokens = tokens;
        this.events = events == null ? ServerEvents.NONE : events;
        this.authTimeout = authTimeout;
        this.clock = clock;
        this.peleas = new PeleaRepository(db);
        this.lugares = new LugarJuezRepository(db);
        this.funciones = new FuncionRepository(db);
        this.rounds = new RoundRepository(db);
        this.tarjetas = new TarjetaRoundRepository(db);
        this.versiones = new VersionTarjetaRepository(db);
        this.finales = new TarjetaFinalRepository(db);
        this.terminaciones = new TerminacionRepository(db);
        this.processed = new ProcessedMessages(db);
        this.log = new EventLog(db);
    }

    // ---- transporte ----

    public synchronized void onOpen(Conn conn) {
        if (unauthenticated.size() >= MAX_UNAUTHENTICATED) {
            var oldest = sessions.get(unauthenticated.iterator().next());
            if (oldest != null) {
                drop(oldest, Conn.POLICY, "busy");
            }
        }
        var s = new Session(conn, DeviceProof.newNonce(random));
        sessions.put(conn.id(), s);
        unauthenticated.add(conn.id());
        conn.send(Messages.encode(new Challenge(s.nonce)));
        timer.schedule(() -> expireIfUnauthenticated(conn.id()), authTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public synchronized void onMessage(String connId, String text) {
        var s = sessions.get(connId);
        if (s == null) {
            return;
        }
        if (text == null || text.length() > MAX_MESSAGE_CHARS) {
            drop(s, Conn.TOO_BIG, "too big");
            return;
        }
        Message msg;
        try {
            msg = Messages.decode(text);
        } catch (ProtocolException e) {
            if (!s.authenticated) {
                drop(s, Conn.POLICY, "malformed");
            } else {
                logSeat(s, "MALFORMED", null); // nunca el contenido recibido
            }
            return;
        }
        switch (msg) {
            case Join m -> onJoin(s, m);
            case Hello m -> onHello(s, m);
            case CodeLookup m -> onCodeLookup(s, m);
            case Ping m -> {
                if (requireAuth(s)) {
                    send(s, new Pong(m.ts()));
                }
            }
            case Pong m -> requireAuth(s);
            case NextFight m -> {
                // NEXT_FIGHT solo va de la central al juez (ADR 0002).
                if (requireAuth(s)) {
                    logSeat(s, "NEXT_FIGHT_REJECTED", null);
                    send(s, new Ack(m.id(), false, null, Ack.Error.MALFORMED));
                }
            }
            case SubmitRound m -> {
                if (requireAuth(s)) {
                    onSubmitRound(s, m);
                }
            }
            case SubmitFinalCard m -> {
                if (requireAuth(s)) {
                    onSubmitFinalCard(s, m);
                }
            }
            default -> {
                // Mensajes de control (ROUND_OPENED, STATE, ACK…) solo los emite la central.
                if (requireAuth(s)) {
                    logSeat(s, "CONTROL_REJECTED", msg.getClass().getSimpleName());
                    send(s, new Ack(msg.id(), false, null, Ack.Error.MALFORMED));
                }
            }
        }
    }

    public synchronized void onClose(String connId) {
        var s = sessions.remove(connId);
        unauthenticated.remove(connId);
        if (s == null || s.fightId == null) {
            return;
        }
        var key = new SeatKey(s.fightId, s.seat);
        var candidate = candidates.get(key);
        if (candidate != null && candidate.session() == s) {
            candidates.remove(key);
        }
        if (holders.get(key) == s) {
            holders.remove(key);
            if (s.approved) {
                setConexion(s.fightId, s.seat, EstadoConexion.DESCONECTADO);
                log.append(s.fightId, "JUDGE_DISCONNECTED", "lugar " + s.seat);
                events.onJudgeDisconnected(s.fightId, s.seat);
            }
        }
    }

    // ---- API para la UI y los servicios ----

    /** Emite el token de un solo uso del lugar (va en el QR). */
    public String issueToken(String fightId, int seat) {
        return tokens.issue(fightId, seat);
    }

    /** Aprueba un lugar pendiente. {@code false} si no hay solicitud pendiente. */
    public synchronized boolean approve(String fightId, int seat) {
        var lugar = lugares.find(fightId, seat).orElse(null);
        var pelea = peleas.find(fightId).orElse(null);
        if (lugar == null || pelea == null || lugar.aprobadoEnMs() != null
                || lugar.conexion() == EstadoConexion.RECHAZADO) {
            return false;
        }
        var holder = holders.get(new SeatKey(fightId, seat));
        var estado = holder != null ? EstadoConexion.CONECTADO : EstadoConexion.DESCONECTADO;
        lugares.save(new LugarJuez(fightId, seat, lugar.nombre(), lugar.dispositivo(), lugar.huellaCredencial(),
                estado, APPROVED_BY, clock.getAsLong()));
        log.append(fightId, "JOIN_APPROVED", "lugar " + seat);
        if (holder != null) {
            holder.approved = true;
            send(holder, state(holder)); // sin esto el teléfono se queda en el esqueleto hasta reconectar
            send(holder, approvedMessage(pelea, seat, lugar.nombre()));
            events.onJudgeConnected(fightId, seat);
        }
        return true;
    }

    /** Huella corta del teléfono que pide quedarse con un lugar ya aprobado (DSK-055), si hay. */
    public synchronized Optional<String> replacementFingerprint(String fightId, int seat) {
        return Optional.ofNullable(candidates.get(new SeatKey(fightId, seat)))
                .map(c -> FightService.shortFingerprint(c.fingerprint()));
    }

    /** Aprueba el cambio de teléfono del lugar. {@code false} si no hay cambio pendiente. */
    public synchronized boolean approveReplacement(String fightId, int seat) {
        var key = new SeatKey(fightId, seat);
        var candidate = candidates.get(key);
        var lugar = lugares.find(fightId, seat).orElse(null);
        var pelea = peleas.find(fightId).orElse(null);
        if (candidate == null || lugar == null || pelea == null || lugar.aprobadoEnMs() == null
                || holders.containsKey(key)) {
            return false;
        }
        candidates.remove(key);
        var before = FightService.shortFingerprint(lugar.huellaCredencial());
        var after = FightService.shortFingerprint(candidate.fingerprint());
        var cambiado = new LugarJuez(fightId, seat, lugar.nombre(), candidate.deviceId(), candidate.fingerprint(),
                EstadoConexion.CONECTADO, APPROVED_BY, clock.getAsLong());
        db.inTransaction(c -> {
            // La llave anterior deja de servir: su HELLO ya no coincide. Las tarjetas recibidas no cambian.
            lugares.save(cambiado);
            lugares.savePublicKey(fightId, seat, candidate.publicKey());
            log.append(fightId, "JOIN_APPROVED", "lugar " + seat + " · teléfono cambiado: " + before + " → " + after);
            return null;
        });
        var s = candidate.session();
        bind(s, fightId, seat);
        s.approved = true;
        send(s, state(s));
        send(s, approvedMessage(pelea, seat, lugar.nombre()));
        events.onJudgeConnected(fightId, seat);
        return true;
    }

    /** Rechaza el cambio de teléfono; el lugar sigue con el teléfono anterior. */
    public synchronized boolean rejectReplacement(String fightId, int seat) {
        var candidate = candidates.remove(new SeatKey(fightId, seat));
        if (candidate == null) {
            return false;
        }
        log.append(fightId, "JOIN_REJECTED", "lugar " + seat + " · cambio de teléfono");
        send(candidate.session(), new JoinRejected(fightId, seat, JoinRejected.Reason.REJECTED));
        drop(candidate.session(), 1000, "rejected");
        return true;
    }

    /** Rechaza un lugar pendiente y cierra su conexión. */
    public synchronized boolean reject(String fightId, int seat) {
        var lugar = lugares.find(fightId, seat).orElse(null);
        if (lugar == null || lugar.aprobadoEnMs() != null) {
            return false;
        }
        lugares.save(new LugarJuez(fightId, seat, lugar.nombre(), lugar.dispositivo(), lugar.huellaCredencial(),
                EstadoConexion.RECHAZADO, null, null));
        log.append(fightId, "JOIN_REJECTED", "lugar " + seat);
        var holder = holders.remove(new SeatKey(fightId, seat));
        if (holder != null) {
            send(holder, new JoinRejected(fightId, seat, JoinRejected.Reason.REJECTED));
            drop(holder, 1000, "rejected");
        }
        return true;
    }

    public synchronized void roundOpened(String fightId, int round) {
        for (var s : approvedSessions(fightId)) {
            send(s, new RoundOpened(fightId, round));
        }
    }

    /** Difunde el cierre; {@code mustSubmit} es verdadero para cada juez sin tarjeta en ese round. */
    public synchronized void roundClosed(String fightId, int round, boolean forced) {
        for (var s : approvedSessions(fightId)) {
            boolean missing = tarjetas.find(fightId, s.seat, round).isEmpty();
            send(s, new RoundClosed(fightId, round, forced, missing));
        }
    }

    public synchronized void fightEnded(String fightId, MotivoTerminacion reason) {
        for (var s : approvedSessions(fightId)) {
            send(s, new FightEnded(fightId, reason));
        }
    }

    /**
     * Difunde {@code FIGHT_CLOSED}, descarta los códigos sin usar de la pelea y, si era la última del evento (no
     * queda ninguna sin cerrar ni saltar en su función), manda además {@code EVENT_ENDED}.
     */
    public synchronized void fightClosed(String fightId) {
        tokens.forget(fightId);
        boolean lastOfEvent = lastOfEvent(fightId);
        for (var s : approvedSessions(fightId)) {
            send(s, new FightClosed(fightId));
            if (lastOfEvent) {
                send(s, new EventEnded(fightId));
            }
        }
    }

    /**
     * Avisa a cada teléfono aprobado de {@code fightId} que la función sigue con {@code nextFightId}:
     * su lugar en la nueva pelea, o {@code null} si no participa. Los lugares ya se copiaron y
     * persistieron ({@code FuncionService.siguiente}) antes de llamar a este método.
     */
    public synchronized void nextFight(String fightId, String nextFightId) {
        var next = peleas.find(nextFightId).orElse(null);
        if (next == null) {
            return;
        }
        for (var s : approvedSessions(fightId)) {
            var seat = lugares.find(nextFightId, s.seat).filter(l -> l.aprobadoEnMs() != null)
                    .map(LugarJuez::numero).orElse(null);
            send(s, new NextFight(fightId, nextFightId, seat, next.esquinaRoja(), next.esquinaAzul(), next.rounds()));
            logSeat(s, "NEXT_FIGHT_SENT", seat == null ? "sin lugar" : "lugar " + seat);
        }
    }

    /**
     * Corre {@code trabajo} con el candado del hub (spec 005, DSK-040): mientras se revisan y quitan lugares no se
     * atiende ningún {@code JOIN}, {@code HELLO} ni {@code CODE_LOOKUP}. Orden de candados: hub → base de datos.
     */
    @Override
    public synchronized <T> T exclusivo(Supplier<T> trabajo) {
        return trabajo.get();
    }

    /**
     * La pelea {@code fightId} quedó con {@code jueces} lugares: se cierra cualquier conexión ligada a un lugar que
     * sobra (no debería haber: solo se quitan lugares sin juez) y, si cambiaron los rounds, cada juez aprobado recibe
     * un {@code STATE} nuevo (el teléfono usa {@code rounds} para pedir la tarjeta final).
     */
    @Override
    public synchronized void peleaCambiada(String fightId, int jueces, boolean roundsCambiaron) {
        for (var s : new ArrayList<>(holders.values())) {
            if (s.fightId.equals(fightId) && s.seat > jueces) {
                drop(s, Conn.POLICY, "seat removed");
            }
        }
        if (roundsCambiaron) {
            for (var s : approvedSessions(fightId)) {
                try { // el cambio ya se guardó: un teléfono que falla no deja sin STATE a los demás
                    send(s, state(s));
                } catch (RuntimeException e) {
                    logSeat(s, "CONTROL_REJECTED", "STATE tras cambiar rounds");
                }
            }
        }
    }

    /** Lugares con conexión abierta en este momento. */
    public synchronized List<Integer> connectedSeats(String fightId) {
        return holders.keySet().stream()
                .filter(k -> k.fightId().equals(fightId))
                .map(SeatKey::seat)
                .sorted()
                .toList();
    }

    @Override
    public void close() {
        timer.shutdownNow();
    }

    // ---- autenticación ----

    private void onCodeLookup(Session s, CodeLookup m) {
        if (s.authenticated) {
            // Solo antes del JOIN: un juez ya unido no busca lugares.
            logSeat(s, "CONTROL_REJECTED", "CodeLookup");
            send(s, new Ack(m.id(), false, null, Ack.Error.MALFORMED));
            return;
        }
        long now = clock.getAsLong();
        boolean tooSoon = s.lookups > 0 && now - s.lastLookupMs < LOOKUP_INTERVAL_MS;
        s.lookups++;
        s.lastLookupMs = now;
        boolean overBudget = !tooSoon && !takeGlobalLookup(now);
        var info = tooSoon || overBudget ? null : codeInfo(m.code());
        if (info != null) {
            send(s, info);
            return;
        }
        // No dice si no existe, ya se usó o es de una pelea cerrada. Sin cupo global no se consultó nada, así que no
        // cuenta para cerrar: un atacante que agota el cupo no hace que la central eche a los jueces legítimos.
        send(s, new CodeUnknown(m.code()));
        if (!overBudget && ++s.unknownCodes >= MAX_UNKNOWN_CODES) {
            if (!limitLogged || now - lastLimitLogMs >= LIMIT_LOG_INTERVAL_MS) {
                limitLogged = true;
                lastLimitLogMs = now;
                // Tipo con etiqueta ya existente; del código solo los 2 últimos dígitos.
                log.append(null, "JOIN_REJECTED", "búsqueda de código bloqueada tras " + MAX_UNKNOWN_CODES
                        + " intentos · …" + m.code().substring(m.code().length() - 2));
            }
            drop(s, Conn.POLICY, "codes");
        }
    }

    private boolean takeGlobalLookup(long now) {
        while (!recentLookups.isEmpty() && now - recentLookups.peekFirst() >= LOOKUP_INTERVAL_MS) {
            recentLookups.pollFirst();
        }
        if (recentLookups.size() >= GLOBAL_LOOKUPS_PER_SECOND) {
            return false;
        }
        recentLookups.addLast(now);
        return true;
    }

    /** {@code CODE_INFO} si el código es de un lugar libre y sin registrar de una pelea abierta. */
    private CodeInfo codeInfo(String code) {
        var seat = tokens.lookup(code).orElse(null);
        if (seat == null) {
            return null;
        }
        var pelea = peleas.find(seat.fightId()).orElse(null);
        if (pelea == null || pelea.estado() == EstadoPelea.CERRADA || seat.seat() > pelea.jueces()) {
            return null;
        }
        var lugar = lugares.find(seat.fightId(), seat.seat()).orElse(null);
        if (lugar != null && lugar.conexion() != EstadoConexion.RECHAZADO) {
            // Lugar registrado: solo con su código de vuelta, aprobado, sin conexión y sin otro cambio pendiente.
            var key = new SeatKey(seat.fightId(), seat.seat());
            if (lugar.aprobadoEnMs() == null || !tokens.rejoinOpen(seat.fightId(), seat.seat())
                    || holders.containsKey(key) || candidates.containsKey(key)) {
                return null;
            }
        }
        return new CodeInfo(code, pelea.id(), seat.seat(), pelea.esquinaRoja(), pelea.esquinaAzul());
    }

    /** Sin función, la pelea es todo el evento; con función, no queda otra sin cerrar ni saltar. */
    private boolean lastOfEvent(String fightId) {
        var funcionId = funcionOf(fightId);
        if (funcionId == null) {
            return true;
        }
        for (var row : funciones.card(funcionId)) {
            if (row.saltada() || fightId.equals(row.peleaId())) {
                continue;
            }
            if (row.peleaId() == null || peleas.find(row.peleaId()).map(p -> p.estado() != EstadoPelea.CERRADA)
                    .orElse(false)) {
                return false;
            }
        }
        return true;
    }

    private String funcionOf(String fightId) {
        return db.inTransaction(c -> {
            try (var ps = c.prepareStatement("SELECT funcion_id FROM funcion_card WHERE fight_id = ?")) {
                ps.setString(1, fightId);
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        });
    }

    private void onJoin(Session s, Join m) {
        if (s.authenticated || !consumeNonce(s, m.nonce())) {
            drop(s, Conn.POLICY, "nonce");
            return;
        }
        String fightId = m.fightId();
        int seat = m.seat();
        var pelea = peleas.find(fightId).orElse(null);
        if (pelea == null || pelea.estado() == EstadoPelea.CERRADA || seat > pelea.jueces()) {
            rejectAndDrop(s, fightId, seat, JoinRejected.Reason.TOKEN_INVALID);
            return;
        }
        var text = DeviceProof.joinText(fightId, seat, m.token(), m.deviceId(), s.nonce);
        if (!verify(m.publicKey(), text, m.sig())) {
            rejectAndDrop(s, fightId, seat, JoinRejected.Reason.BAD_SIGNATURE);
            return;
        }
        var lugar = lugares.find(fightId, seat).orElse(null);
        var storedKey = lugares.publicKey(fightId, seat).orElse(null);
        boolean sameDevice = storedKey != null && storedKey.equals(m.publicKey())
                && lugar != null && Objects.equals(lugar.dispositivo(), m.deviceId());
        // Un lugar que el organizador rechazó queda libre para otro teléfono con una invitación nueva: se consume el
        // token como en un lugar vacío y se reemplaza la llave. El teléfono rechazado sigue rechazado.
        boolean reopened = lugar != null && lugar.conexion() == EstadoConexion.RECHAZADO && !sameDevice;
        if (lugar != null && storedKey != null && !reopened) {
            // Lugar ya registrado: nunca se reemplaza la llave.
            if (!sameDevice) {
                var key = new SeatKey(fightId, seat);
                boolean replaceable = lugar.aprobadoEnMs() != null && lugar.conexion() != EstadoConexion.RECHAZADO
                        && !holders.containsKey(key) && !candidates.containsKey(key);
                if (replaceable && tokens.consumeRejoin(fightId, seat, m.token())) {
                    // Otro teléfono con el código de vuelta: espera a que el operador lo cambie; la llave no cambia.
                    s.authenticated = true;
                    unauthenticated.remove(s.conn.id());
                    s.fightId = fightId;
                    s.seat = seat;
                    candidates.put(key, new Candidate(s, m.judgeName(), m.deviceId(), m.publicKey(),
                            DeviceProof.fingerprint(m.publicKey())));
                    log.append(fightId, "JOIN_REQUESTED", "lugar " + seat + " · cambio de teléfono");
                    send(s, new JoinPending(fightId, seat));
                } else {
                    rejectAndDrop(s, fightId, seat, JoinRejected.Reason.SEAT_TAKEN);
                }
            } else if (lugar.conexion() == EstadoConexion.RECHAZADO) {
                rejectAndDrop(s, fightId, seat, JoinRejected.Reason.REJECTED);
            } else if (lugar.aprobadoEnMs() != null) {
                bind(s, fightId, seat);
                markApprovedAndConnected(s, lugar);
                send(s, state(s)); // la tarjeta final automática necesita pelea y rounds
                send(s, approvedMessage(pelea, seat, lugar.nombre()));
            } else {
                bind(s, fightId, seat); // el teléfono reenvía el JOIN tras reconectar; no se consulta el token
                send(s, new JoinPending(fightId, seat));
            }
            return;
        }
        switch (tokens.consume(fightId, seat, m.token())) {
            case INVALID -> rejectAndDrop(s, fightId, seat, JoinRejected.Reason.TOKEN_INVALID);
            case USED -> rejectAndDrop(s, fightId, seat, JoinRejected.Reason.TOKEN_USED);
            case OK -> {
                var fingerprint = DeviceProof.fingerprint(m.publicKey());
                db.inTransaction(c -> {
                    lugares.save(new LugarJuez(fightId, seat, m.judgeName(), m.deviceId(), fingerprint,
                            EstadoConexion.PENDIENTE, null, null));
                    lugares.savePublicKey(fightId, seat, m.publicKey());
                    log.append(fightId, "JOIN_REQUESTED", "lugar " + seat);
                    return null;
                });
                bind(s, fightId, seat);
                send(s, new JoinPending(fightId, seat));
                events.onJoinRequested(
                        new ServerEvents.JoinRequest(fightId, seat, m.judgeName(), m.deviceId(), fingerprint));
            }
        }
    }

    private void onHello(Session s, Hello m) {
        if (s.authenticated || !consumeNonce(s, m.nonce())) {
            drop(s, Conn.POLICY, "nonce");
            return;
        }
        var lugar = lugares.find(m.fightId(), m.seat()).orElse(null);
        var key = lugar == null ? null : lugares.publicKey(lugar.peleaId(), lugar.numero()).orElse(null);
        if (lugar == null || key == null || lugar.aprobadoEnMs() == null
                || lugar.conexion() == EstadoConexion.RECHAZADO || !Objects.equals(lugar.dispositivo(), m.deviceId())) {
            drop(s, Conn.POLICY, "hello");
            return;
        }
        // Texto firmado con los datos del registro aprobado y el nonce de esta conexión.
        var text = DeviceProof.helloText(lugar.peleaId(), lugar.numero(), lugar.dispositivo(), s.nonce);
        if (!verify(key, text, m.sig())) {
            drop(s, Conn.POLICY, "signature");
            return;
        }
        var next = nextFightFor(lugar, key);
        if (next != null) {
            // Pelea cerrada y la función ya sigue. El teléfono solo vacía su bandeja mientras está "sincronizado" (tras
            // un STATE) y difiere NEXT_FIGHT mientras le queden envíos sin ACK. Por eso va primero el STATE de la
            // pelea cerrada (sin totales ni ganador) y luego NEXT_FIGHT; la conexión sigue abierta, ligada a la pelea
            // cerrada y sin tomar el lugar, hasta que el teléfono cierre o venza HANDOFF_GRACE.
            s.authenticated = true;
            s.approved = true;
            s.handoff = true;
            s.fightId = lugar.peleaId();
            s.seat = lugar.numero();
            log.append(lugar.peleaId(), "NEXT_FIGHT_SENT", "lugar " + lugar.numero() + " al reconectar");
            send(s, state(s));
            send(s, next);
            timer.schedule(() -> expireHandoff(s.conn.id()), HANDOFF_GRACE.toMillis(), TimeUnit.MILLISECONDS);
            return;
        }
        bind(s, lugar.peleaId(), lugar.numero());
        markApprovedAndConnected(s, lugar);
        send(s, state(s));
    }

    /**
     * {@code NEXT_FIGHT} para un {@code HELLO} válido de una pelea cerrada, si la misma llave está en un
     * lugar aprobado de la pelea activa de la misma función; {@code null} si no aplica (se atiende como hoy).
     */
    private NextFight nextFightFor(LugarJuez lugar, String key) {
        var closed = peleas.find(lugar.peleaId()).orElse(null);
        if (closed == null || closed.estado() != EstadoPelea.CERRADA) {
            return null;
        }
        var funcionId = funcionOf(closed.id());
        if (funcionId == null) {
            return null;
        }
        for (var row : funciones.card(funcionId)) {
            var active = row.peleaId() == null || row.peleaId().equals(closed.id()) ? null
                    : peleas.find(row.peleaId()).orElse(null);
            if (active == null || active.estado() == EstadoPelea.CERRADA) {
                continue;
            }
            for (var candidate : lugares.byPelea(active.id())) {
                if (candidate.aprobadoEnMs() != null && candidate.conexion() != EstadoConexion.RECHAZADO
                        && key.equals(lugares.publicKey(active.id(), candidate.numero()).orElse(null))) {
                    return new NextFight(closed.id(), active.id(), candidate.numero(), active.esquinaRoja(),
                            active.esquinaAzul(), active.rounds());
                }
            }
        }
        return null;
    }

    private boolean consumeNonce(Session s, String nonce) {
        if (s.nonceUsed || !s.nonce.equals(nonce)) {
            return false;
        }
        s.nonceUsed = true;
        return true;
    }

    /** Antes de un JOIN/HELLO válido, cualquier otro mensaje cierra la conexión. */
    private boolean requireAuth(Session s) {
        if (!s.authenticated) {
            drop(s, Conn.POLICY, "unauthenticated");
            return false;
        }
        return true;
    }

    private void bind(Session s, String fightId, int seat) {
        s.authenticated = true;
        unauthenticated.remove(s.conn.id());
        s.fightId = fightId;
        s.seat = seat;
        var old = holders.put(new SeatKey(fightId, seat), s);
        if (old != null && old != s) {
            // Una conexión más nueva toma el lugar; la vieja se cierra sin marcar desconexión.
            sessions.remove(old.conn.id());
            old.conn.close(1000, "replaced");
        }
        // DSK-055: el teléfono ya está en su lugar; su código de vuelta y cualquier cambio pendiente sobran.
        tokens.forgetRejoin(fightId, seat);
        var candidate = candidates.get(new SeatKey(fightId, seat));
        if (candidate != null && candidate.session() != s) {
            candidates.remove(new SeatKey(fightId, seat));
            send(candidate.session(), new JoinRejected(fightId, seat, JoinRejected.Reason.SEAT_TAKEN));
            drop(candidate.session(), Conn.POLICY, "seat back");
        }
    }

    private void markApprovedAndConnected(Session s, LugarJuez lugar) {
        s.approved = true;
        setConexion(lugar.peleaId(), lugar.numero(), EstadoConexion.CONECTADO);
        log.append(lugar.peleaId(), "JUDGE_CONNECTED", "lugar " + lugar.numero());
        events.onJudgeConnected(lugar.peleaId(), lugar.numero());
    }

    // ---- tarjetas ----

    private void onSubmitRound(Session s, SubmitRound m) {
        if (!s.approved) {
            send(s, new Ack(m.id(), false, null, Ack.Error.NOT_APPROVED));
            return;
        }
        // Idempotencia primero: un reenvío tras perder el ACK recibe el mismo ACK aunque el round ya cerró.
        if (processed.seen(s.fightId, s.seat, m.id())) {
            var stored = m.round() == null ? Optional.<TarjetaRound>empty()
                    : tarjetas.find(s.fightId, s.seat, m.round());
            send(s, new Ack(m.id(), true, stored.map(TarjetaRound::estado).orElse(null), null));
            return;
        }
        var pelea = peleas.find(s.fightId).orElseThrow();
        var error = ScoreValidator.validar(new ScoreValidator.Sesion(s.fightId, s.seat, pelea.rounds()), m);
        if (error.isPresent()) {
            send(s, new Ack(m.id(), false, null, error.get()));
            return;
        }
        // Una conexión de traspaso entrega a la pelea cerrada lo que el teléfono no alcanzó a mandar (principio 1).
        // La pelea termina sola al cerrar el último round: su tarjeta tardía aún entra, salvo que una terminación la cortó.
        boolean accepts = pelea.estado() == EstadoPelea.EN_CURSO
                || (pelea.estado() == EstadoPelea.TERMINADA && terminaciones.find(s.fightId).isEmpty())
                || (s.handoff && pelea.estado() == EstadoPelea.CERRADA);
        if (!accepts) {
            send(s, new Ack(m.id(), false, null, Ack.Error.FIGHT_NOT_ACTIVE));
            return;
        }
        int roundNo = m.round();
        var round = rounds.find(s.fightId, roundNo).map(Round::estado).orElse(EstadoRound.PENDIENTE);
        var existing = tarjetas.find(s.fightId, s.seat, roundNo).orElse(null);
        EstadoTarjeta estado;
        if (round == EstadoRound.ABIERTO && (existing == null || existing.origen() == OrigenTarjeta.JUEZ)) {
            estado = EstadoTarjeta.ENVIADA; // también una corrección del juez con el round abierto
        } else if (round == EstadoRound.CERRADO && existing == null) {
            estado = EstadoTarjeta.TARDIA;
        } else {
            if (existing != null && existing.estado() == EstadoTarjeta.CAPTURADA) {
                // Lo capturado manda, pero el organizador debe ver que el juez puntuó distinto o igual.
                logSeat(s, "CARD_OVER_CAPTURE", "round " + roundNo + " juez " + m.red() + "-" + m.blue()
                        + " capturada " + existing.rojo() + "-" + existing.azul());
            }
            send(s, new Ack(m.id(), false, null, Ack.Error.ROUND_NOT_OPEN));
            return;
        }
        long now = clock.getAsLong();
        var card = new TarjetaRound(s.fightId, s.seat, roundNo, m.red(), m.blue(), estado, OrigenTarjeta.JUEZ, now);
        db.inTransaction(c -> {
            processed.markIfNew(s.fightId, s.seat, m.id());
            tarjetas.save(card);
            versiones.append(new VersionTarjeta(s.fightId, s.seat, roundNo,
                    existing == null ? null : existing.rojo(), existing == null ? null : existing.azul(),
                    m.red(), m.blue(), OrigenTarjeta.JUEZ, null, now));
            log.append(s.fightId, "SUBMIT_ROUND", "lugar " + s.seat + " round " + roundNo + " " + estado);
            return null;
        });
        send(s, new Ack(m.id(), true, estado, null));
        events.onCardReceived(card);
    }

    private void onSubmitFinalCard(Session s, SubmitFinalCard m) {
        if (!s.approved) {
            send(s, new Ack(m.id(), false, null, Ack.Error.NOT_APPROVED));
            return;
        }
        if (processed.seen(s.fightId, s.seat, m.id())) {
            send(s, new Ack(m.id(), true, null, null));
            return;
        }
        if (!s.fightId.equals(m.fightId()) || s.seat != m.seat()) {
            send(s, new Ack(m.id(), false, null, Ack.Error.WRONG_SESSION));
            return;
        }
        var pelea = peleas.find(s.fightId).orElseThrow();
        if (pelea.estado() != EstadoPelea.TERMINADA && !(s.handoff && pelea.estado() == EstadoPelea.CERRADA)) {
            send(s, new Ack(m.id(), false, null, Ack.Error.FIGHT_NOT_ACTIVE));
            return;
        }
        if (finales.byPelea(s.fightId).stream().anyMatch(f -> f.juez() == s.seat)) {
            send(s, new Ack(m.id(), false, null, Ack.Error.ALREADY_FINAL));
            return;
        }
        // Se reconstruye con lo guardado; nunca con lo que dice el mensaje.
        var cards = ownCards(s.fightId, s.seat);
        var hash = FinalCardHash.of(s.fightId, s.seat, cards);
        var key = lugares.publicKey(s.fightId, s.seat).orElse(null);
        boolean valid = hash.equals(m.hash()) && key != null
                && verify(key, DeviceProof.finalCardText(s.fightId, s.seat, cards), m.sig());
        if (!valid) {
            log.append(s.fightId, "FINAL_CARD_REJECTED", "lugar " + s.seat);
            send(s, new Ack(m.id(), false, null, Ack.Error.BAD_SIGNATURE));
            events.onFinalCardRejected(s.fightId, s.seat);
            return;
        }
        long now = clock.getAsLong();
        db.inTransaction(c -> {
            processed.markIfNew(s.fightId, s.seat, m.id());
            finales.save(new TarjetaFinal(s.fightId, s.seat, hash, now), m.sig());
            log.append(s.fightId, "SUBMIT_FINAL_CARD", "lugar " + s.seat);
            return null;
        });
        send(s, new Ack(m.id(), true, null, null));
        events.onFinalCard(s.fightId, s.seat);
    }

    // ---- STATE ----

    private State state(Session s) {
        var pelea = peleas.find(s.fightId).orElseThrow();
        var lugar = lugares.find(s.fightId, s.seat).orElseThrow();
        var current = currentRound(rounds.byPelea(s.fightId));
        boolean finalSent = finales.byPelea(s.fightId).stream().anyMatch(f -> f.juez() == s.seat);
        return new State(s.fightId, s.seat, lugar.nombre(), pelea.esquinaRoja(), pelea.esquinaAzul(), pelea.rounds(),
                pelea.estado(), current.map(Round::numero).orElse(0), current.map(Round::estado).orElse(null),
                ownCards(s.fightId, s.seat), finalSent);
    }

    /** Solo las tarjetas del juez {@code seat}: un juez nunca ve las de otro ni totales. */
    private List<State.OwnCard> ownCards(String fightId, int seat) {
        var out = new ArrayList<State.OwnCard>();
        for (var t : tarjetas.byPelea(fightId)) {
            if (t.juez() == seat) {
                out.add(new State.OwnCard(t.round(), t.rojo(), t.azul(), t.estado()));
            }
        }
        return out;
    }

    /** El round abierto; si no hay, el último cerrado. */
    private static Optional<Round> currentRound(List<Round> rs) {
        var open = rs.stream().filter(r -> r.estado() == EstadoRound.ABIERTO).findFirst();
        return open.isPresent() ? open
                : rs.stream().filter(r -> r.estado() == EstadoRound.CERRADO).max(Comparator.comparingInt(Round::numero));
    }

    // ---- utilidades ----

    private JoinApproved approvedMessage(Pelea pelea, int seat, String judgeName) {
        return new JoinApproved(pelea.id(), seat, judgeName, pelea.esquinaRoja(), pelea.esquinaAzul(), pelea.rounds());
    }

    private List<Session> approvedSessions(String fightId) {
        return holders.values().stream().filter(s -> s.approved && s.fightId.equals(fightId)).toList();
    }

    private void setConexion(String fightId, int seat, EstadoConexion estado) {
        lugares.find(fightId, seat).ifPresent(l -> lugares.save(new LugarJuez(l.peleaId(), l.numero(), l.nombre(),
                l.dispositivo(), l.huellaCredencial(), estado, l.aprobadoPor(), l.aprobadoEnMs())));
    }

    private void rejectAndDrop(Session s, String fightId, int seat, JoinRejected.Reason reason) {
        send(s, new JoinRejected(fightId, seat, reason));
        drop(s, Conn.POLICY, "join rejected");
    }

    private void drop(Session s, int code, String reason) {
        sessions.remove(s.conn.id());
        unauthenticated.remove(s.conn.id());
        if (s.fightId != null) {
            holders.remove(new SeatKey(s.fightId, s.seat), s);
        }
        s.conn.close(code, reason);
    }

    private synchronized void expireIfUnauthenticated(String connId) {
        var s = sessions.get(connId);
        if (s != null && !s.authenticated) {
            drop(s, Conn.POLICY, "timeout");
        }
    }

    private synchronized void expireHandoff(String connId) {
        var s = sessions.get(connId);
        if (s != null && s.handoff) {
            drop(s, 1000, "next fight");
        }
    }

    private void logSeat(Session s, String type, String detail) {
        log.append(s.fightId, type, "lugar " + s.seat + (detail == null ? "" : " " + detail));
    }

    private static void send(Session s, Message m) {
        s.conn.send(Messages.encode(m));
    }

    private static boolean verify(String publicKey, String text, String sig) {
        try {
            return DeviceProof.verify(publicKey, text, sig);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
