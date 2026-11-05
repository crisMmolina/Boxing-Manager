package com.sigebox.central.service;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.DescuentoRepository;
import com.sigebox.central.db.EventLog;
import com.sigebox.central.db.LugarJuezRepository;
import com.sigebox.central.db.PeleaRepository;
import com.sigebox.central.db.ResultadoRepository;
import com.sigebox.central.db.RoundRepository;
import com.sigebox.central.db.TarjetaFinalRepository;
import com.sigebox.central.db.TarjetaRoundRepository;
import com.sigebox.central.db.TerminacionRepository;
import com.sigebox.central.export.ResultSheet;
import com.sigebox.central.server.JudgeHub;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.Ganador;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Resultado;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaRound;
import com.sigebox.core.model.Terminacion;
import com.sigebox.core.model.TipoDecision;
import com.sigebox.core.scoring.ResultCalculator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fin de la pelea: cerrar cuando están todas las tarjetas finales (o hay terminación) y calcular el
 * resultado con {@link ResultCalculator} de {@code core}, con los descuentos vigentes.
 */
public final class ResultService {

    /** Lo que se difunde a los jueces; en la app es {@link JudgeHub}. */
    public interface Broadcaster {
        void fightEnded(String fightId, MotivoTerminacion reason);

        void fightClosed(String fightId);
    }

    private final Database db;
    private final Broadcaster broadcaster;
    private final PeleaRepository peleas;
    private final LugarJuezRepository lugares;
    private final RoundRepository rounds;
    private final TarjetaRoundRepository tarjetas;
    private final TarjetaFinalRepository finales;
    private final TerminacionRepository terminaciones;
    private final ResultadoRepository resultados;
    private final DescuentoRepository descuentos;
    private final EventLog log;

    public ResultService(Database db, Broadcaster broadcaster) {
        this.db = db;
        this.broadcaster = broadcaster;
        this.peleas = new PeleaRepository(db);
        this.lugares = new LugarJuezRepository(db);
        this.rounds = new RoundRepository(db);
        this.tarjetas = new TarjetaRoundRepository(db);
        this.finales = new TarjetaFinalRepository(db);
        this.terminaciones = new TerminacionRepository(db);
        this.resultados = new ResultadoRepository(db);
        this.descuentos = new DescuentoRepository(db);
        this.log = new EventLog(db);
    }

    public static ResultService forHub(Database db, JudgeHub hub) {
        return new ResultService(db, new Broadcaster() {
            @Override
            public void fightEnded(String fightId, MotivoTerminacion reason) {
                hub.fightEnded(fightId, reason);
            }

            @Override
            public void fightClosed(String fightId) {
                hub.fightClosed(fightId);
            }
        });
    }

    /**
     * Pelea completa: con todos los rounds cerrados pasa a {@code TERMINADA} y pide a los jueces
     * su tarjeta final. Lo normal es que {@link RoundService#closeRound} ya lo haya hecho al cerrar
     * el último round; entonces no hace nada.
     */
    public synchronized void endFight(String fightId) {
        var pelea = pelea(fightId);
        if (pelea.estado() == EstadoPelea.TERMINADA && endedByLastRound(fightId)) {
            return;
        }
        if (pelea.estado() != EstadoPelea.EN_CURSO) {
            throw new IllegalStateException("la pelea no está en curso");
        }
        long closed = rounds.byPelea(fightId).stream().filter(r -> r.estado() == EstadoRound.CERRADO).count();
        if (closed < pelea.rounds()) {
            throw new IllegalStateException("faltan rounds por cerrar (" + closed + " de " + pelea.rounds() + ")");
        }
        db.inTransaction(c -> {
            peleas.updateEstado(fightId, EstadoPelea.TERMINADA);
            log.append(fightId, "FIGHT_ENDED", "todos los rounds");
            return null;
        });
        broadcaster.fightEnded(fightId, null);
    }

    private boolean endedByLastRound(String fightId) {
        return log.byPelea(fightId).stream().anyMatch(e -> "FIGHT_ENDED".equals(e.tipo())
                && RoundService.ENDED_BY_LAST_ROUND.equals(e.detalle()));
    }

    /** Lugares que aún no mandan su tarjeta final (vacío si ya están todas). */
    public synchronized List<Integer> missingFinalCards(String fightId) {
        var pelea = pelea(fightId);
        var sent = finales.byPelea(fightId).stream().map(f -> f.juez()).collect(Collectors.toSet());
        var out = new ArrayList<Integer>();
        for (int seat = 1; seat <= pelea.jueces(); seat++) {
            if (!sent.contains(seat)) {
                out.add(seat);
            }
        }
        return out;
    }

    /** Si ya se puede cerrar: pelea terminada y todas las tarjetas finales, o una terminación. */
    public synchronized boolean canClose(String fightId) {
        var pelea = pelea(fightId);
        return pelea.estado() == EstadoPelea.TERMINADA
                && (terminaciones.find(fightId).isPresent() || missingFinalCards(fightId).isEmpty());
    }

    /** Si la pelea ya terminó (cerrada o no): antes de esto no se muestran puntos (principio 3). */
    public synchronized boolean ended(String fightId) {
        var estado = pelea(fightId).estado();
        return estado == EstadoPelea.TERMINADA || estado == EstadoPelea.CERRADA;
    }

    /** Resultado calculado por {@code core} con lo guardado y los descuentos (no lo persiste). */
    public synchronized Resultado compute(String fightId) {
        var pelea = pelea(fightId);
        return ResultCalculator.calcular(pelea, lugares.byPelea(fightId), rounds.byPelea(fightId),
                tarjetas.byPelea(fightId), terminaciones.find(fightId).orElse(null), descuentos.byPelea(fightId));
    }

    /** Cierra la pelea: guarda el resultado, la pasa a {@code CERRADA} y avisa a los jueces. */
    public synchronized Resultado close(String fightId) {
        if (pelea(fightId).estado() == EstadoPelea.CERRADA) {
            return resultados.find(fightId).orElseThrow();
        }
        if (!canClose(fightId)) {
            throw new IllegalStateException("faltan tarjetas finales de los jueces " + missingFinalCards(fightId));
        }
        var resultado = compute(fightId);
        db.inTransaction(c -> {
            resultados.save(resultado);
            peleas.updateEstado(fightId, EstadoPelea.CERRADA);
            log.append(fightId, "FIGHT_CLOSED", headline(pelea(fightId), resultado, terminaciones.find(fightId)));
            return null;
        });
        broadcaster.fightClosed(fightId);
        return resultado;
    }

    /** Resultado guardado de una pelea cerrada. */
    public synchronized Optional<Resultado> stored(String fightId) {
        return resultados.find(fightId);
    }

    /**
     * Hoja para el paso Resultado: vacía mientras la pelea siga en curso o si aún falta la tarjeta de
     * algún round que cuenta.
     */
    public synchronized Optional<ResultSheet> scoreSheet(String fightId) {
        if (!ended(fightId)) {
            return Optional.empty();
        }
        try {
            return Optional.of(sheet(fightId));
        } catch (IllegalArgumentException | IllegalStateException incompleta) {
            return Optional.empty();
        }
    }

    /** Hoja para la pantalla de resultado y la exportación: los puntos ya llevan los descuentos. */
    public synchronized ResultSheet sheet(String fightId) {
        var pelea = pelea(fightId);
        var resultado = resultados.find(fightId).orElseGet(() -> compute(fightId));
        var terminacion = terminaciones.find(fightId);
        int lastCounted = terminacion.map(t -> t.round() - 1).orElse(pelea.rounds());
        var closed = rounds.byPelea(fightId).stream().filter(r -> r.estado() == EstadoRound.CERRADO)
                .map(Round::numero).collect(Collectors.toSet());
        Map<String, TarjetaRound> cards = tarjetas.byPelea(fightId).stream()
                .collect(Collectors.toMap(t -> t.juez() + "/" + t.round(), Function.identity()));
        var names = lugares.byPelea(fightId).stream().filter(l -> l.nombre() != null)
                .collect(Collectors.toMap(LugarJuez::numero, LugarJuez::nombre));
        var rows = new ArrayList<ResultSheet.JudgeRow>();
        for (var total : resultado.totales()) {
            var discounted = total.rounds().stream()
                    .collect(Collectors.toMap(Resultado.RoundJuez::round, Function.identity()));
            var cells = new ArrayList<ResultSheet.Cell>();
            for (int r = 1; r <= pelea.rounds(); r++) {
                var t = cards.get(total.juez() + "/" + r);
                boolean counts = r <= lastCounted && closed.contains(r);
                if (t == null || !counts) {
                    cells.add(new ResultSheet.Cell(r, null, null, null));
                    continue;
                }
                // Un resultado guardado antes de los descuentos no trae desglose: vale la tarjeta.
                var d = discounted.get(r);
                cells.add(new ResultSheet.Cell(r, d != null ? d.rojo() : t.rojo(), d != null ? d.azul() : t.azul(),
                        t.estado().name()));
            }
            var name = total.nombre() != null ? total.nombre() : names.get(total.juez());
            rows.add(new ResultSheet.JudgeRow(total.juez(), name, cells, total.rojo(), total.azul(),
                    winnerLabel(pelea, total.ganador())));
        }
        return new ResultSheet(pelea, resultado, headline(pelea, resultado, terminacion), rows,
                terminacion.orElse(null));
    }

    /** "Gana Roja por decisión dividida 2–1", "Empate", "Gana Azul por KO (round 3, 1:42)". */
    public static String headline(Pelea pelea, Resultado r, Optional<Terminacion> terminacion) {
        var t = terminacion.orElse(null);
        if (r.ganador() == Ganador.SIN_DECISION) {
            return "Sin decisión" + (t == null ? "" : " · " + motivo(t.motivo()));
        }
        if (t != null && t.motivo() != MotivoTerminacion.DECISION_TECNICA) {
            return "Gana " + corner(pelea, r.ganador()) + " por " + motivo(t.motivo())
                    + " (round " + t.round() + ", " + t.tiempo() + ")";
        }
        long rojo = r.totales().stream().filter(x -> x.ganador() == Ganador.ROJA).count();
        long azul = r.totales().stream().filter(x -> x.ganador() == Ganador.AZUL).count();
        var prefix = t != null ? "decisión técnica " : "decisión ";
        if (r.ganador() == Ganador.EMPATE) {
            return "Empate" + (r.tipo() == TipoDecision.DIVIDIDA ? " dividido " + rojo + "–" + azul : "");
        }
        long favor = Math.max(rojo, azul);
        long contra = Math.min(rojo, azul);
        return "Gana " + corner(pelea, r.ganador()) + " por " + prefix + tipo(r.tipo()) + " " + favor + "–" + contra;
    }

    private static String motivo(MotivoTerminacion m) {
        return switch (m) {
            case KO -> "KO";
            case KOT -> "KO técnico";
            case MEDICO -> "detención médica";
            case ABANDONO -> "abandono";
            case DESCALIFICACION -> "descalificación";
            case DECISION_TECNICA -> "decisión técnica";
            case SIN_DECISION -> "sin decisión";
            case OTRO -> "otro motivo";
        };
    }

    private static String tipo(TipoDecision t) {
        if (t == null) {
            return "";
        }
        return switch (t) {
            case UNANIME -> "unánime";
            case MAYORITARIA -> "mayoritaria";
            case DIVIDIDA -> "dividida";
        };
    }

    private static String corner(Pelea pelea, Ganador g) {
        return g == Ganador.ROJA ? pelea.esquinaRoja() : pelea.esquinaAzul();
    }

    private static String winnerLabel(Pelea pelea, Ganador g) {
        return switch (g) {
            case ROJA, AZUL -> corner(pelea, g);
            case EMPATE -> "Empate";
            case SIN_DECISION -> "Sin decisión";
        };
    }

    private Pelea pelea(String fightId) {
        return peleas.find(fightId).orElseThrow(() -> new IllegalArgumentException("pelea desconocida"));
    }
}
