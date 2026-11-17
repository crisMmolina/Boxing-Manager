package com.sigebox.juez.wait;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.room.Room;

import com.google.android.material.button.MaterialButton;
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

/** AND-031 (pulido v6): fin de evento con su única acción llena y píldoras de esquina separadas sin chip. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, qualifiers = "w390dp-h844dp-xxhdpi")
public class WaitPolishV6Test {

    private final Function<Context, JudgeConnection> original = NextFightActivity.connections;
    private SigeboxDatabase db;

    @Before
    public void setUp() throws Exception {
        Application app = RuntimeEnvironment.getApplication();
        db = Room.inMemoryDatabaseBuilder(app, SigeboxDatabase.class).allowMainThreadQueries().build();
        JudgeConnection connection = ConnectionTestAccess.newConnection(db);
        NextFightActivity.connections = context -> connection;
    }

    @After
    public void tearDown() {
        NextFightActivity.connections = original;
        db.close();
    }

    @Test
    public void finDeEventoVolverAUnirseEsLaPildoraLlena() {
        Application app = RuntimeEnvironment.getApplication();
        NextFightActivity activity = Robolectric.buildActivity(NextFightActivity.class,
                new Intent(app, NextFightActivity.class).putExtra(NextFightActivity.EXTRA_EVENT_ENDED, true))
                .setup().get();
        MaterialButton back = activity.findViewById(R.id.next_fight_back);
        assertEquals(View.VISIBLE, back.getVisibility());
        assertEquals("llena, sin contorno", 0, back.getStrokeWidth());
        float dp = activity.getResources().getDisplayMetrics().density;
        assertTrue(back.getMinHeight() >= 56 * dp - 1);
    }

    /** Con "reducir movimiento" el indicador de carga queda quieto y parece un glifo roto: se oculta. */
    @Test
    public void sinAnimacionesNoSeVeElIndicadorDeCarga() {
        Application app = RuntimeEnvironment.getApplication();
        android.provider.Settings.Global.putFloat(app.getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f);
        NextFightActivity activity = Robolectric.buildActivity(NextFightActivity.class,
                new Intent(app, NextFightActivity.class)).setup().get();
        assertEquals(View.GONE, activity.findViewById(R.id.next_fight_loading).getVisibility());
    }

    @Test
    public void sinChipLasPildorasRojaYAzulQuedanSeparadas() {
        Activity host = Robolectric.buildActivity(Activity.class).setup().get();
        Context themed = new ContextThemeWrapper(host, R.style.Theme_Sigebox);
        FrameLayout parent = new FrameLayout(themed);
        View root = LayoutInflater.from(themed).inflate(R.layout.view_fight_card, parent, false);
        parent.addView(root);
        FightCard.bind(parent, "Juan Pérez", "Luis Gómez", null);
        int w = themed.getResources().getDisplayMetrics().widthPixels;
        parent.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        parent.layout(0, 0, w, parent.getMeasuredHeight());
        View red = root.findViewById(R.id.fight_red_pill);
        View blue = root.findViewById(R.id.fight_blue_pill);
        float dp = themed.getResources().getDisplayMetrics().density;
        int gap = blue.getLeft() - red.getRight();
        assertTrue("hueco " + gap + " px", gap >= 8 * dp - 1);
    }
}
