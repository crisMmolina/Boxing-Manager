package com.sigebox.core.scoring;

import com.sigebox.core.model.Esquina;
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
import com.sigebox.core.validation.ScoreValidator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Calcula el resultado de una pelea (IDEA §5, RC-xx). Determinista y sin estado.
 *
 * <ul>
 *   <li>Por juez y round cuenta la última versión de la tarjeta (mayor {@code horaMs}; si empatan, la que viene
 *       después en la lista).
 *   <li>Total por juez = suma de sus rounds contados. Ganador por juez = mayor total; igual = empate.
 *   <li>Resultado = esquina con más jueces a favor; igual = empate. Tipo: unánime (todos con el ganador, o todos
 *       empate), mayoritaria (los demás dieron empate) o dividida (alguien votó por la otra esquina).
 *   <li>Sin terminación cuentan todos los rounds, que deben estar cerrados. Con terminación cuentan solo los rounds
 *       cerrados: en decisión técnica el resultado sale de esas tarjetas; con otro motivo lo da el motivo.
 * </ul>
 */
public final class ResultCalculator {

    private ResultCalculator() {}

    /**
     * @param terminacion {@code null} si la pelea llegó al último round
     * @throws IllegalArgumentException si una tarjeta no es de la pelea o es inválida, falta una tarjeta de un round
     *     contado, o la terminación es incompleta
     * @throws IllegalStateException si, sin terminación, algún round no está cerrado
     */
    public static Resultado calcular(Pelea pelea, List<LugarJuez> lugares, List<Round> rounds,
            List<TarjetaRound> tarjetas, Terminacion terminacion) {
        Objects.requireNonNull(pelea, "pelea");
        validarTerminacion(pelea, terminacion);
        TreeSet<Integer> contados = roundsContados(pelea, rounds, terminacion);
        Map<Integer, TarjetaRound> vigentes = vigentes(pelea, tarjetas);

        List<Resultado.TotalJuez> totales = new ArrayList<>();
        int enviadas = 0;
        int tardias = 0;
        int capturadas = 0;
        int corregidas = 0;
        int porRoja = 0;
        int porAzul = 0;
        for (int juez = 1; juez <= pelea.jueces(); juez++) {
            int rojo = 0;
            int azul = 0;
            for (int round : contados) {
                TarjetaRound t = vigentes.get(clave(juez, round));
                if (t == null) {
                    throw new IllegalArgumentException("falta la tarjeta del juez " + juez + " en el round " + round);
                }
                rojo += t.rojo();
                azul += t.azul();
                switch (t.estado()) {
                    case ENVIADA:
                        enviadas++;
                        break;
                    case TARDIA:
                        tardias++;
                        break;
                    case CAPTURADA:
                        capturadas++;
                        break;
                    default:
                        corregidas++;
                        break;
                }
            }
            Ganador fallo = rojo > azul ? Ganador.ROJA : azul > rojo ? Ganador.AZUL : Ganador.EMPATE;
            if (fallo == Ganador.ROJA) {
                porRoja++;
            } else if (fallo == Ganador.AZUL) {
                porAzul++;
            }
            totales.add(new Resultado.TotalJuez(juez, nombre(lugares, juez), rojo, azul, fallo));
        }
        Resultado.Conteo conteo = new Resultado.Conteo(enviadas, tardias, capturadas, corregidas);
        List<Resultado.TotalJuez> lista = Collections.unmodifiableList(totales);

        if (terminacion != null && terminacion.motivo() != MotivoTerminacion.DECISION_TECNICA) {
            return new Resultado(pelea.id(), lista, porMotivo(terminacion), null, terminacion.motivo(), conteo);
        }
        Ganador ganador;
        TipoDecision tipo;
        if (porRoja != porAzul) {
            ganador = porRoja > porAzul ? Ganador.ROJA : Ganador.AZUL;
            int aFavor = Math.max(porRoja, porAzul);
            int enContra = Math.min(porRoja, porAzul);
            tipo = aFavor == pelea.jueces() ? TipoDecision.UNANIME
                    : enContra == 0 ? TipoDecision.MAYORITARIA : TipoDecision.DIVIDIDA;
        } else {
            ganador = Ganador.EMPATE;
            tipo = porRoja == 0 ? TipoDecision.UNANIME : TipoDecision.DIVIDIDA;
        }
        MotivoTerminacion motivo = terminacion == null ? null : terminacion.motivo();
        return new Resultado(pelea.id(), lista, ganador, tipo, motivo, conteo);
    }

    private static void validarTerminacion(Pelea pelea, Terminacion terminacion) {
        if (terminacion == null) {
            return;
        }
        if (!pelea.id().equals(terminacion.peleaId()) || terminacion.motivo() == null) {
            throw new IllegalArgumentException("terminación de otra pelea o sin motivo");
        }
        if (terminacion.motivo() == MotivoTerminacion.OTRO
                && (terminacion.textoOtro() == null || terminacion.textoOtro().trim().isEmpty())) {
            throw new IllegalArgumentException("el motivo Otro exige una razón escrita");
        }
    }

    private static Ganador porMotivo(Terminacion terminacion) {
        if (terminacion.motivo() == MotivoTerminacion.SIN_DECISION) {
            return Ganador.SIN_DECISION;
        }
        if (terminacion.ganadora() != null) {
            return terminacion.ganadora() == Esquina.ROJA ? Ganador.ROJA : Ganador.AZUL;
        }
        if (terminacion.motivo() == MotivoTerminacion.OTRO) {
            return Ganador.SIN_DECISION;
        }
        throw new IllegalArgumentException("el motivo " + terminacion.motivo() + " exige esquina ganadora");
    }

    private static TreeSet<Integer> roundsContados(Pelea pelea, List<Round> rounds, Terminacion terminacion) {
        TreeSet<Integer> cerrados = new TreeSet<>();
        for (Round r : rounds) {
            if (r.estado() == EstadoRound.CERRADO && r.numero() >= 1 && r.numero() <= pelea.rounds()) {
                cerrados.add(r.numero());
            }
        }
        if (terminacion == null && cerrados.size() != pelea.rounds()) {
            throw new IllegalStateException("la pelea no terminó: hay rounds sin cerrar");
        }
        return cerrados;
    }

    private static Map<Integer, TarjetaRound> vigentes(Pelea pelea, List<TarjetaRound> tarjetas) {
        Map<Integer, TarjetaRound> out = new HashMap<>();
        for (TarjetaRound t : tarjetas) {
            if (ScoreValidator.validar(pelea, t).isPresent()) {
                throw new IllegalArgumentException(
                        "tarjeta inválida del juez " + t.juez() + " en el round " + t.round());
            }
            TarjetaRound actual = out.get(clave(t.juez(), t.round()));
            if (actual == null || t.horaMs() >= actual.horaMs()) {
                out.put(clave(t.juez(), t.round()), t);
            }
        }
        return out;
    }

    private static int clave(int juez, int round) {
        return juez * 100 + round;
    }

    private static String nombre(List<LugarJuez> lugares, int juez) {
        for (LugarJuez l : lugares) {
            if (l.numero() == juez) {
                return l.nombre();
            }
        }
        return null;
    }
}
