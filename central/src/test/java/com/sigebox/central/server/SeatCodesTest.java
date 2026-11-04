package com.sigebox.central.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** El código de 6 dígitos identifica al lugar (ADR 0004): único entre las peleas abiertas y de un solo uso. */
class SeatCodesTest {

    /** Repite cada número dos veces: el segundo lugar choca con el primero si no se regenera. */
    static final class Repeating extends SecureRandom {
        private int n;

        @Override
        public int nextInt(int bound) {
            return (n++ / 2) % bound;
        }
    }

    @Test
    void codigosUnicosEntreVeintePeleasDeCincoLugares() {
        var t = new SeatTokens(new Repeating());
        var codes = new HashSet<String>();
        for (int f = 0; f < 20; f++) {
            for (int seat = 1; seat <= 5; seat++) {
                var code = t.issueInvitation("P" + f, seat).code();
                assertTrue(code.matches("\\d{6}"), code);
                assertTrue(codes.add(code), "repetido: " + code);
            }
        }
        assertEquals(100, codes.size());
    }

    @Test
    void reemitirUnLugarNoChocaConSuPropioCodigoViejo() {
        var t = new SeatTokens(new Repeating());
        var first = t.issueInvitation("P1", 1).code();
        var second = t.issueInvitation("P1", 1).code();
        assertNotEquals(first, second);
        assertEquals(Optional.empty(), t.lookup(first));
        assertEquals(Optional.of(new SeatTokens.Seat("P1", 1)), t.lookup(second));
    }

    @Test
    void elCodigoDiceSuPeleaYLugarHastaQueSeUsa() {
        var t = new SeatTokens();
        var inv = t.issueInvitation("P1", 3);
        assertEquals(Optional.of(new SeatTokens.Seat("P1", 3)), t.lookup(inv.code()));
        assertEquals(SeatTokens.Check.OK, t.consume("P1", 3, inv.code()));
        assertEquals(Optional.empty(), t.lookup(inv.code()), "ya usado");
    }

    @Test
    void codigoDesconocidoOMalFormadoNoDaNada() {
        var t = new SeatTokens();
        t.issueInvitation("P1", 1);
        assertEquals(Optional.empty(), t.lookup("12345"));
        assertEquals(Optional.empty(), t.lookup(null));
    }

    @Test
    void alOlvidarLaPeleaSusCodigosDejanDeServirYSeLiberan() {
        var t = new SeatTokens();
        var inv = t.issueInvitation("P1", 1);
        t.forget("P1");
        assertEquals(Optional.empty(), t.lookup(inv.code()));
        assertEquals(SeatTokens.Check.INVALID, t.consume("P1", 1, inv.code()));
    }
}
