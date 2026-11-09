package com.sigebox.central.ui.inicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Spec 005 (DSK-036): la terminación se hace en En vivo, así que los enlaces secundarios son Inicio · Bitácora.
 * Reemplaza a {@code secondaryLinksAreInicioTerminacionAndBitacora} (spec 005 §Decisions, decisión DSK-036).
 */
class NavTest {

    @Test
    void secondaryLinksAreInicioAndBitacora() {
        assertEquals(List.of("Inicio", "Bitácora"), Nav.LINKS.stream().map(i -> i.label()).toList());
    }
}
