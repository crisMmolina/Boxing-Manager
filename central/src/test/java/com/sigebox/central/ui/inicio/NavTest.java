package com.sigebox.central.ui.inicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Sin Corrección (spec 004): los enlaces secundarios son Inicio · Terminación · Bitácora. */
class NavTest {

    @Test
    void secondaryLinksAreInicioTerminacionAndBitacora() {
        assertEquals(List.of("Inicio", "Terminación", "Bitácora"), Nav.LINKS.stream().map(i -> i.label()).toList());
    }
}
