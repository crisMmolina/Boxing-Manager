package com.sigebox.core.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;

/**
 * Codifica y decodifica mensajes del protocolo v1 en JSON. Los campos {@code null} se omiten. La lectura es
 * estricta: JSON estándar de hasta {@link #MAX_LENGTH} caracteres, sin claves repetidas, con profundidad limitada y sin conversión de tipos (ver
 * {@link StrictAdapters}). Los errores no repiten el contenido recibido.
 */
public final class Messages {

    /** Longitud máxima de un mensaje en caracteres. */
    static final int MAX_LENGTH = 16 * 1024;

    /** Profundidad máxima de objetos y arreglos anidados. */
    static final int MAX_DEPTH = 8;

    /** Longitud máxima de un número en el JSON. */
    static final int MAX_NUMBER_LENGTH = 20;

    private static final Gson GSON = StrictAdapters.gson();

    private Messages() {}

    public static String encode(Message message) {
        return GSON.toJson(message);
    }

    /**
     * Lee un mensaje. Ignora campos desconocidos (el schema los rechaza).
     *
     * @throws ProtocolException si no es JSON estándar, no es un objeto, tiene claves repetidas o demasiado
     *     anidamiento, el tipo no existe, {@code v} no es 1, el {@code id} no es UUID, un campo tiene otro tipo,
     *     falta un campo requerido o un valor sale de su rango, longitud o formato
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
            throw new ProtocolException(messageType + " inválido: " + safeReason(e));
        }
    }

    /** El motivo de {@link Fields} (solo nombres de campo y límites), nunca el contenido recibido. */
    private static String safeReason(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof IllegalArgumentException && t.getStackTrace().length > 0
                    && Fields.class.getName().equals(t.getStackTrace()[0].getClassName())) {
                return t.getMessage();
            }
        }
        return "formato";
    }

    private static JsonObject parseObject(String json) {
        if (json != null && json.length() > MAX_LENGTH) {
            throw new ProtocolException("mensaje demasiado grande");
        }
        JsonElement element;
        try {
            JsonReader reader = new JsonReader(new StringReader(json));
            reader.setStrictness(Strictness.STRICT);
            element = read(reader, 0);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new ProtocolException("datos después del mensaje");
            }
        } catch (IOException | RuntimeException e) {
            throw e instanceof ProtocolException ? (ProtocolException) e : new ProtocolException("JSON inválido");
        }
        if (!element.isJsonObject()) {
            throw new ProtocolException("el mensaje no es un objeto JSON");
        }
        return element.getAsJsonObject();
    }

    private static JsonElement read(JsonReader in, int depth) throws IOException {
        switch (in.peek()) {
            case BEGIN_OBJECT:
                checkDepth(depth);
                JsonObject obj = new JsonObject();
                in.beginObject();
                while (in.hasNext()) {
                    String name = in.nextName();
                    if (obj.has(name)) {
                        throw new ProtocolException("clave repetida");
                    }
                    obj.add(name, read(in, depth + 1));
                }
                in.endObject();
                return obj;
            case BEGIN_ARRAY:
                checkDepth(depth);
                JsonArray array = new JsonArray();
                in.beginArray();
                while (in.hasNext()) {
                    array.add(read(in, depth + 1));
                }
                in.endArray();
                return array;
            case STRING:
                return new JsonPrimitive(in.nextString());
            case NUMBER:
                String number = in.nextString();
                if (number.length() > MAX_NUMBER_LENGTH) {
                    throw new ProtocolException("número demasiado largo");
                }
                return new JsonPrimitive(new BigDecimal(number));
            case BOOLEAN:
                return new JsonPrimitive(in.nextBoolean());
            case NULL:
                in.nextNull();
                return JsonNull.INSTANCE;
            default:
                throw new ProtocolException("JSON inválido");
        }
    }

    private static void checkDepth(int depth) {
        if (depth >= MAX_DEPTH) {
            throw new ProtocolException("demasiado anidamiento");
        }
    }
}
