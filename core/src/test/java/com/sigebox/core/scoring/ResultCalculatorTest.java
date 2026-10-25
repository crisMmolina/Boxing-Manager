package com.sigebox.core.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoConexion;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.Ganador;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.OrigenTarjeta;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Resultado;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaRound;
import com.sigebox.core.model.Terminacion;
import com.sigebox.core.model.TipoDecision;
import com.sigebox.core.protocol.PeleaReferencia;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ResultCalculatorTest {

    private static final String F = "f-1";

    // ---- ayudas: una pelea de N jueces con un [rojo, azul] por juez y round ----

    private static Pelea pelea(int rounds, int jueces) {
        return new Pelea(F, "Roja", "Azul", rounds, jueces, EstadoPelea.CERRADA, "2026-10-25");
    }

    private static List<LugarJuez> lugares(int jueces) {
        List<LugarJuez> out = new ArrayList<>();
        for (int j = 1; j <= jueces; j++) {
            out.add(new LugarJuez(F, j, "Juez " + j, "d" + j, "h" + j, EstadoConexion.CONECTADO, "org", 0L));
        }
        return out;
    }

    private static List<Round> rounds(int total, int cerrados) {
        List<Round> out = new ArrayList<>();
        for (int r = 1; r <= total; r++) {
            out.add(new Round(F, r, r <= cerrados ? EstadoRound.CERRADO : EstadoRound.ABIERTO, 0L, null, false));
        }
        return out;
    }

    private static TarjetaRound t(int juez, int round, int rojo, int azul) {
        return t(juez, round, rojo, azul, EstadoTarjeta.ENVIADA, 1000L * round);
    }

    private static TarjetaRound t(int juez, int round, int rojo, int azul, EstadoTarjeta estado, long hora) {
        return new TarjetaRound(F, juez, round, rojo, azul, estado, OrigenTarjeta.JUEZ, hora);
    }

    /** {@code puntos[round][juez] = {rojo, azul}}. */
    private static List<TarjetaRound> tarjetas(int[][][] puntos) {
        List<TarjetaRound> out = new ArrayList<>();
        for (int r = 0; r < puntos.length; r++) {
            for (int j = 0; j < puntos[r].length; j++) {
                out.add(t(j + 1, r + 1, puntos[r][j][0], puntos[r][j][1]));
            }
        }
        return out;
    }

    private static Resultado decision(int[][][] puntos) {
        int rounds = puntos.length;
        int jueces = puntos[0].length;
        return ResultCalculator.calcular(pelea(rounds, jueces), lugares(jueces), rounds(rounds, rounds), tarjetas(puntos), null);
    }

    private static int[] p(int rojo, int azul) {
        return new int[] {rojo, azul};
    }

    private static List<Ganador> fallos(Resultado r) {
        List<Ganador> out = new ArrayList<>();
        for (Resultado.TotalJuez t : r.totales()) {
            out.add(t.ganador());
        }
        return out;
    }

    // ---- pelea de referencia (IDEA §3) ----

    @Test
    void peleaDeReferencia() {
        PeleaReferencia ref = PeleaReferencia.cargar();
        Resultado r = ResultCalculator.calcular(ref.pelea(), ref.lugares(), ref.rounds(), ref.tarjetas(), null);
        assertEquals(ref.esperado(), r);
        assertEquals(Ganador.ROJA, r.ganador());
        assertEquals(TipoDecision.DIVIDIDA, r.tipo());
    }

    // ---- 12 rounds × 5 jueces contra cálculo a mano ----

    @Test
    void doceRoundsCincoJuecesContraCalculoAMano() {
        int[][][] puntos = {
            {p(10, 8), p(10, 8), p(9, 10), p(9, 10), p(9, 9)},
            {p(9, 9), p(8, 10), p(10, 8), p(10, 9), p(10, 8)},
            {p(10, 10), p(9, 10), p(9, 9), p(9, 10), p(10, 9)},
            {p(10, 10), p(10, 8), p(10, 8), p(9, 9), p(10, 9)},
            {p(10, 10), p(8, 10), p(8, 10), p(10, 9), p(9, 9)},
            {p(10, 8), p(8, 10), p(10, 9), p(8, 10), p(9, 9)},
            {p(10, 9), p(10, 9), p(10, 9), p(10, 10), p(10, 8)},
            {p(10, 8), p(9, 10), p(10, 8), p(10, 9), p(10, 9)},
            {p(9, 9), p(10, 10), p(9, 10), p(10, 8), p(8, 10)},
            {p(10, 8), p(10, 10), p(8, 10), p(10, 10), p(9, 10)},
            {p(9, 10), p(10, 8), p(10, 8), p(9, 9), p(10, 9)},
            {p(10, 10), p(10, 10), p(9, 10), p(8, 10), p(9, 9)},
        };
        Resultado r = decision(puntos);
        // Sumas a mano: J1 117–109, J2 112–113, J3 112–109, J4 112–113, J5 113–108.
        int[][] aMano = {{117, 109}, {112, 113}, {112, 109}, {112, 113}, {113, 108}};
        for (int j = 0; j < 5; j++) {
            assertEquals(j + 1, r.totales().get(j).juez());
            assertEquals("Juez " + (j + 1), r.totales().get(j).nombre());
            assertEquals(aMano[j][0], r.totales().get(j).rojo(), "rojo juez " + (j + 1));
            assertEquals(aMano[j][1], r.totales().get(j).azul(), "azul juez " + (j + 1));
        }
        assertEquals(List.of(Ganador.ROJA, Ganador.AZUL, Ganador.ROJA, Ganador.AZUL, Ganador.ROJA), fallos(r));
        assertEquals(Ganador.ROJA, r.ganador());
        assertEquals(TipoDecision.DIVIDIDA, r.tipo());
        assertNull(r.motivo());
        assertEquals(new Resultado.Conteo(60, 0, 0, 0), r.conteo());
    }

    // ---- 1 a 5 jueces y tipos de decisión ----

    @Test
    void unJuez() {
        Resultado r = decision(new int[][][] {{p(10, 9)}});
        assertEquals(Ganador.ROJA, r.ganador());
        assertEquals(TipoDecision.UNANIME, r.tipo());
    }

    @Test
    void dosJuecesDivididosEsEmpateDividido() {
        Resultado r = decision(new int[][][] {{p(10, 9), p(9, 10)}});
        assertEquals(Ganador.EMPATE, r.ganador());
        assertEquals(TipoDecision.DIVIDIDA, r.tipo());
    }

    @Test
    void tresJuecesUnanime() {
        Resultado r = decision(new int[][][] {{p(9, 10), p(9, 10), p(8, 10)}});
        assertEquals(Ganador.AZUL, r.ganador());
        assertEquals(TipoDecision.UNANIME, r.tipo());
    }

    @Test
    void mayoritariaCuandoLosDemasDanEmpate() {
        Resultado r = decision(new int[][][] {{p(10, 9), p(10, 9), p(10, 10)}});
        assertEquals(Ganador.ROJA, r.ganador());
        assertEquals(TipoDecision.MAYORITARIA, r.tipo());
    }

    @Test
    void unJuezAFavorYLosDemasEmpateTambienEsMayoritaria() {
        // Regla de IDEA §5 al pie de la letra: final = esquina con más jueces a favor.
        Resultado r = decision(new int[][][] {{p(10, 9), p(10, 10), p(10, 10)}});
        assertEquals(Ganador.ROJA, r.ganador());
        assertEquals(TipoDecision.MAYORITARIA, r.tipo());
    }

    @Test
    void cuatroJuecesDosADosEsEmpateDividido() {
        Resultado r = decision(new int[][][] {{p(10, 9), p(9, 10), p(10, 9), p(9, 10)}});
        assertEquals(Ganador.EMPATE, r.ganador());
        assertEquals(TipoDecision.DIVIDIDA, r.tipo());
    }

    @Test
    void todosEmpatanEsEmpateUnanime() {
        Resultado r = decision(new int[][][] {{p(10, 10), p(9, 9), p(10, 10)}});
        assertEquals(Ganador.EMPATE, r.ganador());
        assertEquals(TipoDecision.UNANIME, r.tipo());
        assertEquals(List.of(Ganador.EMPATE, Ganador.EMPATE, Ganador.EMPATE), fallos(r));
    }

    @Test
    void cincoJuecesTresADosEsDividida() {
        Resultado r = decision(new int[][][] {{p(10, 9), p(10, 9), p(10, 9), p(9, 10), p(9, 10)}});
        assertEquals(Ganador.ROJA, r.ganador());
        assertEquals(TipoDecision.DIVIDIDA, r.tipo());
    }

    @Test
    void empatePorJuezCuandoSusTotalesSonIguales() {
        Resultado r = decision(new int[][][] {{p(10, 9)}, {p(9, 10)}});
        assertEquals(Ganador.EMPATE, r.totales().get(0).ganador());
        assertEquals(19, r.totales().get(0).rojo());
        assertEquals(19, r.totales().get(0).azul());
    }

    // ---- versiones, estados y datos incompletos ----

    @Test
    void usaLaUltimaVersionYCuentaEstados() {
        List<TarjetaRound> ts = new ArrayList<>();
        ts.add(t(1, 1, 10, 9, EstadoTarjeta.ENVIADA, 100));
        ts.add(t(1, 1, 9, 10, EstadoTarjeta.CORREGIDA, 200));
        ts.add(t(1, 2, 10, 9, EstadoTarjeta.TARDIA, 300));
        ts.add(t(2, 2, 10, 8, EstadoTarjeta.CAPTURADA, 400));
        ts.add(t(2, 1, 10, 9, EstadoTarjeta.CORREGIDA, 500));
        ts.add(t(2, 1, 9, 10, EstadoTarjeta.ENVIADA, 50));
        Resultado r = ResultCalculator.calcular(pelea(2, 2), lugares(2), rounds(2, 2), ts, null);
        assertEquals(19, r.totales().get(0).rojo());
        assertEquals(19, r.totales().get(0).azul());
        assertEquals(20, r.totales().get(1).rojo());
        assertEquals(17, r.totales().get(1).azul());
        assertEquals(new Resultado.Conteo(0, 1, 1, 2), r.conteo());
    }

    @Test
    void faltaUnaTarjetaEsUnError() {
        List<TarjetaRound> ts = new ArrayList<>(tarjetas(new int[][][] {{p(10, 9), p(10, 9)}, {p(10, 9), p(10, 9)}}));
        ts.remove(3);
        assertThrows(IllegalArgumentException.class,
                () -> ResultCalculator.calcular(pelea(2, 2), lugares(2), rounds(2, 2), ts, null));
    }

    @Test
    void tarjetaAjenaOInvalidaEsUnError() {
        List<TarjetaRound> otraPelea = List.of(new TarjetaRound("f-9", 1, 1, 10, 9, EstadoTarjeta.ENVIADA, OrigenTarjeta.JUEZ, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ResultCalculator.calcular(pelea(1, 1), lugares(1), rounds(1, 1), otraPelea, null));
        List<TarjetaRound> fuera = List.of(t(1, 1, 11, 9));
        assertThrows(IllegalArgumentException.class,
                () -> ResultCalculator.calcular(pelea(1, 1), lugares(1), rounds(1, 1), fuera, null));
        List<TarjetaRound> juez = List.of(t(1, 1, 10, 9), t(2, 1, 10, 9));
        assertThrows(IllegalArgumentException.class,
                () -> ResultCalculator.calcular(pelea(1, 1), lugares(1), rounds(1, 1), juez, null));
    }

    @Test
    void decisionConRoundSinCerrarEsUnError() {
        int[][][] puntos = {{p(10, 9)}, {p(10, 9)}};
        assertThrows(IllegalStateException.class,
                () -> ResultCalculator.calcular(pelea(2, 1), lugares(1), rounds(2, 1), tarjetas(puntos), null));
    }

    @Test
    void nombreNuloSiNoHayLugar() {
        Resultado r = ResultCalculator.calcular(pelea(1, 1), List.of(), rounds(1, 1), List.of(t(1, 1, 10, 9)), null);
        assertNull(r.totales().get(0).nombre());
    }

    // ---- terminación extraordinaria ----

    private static Terminacion fin(MotivoTerminacion motivo, Esquina ganadora) {
        return new Terminacion(F, motivo, motivo == MotivoTerminacion.OTRO ? "Tormenta" : null, ganadora, 3, "1:42", null);
    }

    @ParameterizedTest
    @EnumSource(value = MotivoTerminacion.class, names = {"KO", "KOT", "MEDICO", "ABANDONO", "DESCALIFICACION", "OTRO"})
    void terminacionConGanadorDaElResultadoPorMotivo(MotivoTerminacion motivo) {
        // 4 rounds programados, terminó en el 3: solo cuentan los 2 cerrados; las tarjetas del 3 se ignoran.
        List<TarjetaRound> ts = tarjetas(new int[][][] {{p(9, 10), p(9, 10), p(9, 10)}, {p(9, 10), p(9, 10), p(9, 10)}});
        ts.add(t(1, 3, 9, 10));
        Resultado r = ResultCalculator.calcular(pelea(4, 3), lugares(3), rounds(4, 2), ts, fin(motivo, Esquina.ROJA));
        assertEquals(Ganador.ROJA, r.ganador());
        assertEquals(motivo, r.motivo());
        assertNull(r.tipo());
        assertEquals(18, r.totales().get(0).rojo());
        assertEquals(20, r.totales().get(0).azul());
        assertEquals(6, r.conteo().enviadas());
    }

    @Test
    void terminacionSinGanadoraEsUnError() {
        assertThrows(IllegalArgumentException.class, () -> ResultCalculator.calcular(pelea(4, 1), lugares(1),
                rounds(4, 0), List.of(), fin(MotivoTerminacion.KO, null)));
    }

    @Test
    void otroSinGanadoraEsSinDecision() {
        Resultado r = ResultCalculator.calcular(pelea(4, 1), lugares(1), rounds(4, 0), List.of(),
                fin(MotivoTerminacion.OTRO, null));
        assertEquals(Ganador.SIN_DECISION, r.ganador());
    }

    @Test
    void otroSinTextoEsUnError() {
        Terminacion sinTexto = new Terminacion(F, MotivoTerminacion.OTRO, " ", Esquina.AZUL, 2, "0:10", null);
        assertThrows(IllegalArgumentException.class,
                () -> ResultCalculator.calcular(pelea(4, 1), lugares(1), rounds(4, 0), List.of(), sinTexto));
    }

    @Test
    void sinDecision() {
        Resultado r = ResultCalculator.calcular(pelea(4, 1), lugares(1), rounds(4, 1), List.of(t(1, 1, 10, 9)),
                fin(MotivoTerminacion.SIN_DECISION, null));
        assertEquals(Ganador.SIN_DECISION, r.ganador());
        assertEquals(MotivoTerminacion.SIN_DECISION, r.motivo());
        assertNull(r.tipo());
        assertEquals(10, r.totales().get(0).rojo());
    }

    @Test
    void decisionTecnicaSeCalculaConLosRoundsCerrados() {
        // Round 3 abierto con tarjetas que favorecen a Azul: no deben contar.
        List<TarjetaRound> ts = tarjetas(new int[][][] {
            {p(10, 9), p(10, 9), p(9, 10)},
            {p(10, 9), p(10, 10), p(9, 10)},
            {p(8, 10), p(8, 10), p(8, 10)},
        });
        Resultado r = ResultCalculator.calcular(pelea(6, 3), lugares(3), rounds(6, 2), ts,
                fin(MotivoTerminacion.DECISION_TECNICA, null));
        assertEquals(List.of(Ganador.ROJA, Ganador.ROJA, Ganador.AZUL), fallos(r));
        assertEquals(Ganador.ROJA, r.ganador());
        assertEquals(TipoDecision.DIVIDIDA, r.tipo());
        assertEquals(MotivoTerminacion.DECISION_TECNICA, r.motivo());
        assertEquals(new Resultado.Conteo(6, 0, 0, 0), r.conteo());
    }

    @Test
    void terminacionDeOtraPeleaEsUnError() {
        Terminacion ajena = new Terminacion("f-9", MotivoTerminacion.KO, null, Esquina.ROJA, 1, "0:30", null);
        assertThrows(IllegalArgumentException.class,
                () -> ResultCalculator.calcular(pelea(4, 1), lugares(1), rounds(4, 0), List.of(), ajena));
    }
}
