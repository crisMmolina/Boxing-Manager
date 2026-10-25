package com.sigebox.core.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

/**
 * Adaptadores de Gson que no convierten tipos: {@code "10"} no es un entero, {@code "yes"} no es un booleano y
 * un nombre de enum desconocido es un error, no {@code null}. JSON {@code null} sigue siendo {@code null}.
 */
final class StrictAdapters {

    private StrictAdapters() {}

    static Gson gson() {
        TypeAdapter<Integer> integer = new Strict<Integer>(JsonToken.NUMBER) {
            @Override
            Integer parse(JsonReader in) throws IOException {
                return Integer.valueOf(in.nextString());
            }
        }.nullSafe();
        TypeAdapter<Long> longValue = new Strict<Long>(JsonToken.NUMBER) {
            @Override
            Long parse(JsonReader in) throws IOException {
                return Long.valueOf(in.nextString());
            }
        }.nullSafe();
        TypeAdapter<Boolean> bool = new Strict<Boolean>(JsonToken.BOOLEAN) {
            @Override
            Boolean parse(JsonReader in) throws IOException {
                return in.nextBoolean();
            }
        }.nullSafe();
        TypeAdapter<String> string = new Strict<String>(JsonToken.STRING) {
            @Override
            String parse(JsonReader in) throws IOException {
                return in.nextString();
            }
        }.nullSafe();
        return new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeAdapter(Integer.class, integer)
                .registerTypeAdapter(int.class, integer)
                .registerTypeAdapter(Long.class, longValue)
                .registerTypeAdapter(long.class, longValue)
                .registerTypeAdapter(Boolean.class, bool)
                .registerTypeAdapter(boolean.class, bool)
                .registerTypeAdapter(String.class, string)
                .registerTypeAdapterFactory(new EnumFactory())
                .create();
    }

    /** Lee solo el token esperado; {@code parse} recibe el lector posicionado en él. */
    private abstract static class Strict<T> extends TypeAdapter<T> {

        private final JsonToken expected;

        Strict(JsonToken expected) {
            this.expected = expected;
        }

        abstract T parse(JsonReader in) throws IOException;

        @Override
        public T read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            if (token != expected) {
                throw new JsonSyntaxException("se esperaba " + expected + " y llegó " + token + " en " + in.getPath());
            }
            try {
                return parse(in);
            } catch (NumberFormatException e) {
                throw new JsonSyntaxException("número no entero en " + in.getPath(), e);
            }
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            if (value instanceof Number) {
                out.value((Number) value);
            } else if (value instanceof Boolean) {
                out.value((Boolean) value);
            } else {
                out.value((String) value);
            }
        }
    }

    private static final class EnumFactory implements TypeAdapterFactory {

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            Class<? super T> raw = type.getRawType();
            if (!Enum.class.isAssignableFrom(raw) || raw == Enum.class) {
                return null;
            }
            return (TypeAdapter<T>) new EnumAdapter(raw).nullSafe();
        }
    }

    private static final class EnumAdapter<E extends Enum<E>> extends TypeAdapter<E> {

        private final Class<E> type;

        EnumAdapter(Class<E> type) {
            this.type = type;
        }

        @Override
        public E read(JsonReader in) throws IOException {
            if (in.peek() != JsonToken.STRING) {
                throw new JsonSyntaxException("se esperaba un nombre de " + type.getSimpleName() + " en " + in.getPath());
            }
            String name = in.nextString();
            try {
                return Enum.valueOf(type, name);
            } catch (IllegalArgumentException e) {
                throw new JsonSyntaxException(type.getSimpleName() + " desconocido: " + name, e);
            }
        }

        @Override
        public void write(JsonWriter out, E value) throws IOException {
            out.value(value.name());
        }
    }
}
