package com.sigebox.central;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sigebox.central.ui.Screens;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pruebas sin toolkit JavaFX: corren en CI sin pantalla. */
class SmokeTest {

    @Test
    void titleMatchesSpec() {
        assertEquals("SIGEBOX · Central", App.TITLE);
    }

    @Test
    void appStylesheetIsPackaged() {
        assertNotNull(App.class.getResource("app.css"));
    }

    @Test
    void screensKeepRegistrationOrderAndSwitch() {
        var changes = new ArrayList<String>();
        var screens = new Screens<String>()
                .register("inicio", "Inicio", () -> "nodo-inicio")
                .register("rounds", "Rounds", () -> "nodo-rounds");
        screens.onChange(s -> changes.add(s.id()));

        assertNull(screens.currentId());
        assertEquals(List.of("inicio", "rounds"), screens.all().stream().map(Screens.Screen::id).toList());
        assertEquals("nodo-rounds", screens.show("rounds"));
        assertEquals("rounds", screens.currentId());
        assertEquals(List.of("rounds"), changes);
    }

    @Test
    void screensRejectDuplicateAndUnknownIds() {
        var screens = new Screens<String>().register("inicio", "Inicio", () -> "x");

        assertThrows(IllegalArgumentException.class, () -> screens.register("inicio", "Otra", () -> "y"));
        assertThrows(IllegalArgumentException.class, () -> screens.show("nada"));
        assertThrows(IllegalArgumentException.class, () -> screens.register(" ", "Vacía", () -> "z"));
    }

    @Test
    void coreIsOnTheClasspath() throws Exception {
        assertNotNull(Class.forName("com.google.gson.Gson"));
    }
}
