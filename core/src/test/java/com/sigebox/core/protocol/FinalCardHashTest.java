package com.sigebox.core.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sigebox.core.model.EstadoTarjeta;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinalCardHashTest {

    private static State.OwnCard card(int round, int red, int blue) {
        return new State.OwnCard(round, red, blue, EstadoTarjeta.ENVIADA);
    }

    static final List<State.OwnCard> JUEZ_2 =
            List.of(card(1, 9, 10), card(2, 9, 10), card(3, 10, 9), card(4, 9, 10));

    @Test
    void huellaFijaDelJuez2DeLaPeleaDeReferencia() {
        // printf 'SIGEBOX-FINAL-v1\npelea-ref-001\n2\n1:9:10\n2:9:10\n3:10:9\n4:9:10\n' | shasum -a 256
        assertEquals("65e19d060c0755be1c9b72cf19f772c9230f905fd5d0d02588a6653a467feb4c",
                FinalCardHash.of("pelea-ref-001", 2, JUEZ_2));
    }

    @Test
    void noDependeDelOrdenNiDelEstado() {
        List<State.OwnCard> desordenadas = List.of(card(4, 9, 10), new State.OwnCard(3, 10, 9, EstadoTarjeta.TARDIA),
                card(1, 9, 10), card(2, 9, 10));
        assertEquals(FinalCardHash.of("pelea-ref-001", 2, JUEZ_2), FinalCardHash.of("pelea-ref-001", 2, desordenadas));
    }

    @Test
    void cambiaSiCambiaUnValor() {
        List<State.OwnCard> otra = List.of(card(1, 9, 10), card(2, 9, 10), card(3, 10, 9), card(4, 10, 10));
        assertNotEquals(FinalCardHash.of("pelea-ref-001", 2, JUEZ_2), FinalCardHash.of("pelea-ref-001", 2, otra));
    }

    @Test
    void rechazaRoundRepetido() {
        assertThrows(IllegalArgumentException.class,
                () -> FinalCardHash.of("f", 1, List.of(card(1, 10, 9), card(1, 9, 10))));
    }
}
