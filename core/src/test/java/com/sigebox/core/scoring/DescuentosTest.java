package com.sigebox.core.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.sigebox.core.model.Descuento;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DescuentosTest {

    private static final String F = "f-1";

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
        for (int n = 1; n <= total; n++) {
            out.add(new Round(F, n, n <= cerrados ? EstadoRound.CERRADO : EstadoRound.ABIERTO, 0L, null, false));
        }
        return out;
    }

    /** {@code puntos[juez - 1][round - 1] = {rojo, azul}}. */
    private static List<TarjetaRound> tarjetas(int[][][] puntos) {
        List<TarjetaRound> out = new ArrayList<>();
        for (int j = 0; j < puntos.length; j++) {
            for (int r = 0; r < puntos[j].length; r++) {
                out.add(new TarjetaRound(F, j + 1, r + 1, puntos[j][r][0], puntos[j][r][1], EstadoTarjeta.ENVIADA,
                        OrigenTarjeta.JUEZ, 0L));
            }
        }
        return out;
    }

    private static Descuento d(long id, int round, Esquina esquina) {
        return new Descuento(id, F, round, esquina, Descuento.PUNTOS, false, null);
    }

    private static Descuento anulado(long id, int round, Esquina esquina) {
        return new Descuento(id, F, round, esquina, Descuento.PUNTOS, true, "Error del supervisor");
    }

    private static List<Long> ids(Resultado.TotalJuez total) {
        List<Long> out = new ArrayList<>();
        for (Descuento d : total.descuentos()) {
            out.add(d.id());
        }
        return out;
    }

    private static Resultado calcular(int[][][] puntos, List<Descuento> descuentos) {
        int jueces = puntos.length;
        int total = puntos[0].length;
        return ResultCalculator.calcular(pelea(total, jueces), lugares(jueces), rounds(total, total), tarjetas(puntos),
                null, descuentos);
    }

    @Test
    void unDescuentoALaRojaEnElRound1RestaUnPuntoEnLaTarjetaDeCadaJuez() {
        int[][][] puntos = {{{10, 9}, {10, 9}}, {{9, 10}, {10, 9}}, {{10, 9}, {9, 10}}};

        Resultado r = calcular(puntos, List.of(d(1, 1, Esquina.ROJA)));

        assertEquals(19, r.totales().get(0).rojo());
        assertEquals(18, r.totales().get(0).azul());
        assertEquals(18, r.totales().get(1).rojo());
        assertEquals(18, r.totales().get(2).rojo());
    }

    @Test
    void losAnuladosNoCuentan() {
        int[][][] puntos = {{{10, 9}}};

        Resultado r = calcular(puntos, List.of(anulado(1, 1, Esquina.ROJA), d(2, 1, Esquina.AZUL)));

        assertEquals(10, r.totales().get(0).rojo());
        assertEquals(8, r.totales().get(0).azul());
        assertEquals(List.of(2L), ids(r.totales().get(0)));
    }

    @Test
    void nuncaBajaDeCeroYSoloListaLosQueRestaronEnEsaTarjeta() {
        int[][][] puntos = {{{1, 10}}, {{0, 10}}, {{2, 10}}};

        Resultado r = calcular(puntos, List.of(d(1, 1, Esquina.ROJA), d(2, 1, Esquina.ROJA)));

        assertEquals(0, r.totales().get(0).rojo());
        assertEquals(List.of(1L), ids(r.totales().get(0)));
        assertEquals(0, r.totales().get(1).rojo());
        assertEquals(List.of(), ids(r.totales().get(1)));
        assertEquals(0, r.totales().get(2).rojo());
        assertEquals(List.of(1L, 2L), ids(r.totales().get(2)));
    }

    @Test
    void elGanadorYLaDecisionSalenDeLasSumasDescontadas() {
        int[][][] puntos = {{{10, 9}}, {{10, 9}}, {{10, 9}}};

        Resultado r = calcular(puntos, List.of(d(1, 1, Esquina.ROJA), d(2, 1, Esquina.ROJA)));

        assertEquals(Ganador.AZUL, r.ganador());
        assertEquals(TipoDecision.UNANIME, r.tipo());
        assertEquals(Ganador.AZUL, r.totales().get(0).ganador());
    }

    @Test
    void exponeLosPuntosPorRoundYaDescontados() {
        int[][][] puntos = {{{10, 9}, {9, 10}}};

        Resultado r = calcular(puntos, List.of(d(1, 2, Esquina.AZUL)));

        assertEquals(List.of(new Resultado.RoundJuez(1, 10, 9), new Resultado.RoundJuez(2, 9, 9)),
                r.totales().get(0).rounds());
    }

    @Test
    void unDescuentoEnUnRoundQueNoCuentaNoRestaNiSeLista() {
        Terminacion ko = new Terminacion(F, MotivoTerminacion.KO, null, Esquina.ROJA, 3, "1:42", null);

        Resultado r = ResultCalculator.calcular(pelea(4, 1), lugares(1), rounds(4, 2),
                tarjetas(new int[][][] {{{10, 9}, {10, 9}}}), ko,
                List.of(d(1, 2, Esquina.AZUL), d(2, 3, Esquina.ROJA)));

        assertEquals(20, r.totales().get(0).rojo());
        assertEquals(17, r.totales().get(0).azul());
        assertEquals(List.of(1L), ids(r.totales().get(0)));
    }

    @Test
    void conMotivoElGanadorLoDaElMotivoAunqueLosDescuentosCambienLasSumas() {
        Terminacion ko = new Terminacion(F, MotivoTerminacion.KO, null, Esquina.ROJA, 2, "1:42", null);

        Resultado r = ResultCalculator.calcular(pelea(4, 1), lugares(1), rounds(4, 1),
                tarjetas(new int[][][] {{{10, 9}}}), ko, List.of(d(1, 1, Esquina.ROJA), d(2, 1, Esquina.ROJA)));

        assertEquals(8, r.totales().get(0).rojo());
        assertEquals(Ganador.AZUL, r.totales().get(0).ganador());
        assertEquals(Ganador.ROJA, r.ganador());
        assertNull(r.tipo());
        assertEquals(MotivoTerminacion.KO, r.motivo());
    }

    @Test
    void laDecisionTecnicaUsaLasSumasDescontadas() {
        Terminacion tecnica = new Terminacion(F, MotivoTerminacion.DECISION_TECNICA, null, null, 3, "0:50", null);
        int[][][] puntos = {{{10, 9}, {10, 9}}, {{10, 9}, {10, 9}}, {{10, 9}, {10, 9}}};

        Resultado r = ResultCalculator.calcular(pelea(4, 3), lugares(3), rounds(4, 2), tarjetas(puntos), tecnica,
                List.of(d(1, 1, Esquina.ROJA), d(2, 1, Esquina.ROJA), d(3, 2, Esquina.ROJA)));

        assertEquals(17, r.totales().get(0).rojo());
        assertEquals(18, r.totales().get(0).azul());
        assertEquals(Ganador.AZUL, r.ganador());
        assertEquals(TipoDecision.UNANIME, r.tipo());
        assertEquals(MotivoTerminacion.DECISION_TECNICA, r.motivo());
    }

    @Test
    void laFirmaViejaDaElEsperadoDeLaPeleaDeReferencia() {
        PeleaReferencia ref = PeleaReferencia.cargar();

        Resultado r = ResultCalculator.calcular(ref.pelea(), ref.lugares(), ref.rounds(), ref.tarjetas(), null);

        assertEquals(ref.esperado(), r);
    }

    @Test
    void unaListaVaciaDaLosMismosTotalesYDecisionQueLaFirmaVieja() {
        PeleaReferencia ref = PeleaReferencia.cargar();

        Resultado vieja = ResultCalculator.calcular(ref.pelea(), ref.lugares(), ref.rounds(), ref.tarjetas(), null);
        Resultado nueva = ResultCalculator.calcular(ref.pelea(), ref.lugares(), ref.rounds(), ref.tarjetas(), null,
                List.of());

        assertEquals(vieja.ganador(), nueva.ganador());
        assertEquals(vieja.tipo(), nueva.tipo());
        assertEquals(vieja.motivo(), nueva.motivo());
        assertEquals(vieja.conteo(), nueva.conteo());
        for (int i = 0; i < vieja.totales().size(); i++) {
            Resultado.TotalJuez a = vieja.totales().get(i);
            Resultado.TotalJuez b = nueva.totales().get(i);
            assertEquals(a.rojo(), b.rojo());
            assertEquals(a.azul(), b.azul());
            assertEquals(a.ganador(), b.ganador());
            assertTrue(a.rounds().isEmpty());
            assertEquals(4, b.rounds().size());
            assertTrue(b.descuentos().isEmpty());
        }
    }

    /** {@code scoring/pelea_referencia_descuentos.json}: el esperado está calculado a mano. */
    private record CasoDescuentos(List<Descuento> descuentos, Resultado esperado) {}

    @Test
    void casoDeReferenciaCalculadoAMano() throws IOException {
        PeleaReferencia ref = PeleaReferencia.cargar();
        CasoDescuentos caso;
        try (InputStream in = DescuentosTest.class.getResourceAsStream("/scoring/pelea_referencia_descuentos.json");
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            caso = new Gson().fromJson(reader, CasoDescuentos.class);
        }

        Resultado r = ResultCalculator.calcular(ref.pelea(), ref.lugares(), ref.rounds(), ref.tarjetas(), null,
                caso.descuentos());

        assertEquals(caso.esperado(), r);
    }

    @Test
    void rechazaDescuentosInvalidosAunqueEstenAnulados() {
        int[][][] puntos = {{{10, 9}}};
        List<Descuento> malos = List.of(
                new Descuento(1, "f-9", 1, Esquina.ROJA, Descuento.PUNTOS, false, null),
                new Descuento(2, F, 0, Esquina.ROJA, Descuento.PUNTOS, false, null),
                new Descuento(3, F, 2, Esquina.ROJA, Descuento.PUNTOS, false, null),
                new Descuento(4, F, 1, Esquina.ROJA, 2, false, null),
                new Descuento(5, F, 1, null, Descuento.PUNTOS, false, null),
                new Descuento(6, "f-9", 1, Esquina.ROJA, Descuento.PUNTOS, true, "Otra pelea"));
        for (Descuento malo : malos) {
            assertThrows(IllegalArgumentException.class, () -> calcular(puntos, List.of(malo)));
        }
    }

    @Test
    void laListaDeDescuentosEsObligatoriaEnLaFirmaNueva() {
        assertThrows(NullPointerException.class, () -> calcular(new int[][][] {{{10, 9}}}, null));
    }

    @Test
    void unTotalSinDesgloseTraeListasVaciasEInmutables() {
        Resultado.TotalJuez corto = new Resultado.TotalJuez(1, "Juez 1", 10, 9, Ganador.ROJA);
        Resultado.TotalJuez deJson = new Gson().fromJson(
                "{\"juez\":1,\"nombre\":\"Juez 1\",\"rojo\":10,\"azul\":9,\"ganador\":\"ROJA\"}",
                Resultado.TotalJuez.class);

        assertEquals(corto, deJson);
        assertTrue(corto.rounds().isEmpty());
        assertTrue(corto.descuentos().isEmpty());
        List<Resultado.RoundJuez> origen = new ArrayList<>();
        origen.add(new Resultado.RoundJuez(1, 10, 9));
        Resultado.TotalJuez lleno = new Resultado.TotalJuez(1, "Juez 1", 10, 9, Ganador.ROJA, origen, List.of());
        origen.clear();
        assertEquals(1, lleno.rounds().size());
        assertThrows(UnsupportedOperationException.class, () -> lleno.rounds().clear());
    }
}
