package com.sigebox.core.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** NEXT_FIGHT (spec 002, ADR 0002): la central pasa al juez a la siguiente pelea de la función. */
class NextFightTest {

    @Test
    void elFixtureSeLeeYVuelveIgual() {
        var json = ProtocolSchemaTest.fixture(MessageType.NEXT_FIGHT).toString();
        var msg = assertInstanceOf(NextFight.class, Messages.decode(json));
        assertEquals("pelea-ref-001", msg.fightId());
        assertEquals(2, msg.seat());
        assertEquals(msg, Messages.decode(Messages.encode(msg)));
    }

    @Test
    void sinLugarElJuezNoParticipaYSeOmiteDelJson() {
        var msg = new NextFight("f-1", "f-2", null, "Roja", "Azul", 4);
        assertNull(msg.seat());
        var json = Messages.encode(msg);
        assertTrue(!json.contains("seat"), json);
        assertTrue(ProtocolSchemaTest.valid(MessageType.NEXT_FIGHT, json), json);
        assertEquals(msg, Messages.decode(json));
    }

    @Test
    void validaRangosYTextos() {
        assertThrows(IllegalArgumentException.class, () -> new NextFight("f-1", "f-2", 0, "Roja", "Azul", 4));
        assertThrows(IllegalArgumentException.class, () -> new NextFight("f-1", "f-2", 6, "Roja", "Azul", 4));
        assertThrows(IllegalArgumentException.class, () -> new NextFight("f-1", "f-2", 1, "Roja", "Azul", 13));
        assertThrows(IllegalArgumentException.class, () -> new NextFight("f-1", "f-2", 1, "Ro‮ja", "Azul", 4));
        assertThrows(IllegalArgumentException.class, () -> new NextFight("f-1", " ", 1, "Roja", "Azul", 4));
        var bad = ProtocolSchemaTest.fixture(MessageType.NEXT_FIGHT);
        bad.addProperty("seat", 9);
        assertThrows(ProtocolException.class, () -> Messages.decode(bad.toString()));
    }
}
