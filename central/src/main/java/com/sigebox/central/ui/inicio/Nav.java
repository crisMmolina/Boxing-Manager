package com.sigebox.central.ui.inicio;

import com.sigebox.central.ui.funcion.FuncionScreen;
import com.sigebox.central.ui.log.LogScreen;
import com.sigebox.central.ui.result.ResultStepScreen;
import com.sigebox.central.ui.rounds.RoundsScreen;
import com.sigebox.central.ui.terms.TermsScreen;
import com.sigebox.central.ui.theme.StepsBar;
import java.util.List;
import java.util.Set;

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
    public static void apply(StepsBar bar, String screenId) {
        boolean in = inEvent(screenId);
        bar.showEvent(in, in || InicioScreen.ID.equals(screenId) ? null : InicioScreen.ID);
        bar.select(screenId);
    }

    private Nav() {
    }
}
