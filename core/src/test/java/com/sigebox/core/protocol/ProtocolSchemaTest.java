package com.sigebox.core.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** El DTO de cada mensaje y su JSON Schema en {@code core/schemas/} deben coincidir. */
class ProtocolSchemaTest {

    static final Path SCHEMAS = Path.of("schemas");
    static final Path FIXTURES = Path.of("..", "demo_data", "mensajes");
    private static final SchemaRegistry REGISTRY =
            SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

    static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + path.toAbsolutePath(), e);
        }
    }

    static Schema schema(MessageType type) {
        return REGISTRY.getSchema(read(SCHEMAS.resolve(type.schemaFile())));
    }

    static JsonObject fixture(MessageType type) {
        return JsonParser.parseString(read(FIXTURES.resolve(type.wire() + ".json"))).getAsJsonObject();
    }

    static boolean valid(MessageType type, String json) {
        return schema(type).validate(json, InputFormat.JSON).isEmpty();
    }

    @Test
    void submitRoundDeEjemploPasaSuSchema() {
        SubmitRound msg = new SubmitRound("f-1", 2, 3, 10, 9);
        String json = Messages.encode(msg);
        assertTrue(valid(MessageType.SUBMIT_ROUND, json), json);
        assertEquals(msg, Messages.decode(json));
    }

    @ParameterizedTest
    @EnumSource(MessageType.class)
    void cadaFixtureCumpleSuSchemaYSobreviveIdaYVuelta(MessageType type) {
        JsonObject json = fixture(type);
        assertTrue(valid(type, json.toString()), type + ": " + schema(type).validate(json.toString(), InputFormat.JSON));
        Message msg = Messages.decode(json.toString());
        assertEquals(type.wire(), msg.type());
        assertEquals(type.dtoClass(), msg.getClass());
        assertEquals(json, JsonParser.parseString(Messages.encode(msg)));
    }

    @ParameterizedTest
    @EnumSource(MessageType.class)
    void campoExtraFallaElSchema(MessageType type) {
        JsonObject json = fixture(type);
        json.addProperty("extra", true);
        assertFalse(valid(type, json.toString()));
    }

    @ParameterizedTest
    @EnumSource(MessageType.class)
    void quitarUnCampoRequeridoFallaSchemaYDecode(MessageType type) {
        for (String field : required(type)) {
            JsonObject json = fixture(type);
            json.remove(field);
            assertFalse(valid(type, json.toString()), type + " sin " + field);
            assertThrows(ProtocolException.class, () -> Messages.decode(json.toString()), type + " sin " + field);
        }
    }

    @ParameterizedTest
    @EnumSource(MessageType.class)
    void quitarUnCampoOpcionalPasaSchemaYDecode(MessageType type) {
        List<String> required = required(type);
        for (String field : fixture(type).keySet()) {
            if (required.contains(field)) {
                continue;
            }
            JsonObject json = fixture(type);
            json.remove(field);
            assertTrue(valid(type, json.toString()), type + " sin " + field);
            Messages.decode(json.toString());
        }
    }

    @Test
    void submitRoundSinAzulNoSeLeeComoCero() {
        JsonObject json = fixture(MessageType.SUBMIT_ROUND);
        json.remove("blue");
        assertThrows(ProtocolException.class, () -> Messages.decode(json.toString()));
    }

    @Test
    void submitRoundFueraDeRangoFallaElSchema() {
        JsonObject json = fixture(MessageType.SUBMIT_ROUND);
        json.addProperty("red", 11);
        assertFalse(valid(MessageType.SUBMIT_ROUND, json.toString()));
        json.addProperty("red", 9.5);
        assertFalse(valid(MessageType.SUBMIT_ROUND, json.toString()));
    }

    @Test
    void mensajesConstruidosEnJavaPasanSuSchema() {
        List<Message> samples = List.of(
                new Join("f-1", 1, "tok", "María López", "dev-1", "huella"),
                new JoinPending("f-1", 1),
                new JoinApproved("f-1", 1, "María López", "Roja", "Azul", 4),
                new JoinRejected("f-1", 1, JoinRejected.Reason.TOKEN_USED),
                new Hello("f-1", 1, "dev-1", "huella"),
                new State("f-1", 1, "María López", "Roja", "Azul", 4,
                        com.sigebox.core.model.EstadoPelea.EN_CURSO, 1,
                        com.sigebox.core.model.EstadoRound.ABIERTO,
                        List.of(new State.OwnCard(1, 10, 9, com.sigebox.core.model.EstadoTarjeta.ENVIADA)), false),
                new RoundOpened("f-1", 1),
                new RoundClosed("f-1", 1, true, false),
                new SubmitRound("f-1", 1, 1, 10, 9),
                new Ack("a3b9c2d4-0000-4000-8000-000000000000", true, com.sigebox.core.model.EstadoTarjeta.TARDIA, null),
                new SubmitFinalCard("f-1", 1, "0".repeat(64)),
                new FightEnded("f-1", com.sigebox.core.model.MotivoTerminacion.KO),
                new FightClosed("f-1"),
                new Ping(1L),
                new Pong(1L));
        assertEquals(MessageType.values().length, samples.size());
        for (Message msg : samples) {
            MessageType type = MessageType.fromWire(msg.type());
            String json = Messages.encode(msg);
            assertTrue(valid(type, json), json);
            assertEquals(msg, Messages.decode(json));
        }
    }

    private static List<String> required(MessageType type) {
        JsonObject schema = JsonParser.parseString(read(SCHEMAS.resolve(type.schemaFile()))).getAsJsonObject();
        List<String> out = new ArrayList<>();
        JsonArray req = schema.getAsJsonArray("required");
        for (JsonElement e : req) {
            out.add(e.getAsString());
        }
        return out;
    }
}
