package com.sigebox.core.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Codifica y decodifica mensajes del protocolo v1 en JSON. Los campos {@code null} se omiten. */
public final class Messages {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private Messages() {}

    public static String encode(Message message) {
        return GSON.toJson(message);
    }

    /**
     * Lee un mensaje. Ignora campos desconocidos (el schema los rechaza).
     *
     * @throws ProtocolException si no es JSON, no es un objeto, el tipo no existe, {@code v} no es 1, el
     *     {@code id} no es UUID o falta un campo requerido
     */
    public static Message decode(String json) {
        JsonObject obj = parseObject(json);
        JsonElement type = obj.get("type");
        if (type == null || !type.isJsonPrimitive()) {
            throw new ProtocolException("falta type");
        }
        MessageType messageType = MessageType.fromWire(type.getAsString());
        try {
            return GSON.fromJson(obj, messageType.dtoClass());
        } catch (RuntimeException e) {
            throw new ProtocolException(messageType + " inválido: " + rootMessage(e), e);
        }
    }

    private static JsonObject parseObject(String json) {
        JsonElement element;
        try {
            element = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            throw new ProtocolException("JSON inválido", e);
        }
        if (!element.isJsonObject()) {
            throw new ProtocolException("el mensaje no es un objeto JSON");
        }
        return element.getAsJsonObject();
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage();
    }
}
