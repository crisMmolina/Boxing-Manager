package com.sigebox.central.ui.inicio;

import com.sigebox.central.ui.funcion.FuncionScreen;
import com.sigebox.central.ui.log.LogScreen;
import com.sigebox.central.ui.result.ResultStepScreen;
import com.sigebox.central.ui.rounds.RoundsScreen;
import com.sigebox.central.ui.terms.TermsScreen;
import com.sigebox.central.ui.theme.StepsBar.NavBar;
import com.sigebox.central.ui.theme.StepsBar;
import com.sigebox.central.service.FuncionService;
import com.sigebox.central.ui.theme.Motion;
import java.util.List;
import java.util.Set;
import javafx.animation.Animation;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/** Barra superior de la central (DESIGN v4 §5.7). Ningún id se repite, así nunca hay dos activos. */
public final class Nav {

    /** El paso "Jueces" muestra la pantalla de emparejar de la pelea activa. */
    public static final String JUECES = "jueces";

    /** Pasos al centro, en orden. */
    public static final List<StepsBar.Item> STEPS = List.of(
            new StepsBar.Item(FuncionScreen.ID, "Evento"),
            new StepsBar.Item(JUECES, "Jueces"),
            new StepsBar.Item(RoundsScreen.ID, "En vivo"),
            new StepsBar.Item(ResultStepScreen.ID, "Resultado"));

    /** Enlaces a la derecha: Inicio primero, para volver desde cualquier pantalla. */
    public static final List<StepsBar.Item> LINKS = List.of(
            new StepsBar.Item(InicioScreen.ID, "Inicio"),
            new StepsBar.Item(LogScreen.ID, "Bitácora"));

    /** Pantallas fuera de un evento (spec 005): Inicio, Nuevo evento, la cartelera de consulta y los términos. */
    private static final Set<String> FUERA = Set.of(InicioScreen.ID, NuevoEventoScreen.ID, CarteleraScreen.ID,
            TermsScreen.ID);

    /** {@code true} si la pantalla pertenece al evento en curso y lleva la barra del evento. */
    public static boolean inEvent(String screenId) {
        return !FUERA.contains(screenId);
    }

    /**
     * Ajusta la barra a la pantalla visible: barra del evento dentro de uno; fuera, solo "?" y tema, y el enlace
     * "Inicio" si no se está ya en Inicio.
     */
    public static void apply(NavBar bar, String screenId) {
        boolean in = inEvent(screenId);
        bar.showEvent(in, in || InicioScreen.ID.equals(screenId) ? null : InicioScreen.ID);
        bar.select(screenId);
    }

    /**
     * Avance del evento: la pelea en curso o la siguiente programada, contada desde 1 ({@code [n, total]}); con todas
     * cerradas o saltadas, {@code [total, total]}; sin peleas, {@code [0, 0]}.
     */
    public static int[] progress(List<FuncionService.Fila> cartelera) {
        int total = cartelera.size();
        for (int i = 0; i < total; i++) {
            var e = cartelera.get(i).estado();
            if (e == FuncionService.Estado.EN_CURSO) {
                return new int[] {i + 1, total};
            }
        }
        for (int i = 0; i < total; i++) {
            if (cartelera.get(i).estado() == FuncionService.Estado.PROGRAMADA) {
                return new int[] {i + 1, total};
            }
        }
        return new int[] {total, total};
    }

    /** Pasos ya recorridos: los anteriores al paso de {@code screenId}; ninguno si la pantalla no es un paso. */
    public static List<String> doneBefore(String screenId) {
        var ids = STEPS.stream().map(StepsBar.Item::id).toList();
        int i = ids.indexOf(screenId);
        return i < 0 ? List.of() : ids.subList(0, i);
    }

    /** Cambia la pantalla visible con la entrada de {@link Motion#enter} (solo fundido si se reduce el movimiento). */
    public static Animation swap(StackPane content, Node next) {
        content.getChildren().setAll(next);
        return Motion.enter(next);
    }

    private Nav() {
    }
}
