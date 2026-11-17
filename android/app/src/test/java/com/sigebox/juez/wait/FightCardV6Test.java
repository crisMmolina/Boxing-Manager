package com.sigebox.juez.wait;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.room.Room;

import com.sigebox.core.protocol.NextFight;
import com.sigebox.juez.R;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.net.ConnectionTestAccess;
import com.sigebox.juez.net.JudgeConnection;
import com.sigebox.juez.ui.FightCard;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.function.Function;

/**
 * AND-030 (spec 007, v6 «Esquina»): Esperando y Siguiente pelea muestran la pelea como una tarjeta con las dos
 * esquinas (píldora roja y azul) y el número de rounds en un chip.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class FightCardV6Test {

    private final Function<Context, JudgeConnection> original = NextFightActivity.connections;
    private Application app;
    private SigeboxDatabase db;
    private JudgeConnection connection;

    @Before
    public void setUp() throws Exception {
        app = RuntimeEnvironment.getApplication();
        db = Room.inMemoryDatabaseBuilder(app, SigeboxDatabase.class).allowMainThreadQueries().build();
        connection = ConnectionTestAccess.newConnection(db);
        NextFightActivity.connections = context -> connection;
    }

    @After
    public void tearDown() {
        NextFightActivity.connections = original;
        db.close();
    }

    private static String text(View root, int id) {
        TextView view = root.findViewById(id);
        assertEquals(View.VISIBLE, view.getVisibility());
        return view.getText().toString();
    }

    @Test
    public void siguientePeleaSinDatosNoMuestraLaTarjeta() {
        NextFightActivity activity = Robolectric.buildActivity(NextFightActivity.class,
                new Intent(app, NextFightActivity.class)).setup().get();
        assertEquals(View.GONE, activity.findViewById(R.id.fight_card).getVisibility());
    }

    @Test
    public void siguientePeleaMuestraLasDosEsquinasYElChipDeRounds() {
        NextFightActivity activity = Robolectric.buildActivity(NextFightActivity.class,
                new Intent(app, NextFightActivity.class)).setup().get();
        ConnectionTestAccess.dispatch(connection, new NextFight("F1", "F2", null, "Rojo", "Azul", 3));
        shadowOf(Looper.getMainLooper()).idle();

        View card = activity.findViewById(R.id.fight_card);
        assertEquals(View.VISIBLE, card.getVisibility());
        assertEquals("Roja", text(card, R.id.fight_red_pill));
        assertEquals("Azul", text(card, R.id.fight_blue_pill));
        assertEquals("3 rounds", text(card, R.id.fight_rounds));
        assertEquals("Rojo vs Azul", text(activity.getWindow().getDecorView(), R.id.next_fight_corners));
    }

    @Test
    public void esperandoTieneLaTarjetaYSinRoundsOcultaElChip() {
        Activity host = Robolectric.buildActivity(Activity.class).setup().get();
        Context themed = new ContextThemeWrapper(host, R.style.Theme_Sigebox);
        FrameLayout parent = new FrameLayout(themed);
        View root = LayoutInflater.from(themed).inflate(R.layout.activity_waiting, parent, false);
        parent.addView(root);

        View card = root.findViewById(R.id.fight_card);
        assertEquals(View.GONE, card.getVisibility());
        FightCard.bind(root, "Rojo", "Azul", null);
        assertEquals(View.VISIBLE, card.getVisibility());
        assertEquals("Rojo vs Azul", text(root, R.id.fight_names));
        assertEquals(View.GONE, root.findViewById(R.id.fight_rounds).getVisibility());
        FightCard.bind(root, "Rojo", "Azul", 1);
        assertEquals("1 round", text(root, R.id.fight_rounds));
    }
}
