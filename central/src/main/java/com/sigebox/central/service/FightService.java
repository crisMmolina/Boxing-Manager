package com.sigebox.central.service;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.protocol.JoinUri;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Crear la pelea y emparejar a los jueces (DESIGN v2 §7.1): pelea con id corto, una invitación por
 * lugar (QR con el token y código de 6 dígitos) y el estado de cada lugar para aprobar o rechazar.
 */
public final class FightService {

    public static final int MAX_NAME = 80;

    /** Letras y dígitos sin ambigüedades (sin 0/O, 1/I/L) para dictar el id de la pelea. */
    static final String ID_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    /** Largo del id de la pelea (contrato AND-004: ≤ 8). */
    public static final int ID_LENGTH = 6;

    /**
     * Lo que se muestra por lugar antes de que el juez se una. {@code qrUri} es el QR con solo el
     * código (ADR 0004); {@code uri} es la forma anterior con pelea, lugar y token, que sigue sirviendo.
     */
    public record Invitation(int seat, String uri, String code, String qrUri) {}

    /** Estado visible de un lugar. */
    public enum SeatState { SIN_CONECTAR, ESPERANDO_APROBACION, CONECTADO, SIN_CONEXION, RECHAZADO }

    /** Un lugar con el dato que necesita el organizador para aprobarlo. */
    public record Seat(int seat, SeatState state, String judgeName, String shortFingerprint) {}

    private final Database db;
    private final SeatTokens tokens;
    private final Supplier<LocalDate> today;
    private final SecureRandom random = new SecureRandom();
    private final PeleaRepository peleas;
    private final LugarJuezRepository lugares;
    private final EventLog log;

    public FightService(Database db, SeatTokens tokens, Supplier<LocalDate> today) {
        this.db = db;
        this.tokens = tokens;
        this.today = today;
        this.peleas = new PeleaRepository(db);
        this.lugares = new LugarJuezRepository(db);
        this.log = new EventLog(db);
    }

    public FightService(Database db, SeatTokens tokens) {
        this(db, tokens, LocalDate::now);
    }

    /**
     * Crea la pelea en estado {@code CREADA}. Solo hay una pelea activa a la vez.
     *
     * @throws IllegalArgumentException si un dato está fuera de rango
     * @throws IllegalStateException si ya hay una pelea sin cerrar
     */
    public synchronized Pelea create(String redCorner, String blueCorner, int rounds, int judges) {
        var red = name(redCorner, "la esquina roja");
        var blue = name(blueCorner, "la esquina azul");
        if (rounds < 1 || rounds > 12) {
            throw new IllegalArgumentException("los rounds van de 1 a 12");
        }
        if (judges < 1 || judges > 5) {
            throw new IllegalArgumentException("los jueces van de 1 a 5");
        }
        if (peleas.findActive().isPresent()) {
            throw new IllegalStateException("ya hay una pelea sin cerrar");
        }
        var pelea = new Pelea(newId(), red, blue, rounds, judges, EstadoPelea.CREADA, today.get().toString());
        db.inTransaction(c -> {
            peleas.save(pelea);
            log.append(pelea.id(), "FIGHT_CREATED", rounds + " rounds, " + judges + " jueces");
            return null;
        });
        return pelea;
    }

    /** La pelea sin cerrar, si hay. */
    public synchronized Optional<Pelea> active() {
        return peleas.findActive();
    }

    /**
     * Emite (o vuelve a emitir) la invitación del lugar: el código de 6 dígitos, el URI del QR con ese
     * código y el URI anterior con el token. Las anteriores dejan de servir.
     */
    public synchronized Invitation invite(String fightId, int seat, String host, int port) {
        var pelea = peleas.find(fightId).orElseThrow(() -> new IllegalArgumentException("pelea desconocida"));
        if (seat < 1 || seat > pelea.jueces()) {
            throw new IllegalArgumentException("lugar fuera de rango: " + seat);
        }
        var inv = tokens.issueInvitation(fightId, seat);
        log.append(fightId, "INVITATION_ISSUED", "lugar " + seat);
        return new Invitation(seat, new JoinUri(host, port, fightId, seat, inv.token(), null).toUri(), inv.code(),
                new JoinUri(host, port, inv.code(), null).toUri());
    }

    /** Estado de cada lugar, en orden; {@code connected} son los lugares con conexión abierta. */
    public synchronized List<Seat> seats(String fightId, List<Integer> connected) {
        var pelea = peleas.find(fightId).orElseThrow(() -> new IllegalArgumentException("pelea desconocida"));
        var out = new ArrayList<Seat>();
        for (int n = 1; n <= pelea.jueces(); n++) {
            var lugar = lugares.find(fightId, n).orElse(null);
            out.add(lugar == null ? new Seat(n, SeatState.SIN_CONECTAR, null, null)
                    : new Seat(n, state(lugar, connected.contains(n)), lugar.nombre(),
                            shortFingerprint(lugar.huellaCredencial())));
        }
        return out;
    }

    /** Cuántos lugares faltan por conectarse aprobados (para habilitar "Iniciar pelea"). */
    public synchronized int pendingSeats(String fightId, List<Integer> connected) {
        return (int) seats(fightId, connected).stream().filter(s -> s.state() != SeatState.CONECTADO).count();
    }

    /** "AB12 CD34": primeros 8 hex de la huella del teléfono, para comparar con la del juez. */
    public static String shortFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.length() < 8) {
            return null;
        }
        var hex = fingerprint.substring(0, 8).toUpperCase(Locale.ROOT);
        return hex.substring(0, 4) + " " + hex.substring(4);
    }

    /** IPv4 de la red local por la que se pueden conectar los jueces (la más probable primero). */
    public static List<String> localAddresses() {
        var found = new ArrayList<String>();
        try {
            var nics = NetworkInterface.getNetworkInterfaces();
            while (nics != null && nics.hasMoreElements()) {
                var nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) {
                    continue;
                }
                for (var addr : java.util.Collections.list(nic.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {
                        found.add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            // Sin interfaces legibles: se cae a localhost.
        }
        return rankAddresses(found);
    }

    /** Ordena: redes privadas primero (192.168, 10, 172.16–31), sin loopback ni link-local. */
    static List<String> rankAddresses(List<String> addresses) {
        var out = new ArrayList<String>();
        for (var a : addresses) {
            try {
                var addr = InetAddress.getByName(a);
                if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()
                        && !out.contains(a)) {
                    out.add(a);
                }
            } catch (java.net.UnknownHostException e) {
                // Se ignora una dirección mal formada.
            }
        }
        out.sort(Comparator.comparingInt(FightService::rank));
        if (out.isEmpty()) {
            out.add("127.0.0.1");
        }
        return out;
    }

    private static int rank(String a) {
        if (a.startsWith("192.168.")) {
            return 0;
        }
        if (a.startsWith("10.")) {
            return 1;
        }
        var parts = a.split("\\.");
        int second = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        if (a.startsWith("172.") && second >= 16 && second <= 31) {
            return 2;
        }
        return 3;
    }

    private static SeatState state(LugarJuez l, boolean connectedNow) {
        if (l.conexion() == EstadoConexion.RECHAZADO) {
            return SeatState.RECHAZADO;
        }
        if (l.aprobadoEnMs() == null) {
            return SeatState.ESPERANDO_APROBACION;
        }
        return connectedNow ? SeatState.CONECTADO : SeatState.SIN_CONEXION;
    }

    private String newId() {
        String id;
        do {
            var sb = new StringBuilder(ID_LENGTH);
            for (int i = 0; i < ID_LENGTH; i++) {
                sb.append(ID_ALPHABET.charAt(random.nextInt(ID_ALPHABET.length())));
            }
            id = sb.toString();
        } while (peleas.find(id).isPresent());
        return id;
    }

    private static String name(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("falta el nombre de " + what);
        }
        var t = text.strip();
        if (t.length() > MAX_NAME) {
            throw new IllegalArgumentException("el nombre de " + what + " admite hasta " + MAX_NAME + " caracteres");
        }
        if (t.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("el nombre de " + what + " tiene caracteres no permitidos");
        }
        return t;
    }
}
