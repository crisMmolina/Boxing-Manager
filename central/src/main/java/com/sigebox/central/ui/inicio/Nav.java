package com.sigebox.central.ui.inicio;

import com.sigebox.central.ui.correction.CorrectionScreen;
import com.sigebox.central.ui.funcion.FuncionScreen;
import com.sigebox.central.ui.log.LogScreen;
import com.sigebox.central.ui.result.ResultStepScreen;
import com.sigebox.central.ui.rounds.RoundsScreen;
import com.sigebox.central.ui.termination.TerminationScreen;
import com.sigebox.central.ui.theme.StepsBar;
import java.util.List;

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
            new StepsBar.Item(TerminationScreen.ID, "Terminación"),
            new StepsBar.Item(CorrectionScreen.ID, "Corrección"),
            new StepsBar.Item(LogScreen.ID, "Bitácora"));

    private Nav() {
    }
}
