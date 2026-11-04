package com.sigebox.core.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.model.Ganador;
import com.sigebox.core.model.Resultado;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaRound;
import com.sigebox.core.model.TipoDecision;
import org.junit.jupiter.api.Test;

/** La pelea de referencia es coherente con IDEA §3 (cálculo a mano). */
class PeleaReferenciaTest {

    private final PeleaReferencia ref = PeleaReferencia.cargar();

    @Test
    void tieneLaFormaDeIdea3() {
        assertTrue(ref.demo_data());
        assertEquals(4, ref.pelea().rounds());
        assertEquals(3, ref.pelea().jueces());
        assertEquals(3, ref.lugares().size());
        assertEquals(4, ref.rounds().size());
        assertEquals(12, ref.tarjetas().size());
    }

    @Test
    void sumasPorJuezCoincidenConLasEsperadas() {
        int[][] aMano = {{39, 37}, {37, 39}, {39, 37}};
        for (Resultado.TotalJuez total : ref.esperado().totales()) {
            int rojo = 0;
            int azul = 0;
            for (TarjetaRound t : ref.tarjetas()) {
                if (t.juez() == total.juez()) {
                    rojo += t.rojo();
                    azul += t.azul();
                }
            }
            assertEquals(aMano[total.juez() - 1][0], rojo, "juez " + total.juez());
            assertEquals(aMano[total.juez() - 1][1], azul, "juez " + total.juez());
            assertEquals(rojo, total.rojo());
            assertEquals(azul, total.azul());
        }
    }

    @Test
    void resultadoEsperadoEsRojaDivididaDosAUno() {
        Resultado esperado = ref.esperado();
        assertEquals(Ganador.ROJA, esperado.ganador());
        assertEquals(TipoDecision.DIVIDIDA, esperado.tipo());
        long porRoja = esperado.totales().stream().filter(t -> t.ganador() == Ganador.ROJA).count();
        long porAzul = esperado.totales().stream().filter(t -> t.ganador() == Ganador.AZUL).count();
        assertEquals(2, porRoja);
        assertEquals(1, porAzul);
    }

    @Test
    void elJuez2EnviaTardeElRound3ConCierreForzado() {
        Round r3 = ref.rounds().get(2);
        assertEquals(3, r3.numero());
        assertTrue(r3.cierreForzado());
        long tardias = ref.tarjetas().stream().filter(t -> t.estado() == EstadoTarjeta.TARDIA).count();
        assertEquals(1, tardias);
        TarjetaRound tardia = ref.tarjetas().stream()
                .filter(t -> t.estado() == EstadoTarjeta.TARDIA).findFirst().orElseThrow();
        assertEquals(2, tardia.juez());
        assertEquals(3, tardia.round());
        assertTrue(tardia.horaMs() > r3.cerradoEnMs());
        assertEquals(1, ref.esperado().conteo().tardias());
        assertEquals(11, ref.esperado().conteo().enviadas());
    }
}
