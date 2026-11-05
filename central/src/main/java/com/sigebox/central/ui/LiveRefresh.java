package com.sigebox.central.ui;

import com.sigebox.central.server.ServerEvents;
import com.sigebox.central.ui.theme.Skeleton;
import com.sigebox.core.model.TarjetaRound;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * Mantiene una sección al día sin botón "Actualizar": lee sus datos cada segundo mientras está en escena, y al
 * momento con {@link #EVENTS}. Solo redibuja si el dato cambió, así no se pierde el foco ni lo que se escribe.
 * Mientras llega la primera lectura la sección muestra su {@link Skeleton}; una lectura fallida conserva lo que
 * hay en pantalla y se reintenta en el siguiente segundo.
 */
public final class LiveRefresh<T> {

    public static final Duration PERIOD = Duration.seconds(1);

    /** Lee fuera del hilo de JavaFX, una lectura a la vez: es lo que usa la app. */
    public static final Executor BACKGROUND = Executors.newSingleThreadExecutor(task -> {
        var thread = new Thread(task, "live-refresh");
        thread.setDaemon(true);
        return thread;
    });

    /** Lee en el hilo que pide la lectura: el primer dibujo queda listo al volver de {@link #watch}. */
    public static final Executor DIRECT = Runnable::run;

    private static final System.Logger LOG = System.getLogger(LiveRefresh.class.getName());
    private static final Set<LiveRefresh<?>> RUNNING = ConcurrentHashMap.newKeySet();

    /** Avisos del servidor: cada uno refresca al momento las secciones en escena (corren en hilos de Jetty). */
    public static final ServerEvents EVENTS = new ServerEvents() {
        @Override
        public void onJoinRequested(JoinRequest request) {
            pokeAll();
        }

        @Override
        public void onJudgeConnected(String fightId, int seat) {
            pokeAll();
        }

        @Override
        public void onJudgeDisconnected(String fightId, int seat) {
            pokeAll();
        }

        @Override
        public void onCardReceived(TarjetaRound card) {
            pokeAll();
        }

        @Override
        public void onFinalCard(String fightId, int seat) {
            pokeAll();
        }

        @Override
        public void onFinalCardRejected(String fightId, int seat) {
            pokeAll();
        }

        @Override
        public void onDescuentoChanged(String fightId) {
            pokeAll();
        }
    };

    private final Executor reads;
    private final Supplier<T> read;
    private final Consumer<T> draw;
    private final Timeline timer;
    private T last;
    private boolean drawn;
    private boolean reading;
    private boolean again;
    private boolean failing;

    private LiveRefresh(Executor reads, Supplier<T> read, Consumer<T> draw) {
        this.reads = reads;
        this.read = read;
        this.draw = draw;
        timer = new Timeline(new KeyFrame(PERIOD, e -> load()));
        timer.setCycleCount(Animation.INDEFINITE);
    }

    /**
     * Hace la primera lectura y refresca {@code root} mientras esté en escena. {@code read} corre en {@code reads};
     * {@code draw} siempre en el hilo de JavaFX y solo con un dato distinto del anterior ({@code equals}).
     */
    public static <T> LiveRefresh<T> watch(Node root, Executor reads, Supplier<T> read, Consumer<T> draw) {
        var live = new LiveRefresh<>(reads, read, draw);
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                live.stop();
            } else if (old == null) {
                live.start();
            }
        });
        if (root.getScene() != null) {
            live.start();
        }
        live.load();
        return live;
    }

    /** Pone {@code content} en {@code slot} y apaga el skeleton que hubiera; si ya está, no lo toca (conserva el foco). */
    public static void swap(Pane slot, List<? extends Node> content) {
        if (slot.getChildren().equals(content)) {
            return;
        }
        slot.getChildren().forEach(n -> {
            if (n instanceof Skeleton s) {
                s.stop();
            }
        });
        slot.getChildren().setAll(content);
    }

    /** Vuelve a leer ya, desde cualquier hilo. */
    public void poke() {
        onFx(this::load);
    }

    private static void pokeAll() {
        RUNNING.forEach(LiveRefresh::poke);
    }

    private void start() {
        RUNNING.add(this);
        timer.play();
    }

    private void stop() {
        RUNNING.remove(this);
        timer.stop();
    }

    private void load() {
        if (reading) {
            again = true; // el aviso llegó a media lectura: esa lectura ya puede ser vieja
            return;
        }
        reading = true;
        reads.execute(() -> {
            try {
                var value = read.get();
                onFx(() -> finish(value));
            } catch (RuntimeException e) {
                onFx(() -> fail(e));
            }
        });
    }

    private void finish(T value) {
        failing = false;
        if (!drawn || !Objects.equals(value, last)) {
            last = value;
            drawn = true;
            draw.accept(value);
        }
        next();
    }

    private void fail(RuntimeException e) {
        if (!failing) {
            LOG.log(System.Logger.Level.WARNING, "No se pudo refrescar la sección", e);
        }
        failing = true;
        next();
    }

    private void next() {
        reading = false;
        if (again) {
            again = false;
            load();
        }
    }

    private static void onFx(Runnable work) {
        if (Platform.isFxApplicationThread()) {
            work.run();
        } else {
            Platform.runLater(work);
        }
    }
}
