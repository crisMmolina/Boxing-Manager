package com.sigebox.core.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Validación de los records del protocolo: las mismas reglas que sus JSON Schema (requeridos, longitudes, rangos y
 * patrones), para que la central las aplique sin el validador de Schema. Solo APIs de Android 8 (API 26).
 */
final class Fields {

    static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    static final Pattern HEX64 = Pattern.compile("^[0-9a-f]{64}$");
    static final Pattern NONCE = Pattern.compile("^[A-Za-z0-9_-]{22,128}$");
    static final Pattern BASE64 = Pattern.compile("^[A-Za-z0-9+/]+={0,2}$");

    private Fields() {}

    static String newId() {
        return UUID.randomUUID().toString();
    }

    static void base(int v, String id, String type, String expectedType) {
        if (v != Message.VERSION) {
            throw new IllegalArgumentException("v no soportada");
        }
        if (id == null || !UUID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("id no es un UUID");
        }
        if (!expectedType.equals(type)) {
            throw new IllegalArgumentException("type no es " + expectedType);
        }
    }

    static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException("falta el campo " + name);
        }
        return value;
    }

    /** Copia inmutable sin elementos nulos; {@code List.copyOf} no existe en Android 8. */
    static <T> List<T> copy(List<T> list) {
        for (T item : list) {
            require(item, "elemento de lista");
        }
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    /** Texto no vacío, con longitud en rango y sin caracteres de control ni de formato (p. ej. U+202E). */
    static void text(String value, String name, int min, int max) {
        require(value, name);
        if (value.length() < min || value.length() > max) {
            throw new IllegalArgumentException(name + " fuera de longitud " + min + "–" + max);
        }
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " vacío");
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c) || Character.getType(c) == Character.FORMAT) {
                throw new IllegalArgumentException(name + " con caracteres de control");
            }
        }
    }

    static void match(String value, String name, Pattern pattern) {
        require(value, name);
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " con formato inválido");
        }
    }

    static void range(Number value, String name, long min, long max) {
        long n = require(value, name).longValue();
        if (n < min || n > max) {
            throw new IllegalArgumentException(name + " fuera de rango " + min + "–" + max);
        }
    }
}
