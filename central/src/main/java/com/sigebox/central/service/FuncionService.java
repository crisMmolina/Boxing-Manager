package com.sigebox.central.service;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.FuncionRepository;
import com.sigebox.central.db.FuncionRepository.CardRow;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.Pelea;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Función con cartelera (spec 002): una base de rounds y jueces, la lista ordenada de peleas y los
 * cambios de cada pelea antes de empezar. {@link #siguiente} crea la pelea con {@link FightService},
 * así que sigue habiendo una sola pelea activa. Cada cambio se guarda antes de devolverse y queda
 * en la bitácora de la función.
 */
public final class FuncionService {

    /** Estado de una pelea de la cartelera; se deriva de lo guardado, no se guarda aparte. */
    public enum Estado { PROGRAMADA, EN_CURSO, CERRADA, SALTADA }

    public record Funcion(String id, String fecha, int baseRounds, int baseJueces) {}

    public record Esquinas(String roja, String azul) {}

    /** Pelea de la cartelera con sus rounds y jueces efectivos (los suyos o los de la base). */
    public record Fila(int orden, String roja, String azul, int rounds, int jueces, boolean cambiada, Estado estado,
            String peleaId) {}

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final Database db;
    private final FightService fights;
    private final FuncionRepository repo;
    private final PeleaRepository peleas;
    private final LugarJuezRepository lugares;
    private final EventLog log;
    private BiConsumer<String, String> alSiguiente = (anterior, nueva) -> {};
    private final SecureRandom random = new SecureRandom();

    public FuncionService(Database db, FightService fights) {
        this.db = db;
        this.fights = fights;
        this.repo = new FuncionRepository(db);
        this.peleas = new PeleaRepository(db);
        this.lugares = new LugarJuezRepository(db);
        this.log = new EventLog(db);
    }

    /**
     * Quién avisa a los teléfonos (p. ej. {@code JudgeHub::nextFight}): recibe la pelea que termina y la
     * nueva, y se llama solo después de guardar la copia de lugares.
     */
    public synchronized void alSiguiente(BiConsumer<String, String> aviso) {
        this.alSiguiente = aviso == null ? (anterior, nueva) -> {} : aviso;
    }

    /** Crea la función con su base y su cartelera inicial (puede ir vacía). */
    public synchronized Funcion crear(int baseRounds, int baseJueces, List<Esquinas> cartelera) {
        rounds(baseRounds);
        jueces(baseJueces);
        var rows = new ArrayList<CardRow>();
        for (var e : cartelera) {
            rows.add(new CardRow(0, nombre(e.roja(), "la esquina roja"), nombre(e.azul(), "la esquina azul"), null,
                    null, false, null));
        }
        var f = new Funcion(newId(), LocalDate.now().toString(), baseRounds, baseJueces);
        db.inTransaction(c -> {
            repo.save(new FuncionRepository.FuncionRow(f.id(), f.fecha(), baseRounds, baseJueces));
            repo.replaceCard(f.id(), rows);
            repo.log(f.id(), "FUNCION_CREADA", baseRounds + " rounds, " + baseJueces + " jueces, "
                    + rows.size() + " peleas");
            return null;
        });
        return f;
    }

    public synchronized Funcion funcion(String funcionId) {
        var r = repo.find(funcionId).orElseThrow(() -> new IllegalArgumentException("función desconocida"));
        return new Funcion(r.id(), r.fecha(), r.baseRounds(), r.baseJueces());
    }

    public synchronized List<Fila> cartelera(String funcionId) {
        var f = funcion(funcionId);
        return repo.card(funcionId).stream().map(r -> fila(f, r)).toList();
    }

    /** Agrega una pelea al final. */
    public synchronized void agregar(String funcionId, Esquinas e) {
        var rows = new ArrayList<>(rows(funcionId));
        rows.add(new CardRow(0, nombre(e.roja(), "la esquina roja"), nombre(e.azul(), "la esquina azul"), null, null,
                false, null));
        guardar(funcionId, rows, "CARTELERA_CAMBIADA", "agrega " + e.roja() + " vs " + e.azul());
    }

    /** Quita una pelea que no ha empezado. */
    public synchronized void quitar(String funcionId, int orden) {
        var rows = new ArrayList<>(rows(funcionId));
        var r = noEmpezada(funcionId, rows, orden);
        rows.remove(orden - 1);
        guardar(funcionId, rows, "CARTELERA_CAMBIADA", "quita " + r.roja() + " vs " + r.azul());
    }

    /** Mueve una pelea que no ha empezado a la posición {@code nuevoOrden}, sin pasar a las ya empezadas. */
    public synchronized void mover(String funcionId, int orden, int nuevoOrden) {
        var rows = new ArrayList<>(rows(funcionId));
        var r = noEmpezada(funcionId, rows, orden);
        if (nuevoOrden < 1 || nuevoOrden > rows.size()) {
            throw new IllegalArgumentException("posición fuera de la cartelera: " + nuevoOrden);
        }
        rows.remove(orden - 1);
        rows.add(nuevoOrden - 1, r);
        for (int i = rows.indexOf(r) + 1; i < rows.size(); i++) {
            if (empezo(rows.get(i))) {
                throw new IllegalStateException("no se puede poner antes de una pelea que ya empezó");
            }
        }
        guardar(funcionId, rows, "CARTELERA_CAMBIADA", "mueve " + r.roja() + " vs " + r.azul() + " a " + nuevoOrden);
    }

    /** Marca como saltada una pelea que no ha empezado (se queda en la cartelera). */
    public synchronized void saltar(String funcionId, int orden) {
        var rows = new ArrayList<>(rows(funcionId));
        var r = noEmpezada(funcionId, rows, orden);
        rows.set(orden - 1, new CardRow(r.orden(), r.roja(), r.azul(), r.rounds(), r.jueces(), true, null));
        guardar(funcionId, rows, "PELEA_SALTADA", r.roja() + " vs " + r.azul());
    }

    /** Cambia rounds o jueces de una pelea que no ha empezado; {@code null} deja lo que tenía. */
    public synchronized void cambiar(String funcionId, int orden, Integer rounds, Integer jueces) {
        if (rounds != null) {
            rounds(rounds);
        }
        if (jueces != null) {
            jueces(jueces);
        }
        var rows = new ArrayList<>(rows(funcionId));
        var r = noEmpezada(funcionId, rows, orden);
        var nueva = new CardRow(r.orden(), r.roja(), r.azul(), rounds != null ? rounds : r.rounds(),
                jueces != null ? jueces : r.jueces(), r.saltada(), null);
        rows.set(orden - 1, nueva);
        guardar(funcionId, rows, "CARTELERA_CAMBIADA", r.roja() + " vs " + r.azul() + ": "
                + (nueva.rounds() == null ? "base" : nueva.rounds()) + " rounds, "
                + (nueva.jueces() == null ? "base" : nueva.jueces()) + " jueces");
    }

    /**
     * Crea la pelea de la siguiente fila programada con sus rounds y jueces efectivos.
     *
     * @throws IllegalStateException si hay una pelea sin cerrar o no quedan peleas
     */
    public synchronized Pelea siguiente(String funcionId) {
        var f = funcion(funcionId);
        var rows = new ArrayList<>(rows(funcionId));
        int i = 0;
        while (i < rows.size() && (rows.get(i).saltada() || rows.get(i).peleaId() != null)) {
            i++;
        }
        if (i == rows.size()) {
            throw new IllegalStateException("ya no quedan peleas en la cartelera");
        }
        var r = rows.get(i);
        var fila = fila(f, r);
        int index = i;
        String anterior = null;
        for (int k = 0; k < index; k++) {
            if (rows.get(k).peleaId() != null) {
                anterior = rows.get(k).peleaId();
            }
        }
        String previa = anterior;
        var pelea = db.inTransaction(c -> {
            var nueva = fights.create(r.roja(), r.azul(), fila.rounds(), fila.jueces());
            rows.set(index, new CardRow(r.orden(), r.roja(), r.azul(), r.rounds(), r.jueces(), false, nueva.id()));
            repo.replaceCard(funcionId, rows);
            var copiados = previa == null ? List.<LugarJuez>of() : copiarLugares(previa, nueva);
            var detalle = "pelea " + r.orden() + " de " + rows.size() + ": " + r.roja() + " vs " + r.azul();
            var lugaresTxt = copiados.isEmpty() ? "" : ", lugares copiados: " + describir(copiados);
            repo.log(funcionId, "SIGUIENTE_PELEA", detalle + " (" + nueva.id() + ")" + lugaresTxt);
            log.append(nueva.id(), "SIGUIENTE_PELEA", "función " + funcionId + ", " + detalle
                    + (previa == null ? "" : ", desde " + previa) + lugaresTxt);
            if (previa != null) {
                log.append(previa, "SIGUIENTE_PELEA", "función " + funcionId + ", sigue " + nueva.id() + lugaresTxt);
            }
            return nueva;
        });
        if (previa != null) {
            alSiguiente.accept(previa, pelea.id());
        }
        return pelea;
    }

    /**
     * Copia a la pelea nueva los lugares aprobados 1..N de la anterior (nombre, dispositivo, huella y
     * llave pública), desconectados. Los lugares sobrantes no se copian y los nuevos quedan para el QR.
     */
    private List<LugarJuez> copiarLugares(String anterior, Pelea nueva) {
        var copiados = new ArrayList<LugarJuez>();
        for (var l : lugares.byPelea(anterior)) {
            if (l.numero() > nueva.jueces() || l.aprobadoEnMs() == null || l.conexion() == EstadoConexion.RECHAZADO) {
                continue;
            }
            var key = lugares.publicKey(anterior, l.numero()).orElse(null);
            if (key == null) {
                continue;
            }
            lugares.save(new LugarJuez(nueva.id(), l.numero(), l.nombre(), l.dispositivo(), l.huellaCredencial(),
                    EstadoConexion.DESCONECTADO, l.aprobadoPor(), l.aprobadoEnMs()));
            lugares.savePublicKey(nueva.id(), l.numero(), key);
            copiados.add(l);
        }
        return copiados;
    }

    private static String describir(List<LugarJuez> copiados) {
        var sb = new StringBuilder();
        for (var l : copiados) {
            sb.append(sb.length() == 0 ? "" : "; ").append(l.numero()).append('=')
                    .append(FightService.shortFingerprint(l.huellaCredencial()));
        }
        return sb.toString();
    }

    private List<CardRow> rows(String funcionId) {
        funcion(funcionId);
        return repo.card(funcionId);
    }

    private void guardar(String funcionId, List<CardRow> rows, String tipo, String detalle) {
        db.inTransaction(c -> {
            repo.replaceCard(funcionId, rows);
            repo.log(funcionId, tipo, detalle);
            return null;
        });
    }

    private CardRow noEmpezada(String funcionId, List<CardRow> rows, int orden) {
        if (orden < 1 || orden > rows.size()) {
            throw new IllegalArgumentException("no hay pelea " + orden + " en la cartelera");
        }
        var r = rows.get(orden - 1);
        if (empezo(r)) {
            throw new IllegalStateException("la pelea " + orden + " ya empezó");
        }
        if (r.saltada()) {
            throw new IllegalStateException("la pelea " + orden + " está saltada");
        }
        return r;
    }

    private static boolean empezo(CardRow r) {
        return r.peleaId() != null;
    }

    private Fila fila(Funcion f, CardRow r) {
        Estado estado;
        if (r.saltada()) {
            estado = Estado.SALTADA;
        } else if (r.peleaId() == null) {
            estado = Estado.PROGRAMADA;
        } else {
            var p = peleas.find(r.peleaId()).orElse(null);
            estado = p != null && p.estado() == EstadoPelea.CERRADA ? Estado.CERRADA : Estado.EN_CURSO;
        }
        return new Fila(r.orden(), r.roja(), r.azul(), r.rounds() != null ? r.rounds() : f.baseRounds(),
                r.jueces() != null ? r.jueces() : f.baseJueces(), r.rounds() != null || r.jueces() != null, estado,
                r.peleaId());
    }

    private static void rounds(int rounds) {
        if (rounds < 1 || rounds > 12) {
            throw new IllegalArgumentException("los rounds van de 1 a 12");
        }
    }

    private static void jueces(int jueces) {
        if (jueces < 1 || jueces > 5) {
            throw new IllegalArgumentException("los jueces van de 1 a 5");
        }
    }

    /** Mismas reglas que {@link FightService#create}: no vacío, hasta {@value FightService#MAX_NAME}, sin controles. */
    private static String nombre(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("falta el nombre de " + what);
        }
        var t = text.strip();
        if (t.length() > FightService.MAX_NAME) {
            throw new IllegalArgumentException(
                    "el nombre de " + what + " admite hasta " + FightService.MAX_NAME + " caracteres");
        }
        if (t.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("el nombre de " + what + " tiene caracteres no permitidos");
        }
        return t;
    }

    private String newId() {
        String id;
        do {
            var sb = new StringBuilder("F");
            for (int i = 0; i < 5; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            id = sb.toString();
        } while (repo.find(id).isPresent());
        return id;
    }
}
