package com.sigebox.juez.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.protocol.JoinApproved;
import com.sigebox.core.protocol.Ping;
import com.sigebox.core.protocol.State;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** DESIGN.md v3 §5.2: la tarjeta de esquina lleva el nombre del boxeador aunque la pantalla abra después del mensaje. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class CornerNamesTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void recuerdaLosNombresDelJoinApprovedPorPelea() {
        CornerNames names = new CornerNames(context);
        assertTrue(names.remember(new JoinApproved("F1", 2, "María López", "M. Hernández", "J. Ortega", 4)));
        CornerNames reopened = new CornerNames(context);
        assertEquals("M. Hernández", reopened.red("F1"));
        assertEquals("J. Ortega", reopened.blue("F1"));
        assertNull("otra pelea no hereda nombres", reopened.red("F2"));
    }

    @Test
    public void elStateActualizaLosNombres() {
        CornerNames names = new CornerNames(context);
        names.remember(new State("F1", 2, "María López", "Roja Uno", "Azul Uno", 4, EstadoPelea.EN_CURSO, 1,
                EstadoRound.ABIERTO, List.of(), false));
        assertEquals("Roja Uno", names.red("F1"));
        assertFalse("otros mensajes no cambian nada", names.remember(new Ping(1L)));
    }
}
