package com.sigebox.core.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class MessagesTest {

    private static final String ID = "0f8fad5b-d9cb-469f-a165-70867728950e";

    @ParameterizedTest
    @ValueSource(strings = {
        "{",
        "[]",
        "\"PING\"",
        "",
        "{\"v\":1,\"id\":\"" + ID + "\"}",
        "{\"v\":1,\"id\":\"" + ID + "\",\"type\":{}}",
        "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"KICK\"}",
        "{\"v\":2,\"id\":\"" + ID + "\",\"type\":\"PING\",\"ts\":1}",
        "{\"v\":1,\"id\":\"no-uuid\",\"type\":\"PING\",\"ts\":1}",
        "{\"id\":\"" + ID + "\",\"type\":\"PING\",\"ts\":1}",
        "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"SUBMIT_ROUND\",\"fightId\":\"f\",\"seat\":1,\"round\":1,"
                + "\"red\":\"diez\",\"blue\":9}",
        "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"SUBMIT_ROUND\",\"fightId\":\"f\",\"seat\":1,\"round\":1,"
                + "\"red\":\"10\",\"blue\":9}",
        "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"SUBMIT_ROUND\",\"fightId\":\"f\",\"seat\":1,\"round\":1,"
                + "\"red\":9.5,\"blue\":9}",
        "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"SUBMIT_ROUND\",\"fightId\":7,\"seat\":1,\"round\":1,"
                + "\"red\":10,\"blue\":9}",
        "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"ACK\",\"ackId\":\"" + ID + "\",\"ok\":\"yes\"}",
        "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"ACK\",\"ackId\":\"" + ID + "\",\"ok\":true,\"status\":\"HACKED\"}",
        "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"PING\",\"ts\":\"1\"}",
        "{\"v\":\"1\",\"id\":\"" + ID + "\",\"type\":\"PING\",\"ts\":1}",
        "{v:1,\"id\":\"" + ID + "\",\"type\":\"PING\",\"ts\":1}",
        "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"PING\",\"ts\":1} {}",
        "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"JOIN_REJECTED\",\"fightId\":\"f\",\"seat\":1,\"reason\":\"OTRO\"}",
        "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"STATE\",\"fightId\":\"f\",\"seat\":1,\"judgeName\":\"A\","
                + "\"redCorner\":\"R\",\"blueCorner\":\"B\",\"rounds\":4,\"fightStatus\":\"EN_CURSO\","
                + "\"currentRound\":1,\"cards\":[{\"round\":1,\"red\":10}],\"finalCardSent\":false}"
    })
    void rechazaMensajesInvalidos(String json) {
        assertThrows(ProtocolException.class, () -> Messages.decode(json));
    }

    @Test
    void aceptaNullComoAusenteEnOpcionales() {
        String json = "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"ACK\",\"ackId\":\"" + ID + "\",\"ok\":true,"
                + "\"status\":null}";
        assertEquals(new Ack(1, ID, "ACK", ID, true, null, null), Messages.decode(json));
    }

    private static final String SUBMIT = "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"SUBMIT_ROUND\",\"fightId\":\"f\","
            + "\"seat\":1,\"round\":1,";

    @ParameterizedTest
    @ValueSource(strings = {
        "\"red\":-1,\"blue\":9}",
        "\"red\":11,\"blue\":9}",
        "\"red\":10,\"blue\":9,\"seat\":2}",
    })
    void rechazaFueraDeRangoYClavesRepetidas(String tail) {
        assertThrows(ProtocolException.class, () -> Messages.decode(SUBMIT + tail));
    }

    @Test
    void rechazaAnidamientoProfundo() {
        String deep = "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"PING\",\"ts\":1,\"x\":" + "[".repeat(10000)
                + "]".repeat(10000) + "}";
        assertThrows(ProtocolException.class, () -> Messages.decode(deep));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Ana\u202ELópez", "Ana\nLópez", "Ana\u0000"})
    void rechazaTextoVacioOConCaracteresDeControl(String name) {
        assertThrows(IllegalArgumentException.class, () -> new JoinPending(name, 1));
    }

    @Test
    void rechazaTarjetaNula() {
        java.util.List<State.OwnCard> cards = new java.util.ArrayList<>();
        cards.add(null);
        assertThrows(IllegalArgumentException.class, () -> new State("f", 1, "A", "R", "B", 4,
                com.sigebox.core.model.EstadoPelea.EN_CURSO, 1, null, cards, false));
    }

    @Test
    void rechazaRangoEnConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new SubmitRound("f", 1, 1, 11, 9));
        assertThrows(IllegalArgumentException.class, () -> new SubmitRound("f", 6, 1, 10, 9));
        assertThrows(IllegalArgumentException.class, () -> new Ping(-1L));
    }

    @Test
    void rechazaMensajeDemasiadoGrande() {
        String big = "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"PING\",\"ts\":1,\"x\":\"" + "a".repeat(Messages.MAX_LENGTH)
                + "\"}";
        assertThrows(ProtocolException.class, () -> Messages.decode(big));
    }

    @Test
    void rechazaNumeroDemasiadoLargo() {
        String json = "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"PING\",\"ts\":" + "9".repeat(40) + "}";
        assertThrows(ProtocolException.class, () -> Messages.decode(json));
    }

    @Test
    void errorNoRepiteElValorRecibido() {
        String json = "{\"v\":1,\"id\":\"" + ID + "\",\"type\":\"ACK\",\"ackId\":\"" + ID + "\",\"ok\":false,"
                + "\"error\":\"SECRETO\"}";
        ProtocolException e = assertThrows(ProtocolException.class, () -> Messages.decode(json));
        for (Throwable t = e; t != null; t = t.getCause()) {
            assertFalse(String.valueOf(t.getMessage()).contains("SECRETO"), t.toString());
        }
    }

    @Test
    void ackExigeStatusOErrorSegunOk() {
        assertThrows(IllegalArgumentException.class, () -> new Ack(ID, true, null, Ack.Error.MALFORMED));
        assertThrows(IllegalArgumentException.class,
                () -> new Ack(ID, false, com.sigebox.core.model.EstadoTarjeta.ENVIADA, Ack.Error.MALFORMED));
    }

    @Test
    void rechazaNull() {
        assertThrows(ProtocolException.class, () -> Messages.decode(null));
    }

    @Test
    void omiteCamposNulos() {
        String json = Messages.encode(new Ack(ID, false, null, Ack.Error.ROUND_NOT_OPEN));
        assertFalse(json.contains("status"), json);
        assertTrue(json.contains("\"error\":\"ROUND_NOT_OPEN\""), json);
    }

    @Test
    void noEscapaHtml() {
        String json = Messages.encode(new JoinApproved("f=1", 1, "Ñandú & Co", "a<b", "B", 4));
        assertTrue(json.contains("f=1") && json.contains("a<b") && json.contains("Ñandú & Co"), json);
    }

    @Test
    void cadaMensajeNuevoTieneSuPropioId() {
        Ping a = new Ping(1L);
        Ping b = new Ping(1L);
        assertNotEquals(a.id(), b.id());
        assertEquals(Message.VERSION, a.v());
        assertEquals(Ping.TYPE, a.type());
    }

    @Test
    void constructorRechazaTypeAjeno() {
        assertThrows(IllegalArgumentException.class, () -> new Ping(1, ID, "PONG", 1L));
    }

    @Test
    void stateCopiaLasTarjetasSinAlias() {
        java.util.List<State.OwnCard> cards = new java.util.ArrayList<>();
        cards.add(new State.OwnCard(1, 10, 9, com.sigebox.core.model.EstadoTarjeta.ENVIADA));
        State state = new State("f", 1, "A", "R", "B", 4, com.sigebox.core.model.EstadoPelea.EN_CURSO, 1, null,
                cards, false);
        cards.clear();
        assertEquals(1, state.cards().size());
        assertThrows(UnsupportedOperationException.class, () -> state.cards().clear());
    }

    @Test
    void fromWireRechazaDesconocidos() {
        assertThrows(ProtocolException.class, () -> MessageType.fromWire(null));
        assertEquals(MessageType.ACK, MessageType.fromWire("ACK"));
        assertEquals("submit_round.schema.json", MessageType.SUBMIT_ROUND.schemaFile());
    }
}
