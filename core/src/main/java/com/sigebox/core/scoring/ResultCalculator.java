package com.sigebox.core.scoring;

import com.sigebox.core.model.Descuento;
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
 *   <li>Resultado y tipo: {@link DecisionRules}.
 *   <li>Sin terminación cuentan todos los rounds, que deben estar cerrados. Con terminación cuentan solo los rounds
 *       cerrados anteriores a {@code terminacion.round()}: en decisión técnica el resultado sale de esas tarjetas;
 *       con otro motivo lo da el motivo.
 *   <li>Cada descuento vigente resta 1 a su esquina en su round, en la tarjeta de cada juez y sin bajar de 0 (ADR 0005).
 *       Los anulados y los de rounds que no cuentan no restan. Las firmas y las tarjetas guardadas no cambian: el
 *       descuento solo existe en este cálculo.
 * </ul>
 *
 * <p>{@code Terminacion.round} es el round en que se detuvo la pelea; ese round no cuenta aunque esté cerrado. Un
 * abandono en la esquina entre rounds se registra en el round que iba a empezar, con tiempo {@code 0:00}, así el
 * round recién terminado sí cuenta.
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
        return calcular(pelea, lugares, rounds, tarjetas, terminacion, Collections.emptyList(), false);
    }

    /**
     * Como {@link #calcular(Pelea, List, List, List, Terminacion)}, con descuentos. Los totales, el fallo de cada juez,
     * el ganador y el tipo salen de los puntos ya descontados, y cada {@link Resultado.TotalJuez} trae además el
     * desglose por round y sus descuentos aplicados, aunque la lista venga vacía. La firma sin descuentos da los mismos
     * totales y decisión que una lista vacía, pero sin desglose.
     *
     * @param descuentos todos los de la pelea, anulados incluidos
     * @throws IllegalArgumentException además, si un descuento es de otra pelea, no tiene esquina, su round está fuera
     *     de rango o no vale {@link Descuento#PUNTOS}
     */
    public static Resultado calcular(Pelea pelea, List<LugarJuez> lugares, List<Round> rounds,
            List<TarjetaRound> tarjetas, Terminacion terminacion, List<Descuento> descuentos) {
        Objects.requireNonNull(descuentos, "descuentos");
        return calcular(pelea, lugares, rounds, tarjetas, terminacion, descuentos, true);
    }

    private static Resultado calcular(Pelea pelea, List<LugarJuez> lugares, List<Round> rounds,
            List<TarjetaRound> tarjetas, Terminacion terminacion, List<Descuento> descuentos, boolean desglose) {
        Objects.requireNonNull(pelea, "pelea");
        validarTerminacion(pelea, terminacion);
        List<Descuento> aRestar = descuentosVigentes(pelea, descuentos);
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
            List<Resultado.RoundJuez> porRound = new ArrayList<>();
            List<Descuento> aplicados = new ArrayList<>();
            for (int round : contados) {
                TarjetaRound t = vigentes.get(clave(juez, round));
                if (t == null) {
                    throw new IllegalArgumentException("falta la tarjeta del juez " + juez + " en el round " + round);
                }
                int puntosRojo = t.rojo();
                int puntosAzul = t.azul();
                for (Descuento d : aRestar) {
                    if (d.round() != round) {
                        continue;
                    }
                    if (d.esquina() == Esquina.ROJA && puntosRojo > 0) {
                        puntosRojo -= d.puntos();
                        aplicados.add(d);
                    } else if (d.esquina() == Esquina.AZUL && puntosAzul > 0) {
                        puntosAzul -= d.puntos();
                        aplicados.add(d);
                    }
                }
                rojo += puntosRojo;
                azul += puntosAzul;
                porRound.add(new Resultado.RoundJuez(round, puntosRojo, puntosAzul));
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
                    case CORREGIDA:
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
            totales.add(desglose
                    ? new Resultado.TotalJuez(juez, nombre(lugares, juez), rojo, azul, fallo, porRound, aplicados)
                    : new Resultado.TotalJuez(juez, nombre(lugares, juez), rojo, azul, fallo));
        }
        Resultado.Conteo conteo = new Resultado.Conteo(enviadas, tardias, capturadas, corregidas);
        List<Resultado.TotalJuez> lista = Collections.unmodifiableList(totales);

        if (terminacion != null && terminacion.motivo() != MotivoTerminacion.DECISION_TECNICA) {
            return new Resultado(pelea.id(), lista, porMotivo(terminacion), null, terminacion.motivo(), conteo);
        }
        DecisionRules.Fallo fallo = DecisionRules.fallo(porRoja, porAzul, pelea.jueces());
        MotivoTerminacion motivo = terminacion == null ? null : terminacion.motivo();
        return new Resultado(pelea.id(), lista, fallo.ganador(), fallo.tipo(), motivo, conteo);
    }

    private static void validarTerminacion(Pelea pelea, Terminacion terminacion) {
        if (terminacion == null) {
            return;
        }
        if (!pelea.id().equals(terminacion.peleaId()) || terminacion.motivo() == null) {
            throw new IllegalArgumentException("terminación de otra pelea o sin motivo");
        }
        if (terminacion.round() < 1 || terminacion.round() > pelea.rounds()) {
            throw new IllegalArgumentException("round de la terminación fuera de rango");
        }
        if (terminacion.motivo() == MotivoTerminacion.OTRO
                && (terminacion.textoOtro() == null || terminacion.textoOtro().trim().isEmpty())) {
            throw new IllegalArgumentException("el motivo Otro exige una razón escrita");
        }
    }

    private static List<Descuento> descuentosVigentes(Pelea pelea, List<Descuento> descuentos) {
        List<Descuento> out = new ArrayList<>();
        for (Descuento d : descuentos) {
            if (!pelea.id().equals(d.fightId()) || d.esquina() == null) {
                throw new IllegalArgumentException("descuento " + d.id() + " de otra pelea o sin esquina");
            }
            if (d.round() < 1 || d.round() > pelea.rounds() || d.puntos() != Descuento.PUNTOS) {
                throw new IllegalArgumentException("descuento " + d.id() + " con round o puntos fuera de rango");
            }
            if (!d.anulado()) {
                out.add(d);
            }
        }
        return out;
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
        int ultimo = terminacion == null ? pelea.rounds() : terminacion.round() - 1;
        TreeSet<Integer> cerrados = new TreeSet<>();
        for (Round r : rounds) {
            if (r.estado() == EstadoRound.CERRADO && r.numero() >= 1 && r.numero() <= ultimo) {
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
