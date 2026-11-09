package com.sigebox.central.service;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.db.TerminacionRepository;
import com.sigebox.central.server.JudgeHub;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.Resultado;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.Terminacion;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * Terminación extraordinaria de la pelea (KO, abandono, decisión técnica…). Con las mismas reglas
 * que {@code ResultCalculator}: "Otro" exige razón; los motivos que deciden por sí solos exigen
 * esquina ganadora; el round en que se detuvo no cuenta para las tarjetas.
 */
public final class TerminationService {

    /** Segunda confirmación: el organizador escribe esta palabra. */
    public static final String CONFIRM_WORD = "TERMINAR";

    static final int MAX_TEXT = 500;
    private static final Pattern TIEMPO = Pattern.compile("\\d{1,2}:[0-5]\\d");

    /** Motivos cuyo ganador sale de las tarjetas o que no tienen ganador: sin esquina. */
    private static final Set<MotivoTerminacion> SIN_ESQUINA =
            EnumSet.of(MotivoTerminacion.DECISION_TECNICA, MotivoTerminacion.SIN_DECISION);

    /** Lo que captura el formulario. {@code tiempo} = "m:ss" dentro del round. */
    public record Input(
            MotivoTerminacion motivo, String textoOtro, Esquina ganadora, int round, String tiempo,
            String observaciones) {}

    private final Database db;
    private final BiConsumer<String, MotivoTerminacion> fightEnded;
    private final LongSupplier clock;
    private final PeleaRepository peleas;
    private final RoundRepository rounds;
    private final TerminacionRepository terminaciones;
    private final EventLog log;

    /** @param fightEnded difusión a los jueces (en la app, {@link JudgeHub#fightEnded}) */
    public TerminationService(Database db, BiConsumer<String, MotivoTerminacion> fightEnded, LongSupplier clock) {
        this.db = db;
        this.fightEnded = fightEnded;
        this.clock = clock;
        this.peleas = new PeleaRepository(db);
        this.rounds = new RoundRepository(db);
        this.terminaciones = new TerminacionRepository(db);
        this.log = new EventLog(db);
    }

    public static TerminationService forHub(Database db, JudgeHub hub) {
        return new TerminationService(db, hub::fightEnded, System::currentTimeMillis);
    }

    /** Nombre del motivo para la UI. */
    public static String label(MotivoTerminacion m) {
        return switch (m) {
            case KO -> "KO";
            case KOT -> "KO técnico";
            case MEDICO -> "Detenida por el médico";
            case ABANDONO -> "Abandono";
            case DESCALIFICACION -> "Descalificación";
            case DECISION_TECNICA -> "Decisión técnica";
            case SIN_DECISION -> "Sin decisión";
            case OTRO -> "Otro";
        };
    }

    /** Si el motivo pide esquina ganadora en el formulario. */
    public static boolean requiresCorner(MotivoTerminacion m) {
        return m != MotivoTerminacion.OTRO && !SIN_ESQUINA.contains(m);
    }

    /**
     * Termina la pelea. Exige {@code confirmation} = {@value #CONFIRM_WORD} (la UI pide antes una
     * confirmación). Cierra el round abierto como forzado, guarda la terminación, pasa la pelea a
     * {@code TERMINADA}, lo registra en la bitácora y después avisa a los jueces.
     */
    public synchronized Terminacion terminate(String fightId, Input in, String confirmation) {
        var pelea = peleas.find(fightId).orElseThrow(() -> new IllegalArgumentException("pelea desconocida"));
        if (pelea.estado() != EstadoPelea.EN_CURSO) {
            throw new IllegalStateException("solo se termina una pelea en curso");
        }
        if (terminaciones.find(fightId).isPresent()) {
            throw new IllegalStateException("la pelea ya tiene terminación");
        }
        if (in == null || in.motivo() == null) {
            throw new IllegalArgumentException("falta el motivo");
        }
        var otro = clean(in.textoOtro(), "la razón");
        if (in.motivo() == MotivoTerminacion.OTRO && otro == null) {
            throw new IllegalArgumentException("el motivo Otro exige una razón escrita");
        }
        if (requiresCorner(in.motivo()) && in.ganadora() == null) {
            throw new IllegalArgumentException(label(in.motivo()) + " exige esquina ganadora");
        }
        if (SIN_ESQUINA.contains(in.motivo()) && in.ganadora() != null) {
            throw new IllegalArgumentException(label(in.motivo()) + " no lleva esquina ganadora");
        }
        var all = rounds.byPelea(fightId);
        int reached = all.stream().filter(r -> r.estado() != EstadoRound.PENDIENTE)
                .mapToInt(Round::numero).max().orElse(0);
        int maxRound = Math.min(pelea.rounds(), reached + 1); // abandono entre rounds = el siguiente
        if (in.round() < 1 || in.round() > maxRound) {
            throw new IllegalArgumentException("round fuera de rango: " + in.round() + " (1–" + maxRound + ")");
        }
        if (in.tiempo() == null || !TIEMPO.matcher(in.tiempo().strip()).matches()) {
            throw new IllegalArgumentException("el tiempo va como m:ss");
        }
        var obs = clean(in.observaciones(), "las observaciones");
        if (confirmation == null || !CONFIRM_WORD.equals(confirmation.strip())) {
            throw new IllegalStateException("escribe " + CONFIRM_WORD + " para confirmar");
        }

        var terminacion = new Terminacion(fightId, in.motivo(), otro,
                SIN_ESQUINA.contains(in.motivo()) ? null : in.ganadora(), in.round(), in.tiempo().strip(), obs);
        long now = clock.getAsLong();
        db.inTransaction(c -> {
            for (var r : all) {
                if (r.estado() == EstadoRound.ABIERTO) {
                    rounds.save(new Round(fightId, r.numero(), EstadoRound.CERRADO, r.abiertoEnMs(), now, true));
                    log.append(fightId, "ROUND_CLOSED_FORCED", "round " + r.numero() + " por terminación");
                }
            }
            terminaciones.save(terminacion);
            peleas.updateEstado(fightId, EstadoPelea.TERMINADA);
            log.append(fightId, "FIGHT_TERMINATED", in.motivo() + " round " + in.round() + " " + terminacion.tiempo()
                    + (terminacion.ganadora() == null ? "" : " gana " + terminacion.ganadora()));
            return null;
        });
        fightEnded.accept(fightId, in.motivo());
        return terminacion;
    }

    /** Round y tiempo ("m:ss") de una terminación desde En vivo. */
    public record Punto(int round, String tiempo) {}

    /** Si la pelea se puede terminar ahora: en curso y sin terminación. */
    public synchronized boolean sePuedeTerminar(String fightId) {
        return peleas.find(fightId).map(p -> p.estado() == EstadoPelea.EN_CURSO).orElse(false)
                && terminaciones.find(fightId).isEmpty();
    }

    /**
     * Dónde se termina la pelea si se termina ahora (spec 005): el round abierto con el tiempo que lleva; sin round
     * abierto, el siguiente al último cerrado a 0:00. Ese round no cuenta en {@code ResultCalculator}, así el último
     * cerrado sí cuenta y no se pierde ninguna tarjeta (decisión de DSK-036).
     */
    public synchronized Punto punto(String fightId) {
        var pelea = peleas.find(fightId).orElseThrow(() -> new IllegalArgumentException("pelea desconocida"));
        var all = rounds.byPelea(fightId);
        var open = all.stream().filter(r -> r.estado() == EstadoRound.ABIERTO).findFirst();
        if (open.isPresent()) {
            var r = open.get();
            long ms = r.abiertoEnMs() == null ? 0 : Math.max(0, clock.getAsLong() - r.abiertoEnMs());
            long s = Math.min(ms / 1000, 59 * 60 + 59);
            return new Punto(r.numero(), String.format(java.util.Locale.ROOT, "%d:%02d", s / 60, s % 60));
        }
        int lastClosed = all.stream().filter(r -> r.estado() == EstadoRound.CERRADO).mapToInt(Round::numero).max()
                .orElse(0);
        return new Punto(Math.min(pelea.rounds(), lastClosed + 1), "0:00");
    }

    /**
     * Termina desde En vivo (spec 005): solo el motivo, más la esquina o la razón si el motivo las pide; round y
     * tiempo salen de {@link #punto}. Después cierra la pelea con su resultado, como "Cerrar pelea", y los teléfonos
     * reciben {@code FIGHT_ENDED} y {@code FIGHT_CLOSED} (y {@code EVENT_ENDED} si era la última).
     */
    public Resultado terminarYCerrar(String fightId, MotivoTerminacion motivo, String textoOtro, Esquina ganadora,
            ResultService resultados) {
        synchronized (this) {
            var p = punto(fightId);
            terminate(fightId, new Input(motivo, textoOtro, ganadora, p.round(), p.tiempo(), null), CONFIRM_WORD);
        }
        return resultados.close(fightId);
    }

    /** Texto opcional: sin espacios sobrantes, null si vacío, sin saltos ni caracteres de control. */
    static String clean(String text, String what) {
        if (text == null || text.isBlank()) {
            return null;
        }
        var t = text.strip();
        if (t.length() > MAX_TEXT) {
            throw new IllegalArgumentException(what + " admite hasta " + MAX_TEXT + " caracteres");
        }
        if (t.chars().anyMatch(ch -> Character.isISOControl(ch) && ch != '\n')) {
            throw new IllegalArgumentException(what + " tiene caracteres no permitidos");
        }
        return t;
    }
}
