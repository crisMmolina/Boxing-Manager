package com.sigebox.core.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/** Validación de campos comunes a los records del protocolo. Solo APIs de Android 8 (API 26). */
final class Fields {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private Fields() {}

    static String newId() {
        return UUID.randomUUID().toString();
    }

    static void base(int v, String id, String type, String expectedType) {
        if (v != Message.VERSION) {
            throw new IllegalArgumentException("v no soportada: " + v);
        }
        if (id == null || !UUID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("id no es un UUID: " + id);
        }
        if (!expectedType.equals(type)) {
            throw new IllegalArgumentException("type " + type + " no es " + expectedType);
        }
    }

    /** Copia inmutable; {@code List.copyOf} no existe en Android 8. */
    static <T> List<T> copy(List<T> list) {
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException("falta el campo " + name);
        }
        return value;
    }
}
