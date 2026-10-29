package com.sigebox.central.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Registro de pantallas de la central. Cada tarea agrega la suya con {@link #register}; la
 * ventana muestra una a la vez con {@link #show}. No toca JavaFX hasta que se construye un nodo,
 * así que se puede probar sin arrancar el toolkit.
 *
 * @param <N> tipo de nodo de la UI (en la app, {@code javafx.scene.Node})
 */
public final class Screens<N> {

    /** Pantalla registrada: identificador estable, título para la navegación y fábrica del nodo. */
    public record Screen<N>(String id, String title, Supplier<? extends N> factory) {
        public Screen {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(factory, "factory");
            if (id.isBlank()) {
                throw new IllegalArgumentException("id vacío");
            }
        }
    }

    private final Map<String, Screen<N>> screens = new LinkedHashMap<>();
    private final List<Consumer<Screen<N>>> listeners = new ArrayList<>();
    private Screen<N> current;

    /** Registra una pantalla. Falla si el id ya existe. */
    public Screens<N> register(String id, String title, Supplier<? extends N> factory) {
        var screen = new Screen<N>(id, title, factory);
        if (screens.putIfAbsent(id, screen) != null) {
            throw new IllegalArgumentException("pantalla duplicada: " + id);
        }
        return this;
    }

    /** Pantallas en orden de registro. */
    public List<Screen<N>> all() {
        return Collections.unmodifiableList(new ArrayList<>(screens.values()));
    }

    /** Cambia a la pantalla {@code id}, construye su nodo y avisa a los oyentes. */
    public N show(String id) {
        var screen = screens.get(id);
        if (screen == null) {
            throw new IllegalArgumentException("pantalla desconocida: " + id);
        }
        current = screen;
        listeners.forEach(l -> l.accept(screen));
        return screen.factory().get();
    }

    /** Id de la pantalla visible, o {@code null} si todavía no se mostró ninguna. */
    public String currentId() {
        return current == null ? null : current.id();
    }

    /** Avisa cada vez que cambia la pantalla visible. */
    public void onChange(Consumer<Screen<N>> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }
}
