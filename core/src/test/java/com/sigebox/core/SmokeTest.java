package com.sigebox.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.junit.jupiter.api.Test;

/** Verifica que el build de core tiene sus dependencias y corre en Java 17+. */
class SmokeTest {

    @Test
    void gsonSerializaYLee() {
        Gson gson = new Gson();
        JsonObject json = gson.fromJson("{\"v\":1,\"type\":\"PING\"}", JsonObject.class);
        assertEquals(1, json.get("v").getAsInt());
        assertEquals("PING", json.get("type").getAsString());
    }

    @Test
    void jsonSchemaValidaYRechaza() {
        Schema schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                .getSchema("{\"type\":\"object\",\"required\":[\"v\"],"
                        + "\"properties\":{\"v\":{\"const\":1}},\"additionalProperties\":false}");
        assertTrue(schema.validate("{\"v\":1}", InputFormat.JSON).isEmpty());
        assertFalse(schema.validate("{\"v\":1,\"extra\":true}", InputFormat.JSON).isEmpty());
    }
}
