package com.sigebox.juez.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.protocol.State;
import com.sigebox.juez.R;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.net.ConnectionTestAccess;
import com.sigebox.juez.net.JudgeConnection;
import com.sigebox.juez.terms.TermsActivity;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** DESIGN.md v4 §5.6: la pantalla de puntuar muestra la forma de las tarjetas hasta el primer STATE. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class StateSkeletonTest {

    private final Function<Context, JudgeConnection> realConnections = StateSkeleton.connections;
    private SigeboxDatabase db;
    private JudgeConnection connection;

    @Before
    public void setUp() throws Exception {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SigeboxDatabase.class)
                .allowMainThreadQueries()
                .build();
        connection = ConnectionTestAccess.newConnection(db);
        StateSkeleton.connections = context -> connection;
    }

    @After
    public void tearDown() {
        StateSkeleton.connections = realConnections;
        db.close();
    }

    private static State state() {
        return new State("F1", 2, "María López", "Roja", "Azul", 4,
                EstadoPelea.EN_CURSO, 3, EstadoRound.ABIERTO, List.of(), false);
    }

    private static View screen() {
        TermsActivity activity = Robolectric.buildActivity(TermsActivity.class).setup().get();
        View root = LayoutInflater.from(activity).inflate(R.layout.activity_score, null);
        activity.setContentView(root);
        return root;
    }

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static void assertShowsSkeleton(View root, boolean skeleton) {
        assertEquals(skeleton ? View.VISIBLE : View.GONE, root.findViewById(R.id.score_skeleton).getVisibility());
        assertEquals(skeleton ? View.GONE : View.VISIBLE, root.findViewById(R.id.score_content).getVisibility());
    }

    @Test
    public void antesDelPrimerStateSeVeLaFormaDeLaTarjeta() {
        View root = screen();
        idle();
        assertShowsSkeleton(root, true);
        float radius = root.getResources().getDimension(R.dimen.card_radius);
        for (int id : new int[] {R.id.skeleton_card_red, R.id.skeleton_card_blue}) {
            GradientDrawable block = (GradientDrawable) root.findViewById(id).getBackground();
            assertEquals(radius, block.getCornerRadius(), 0f);
        }
    }

    @Test
    public void seAnunciaCargandoUnaSolaVez() {
        View root = screen();
        View skeleton = root.findViewById(R.id.score_skeleton);
        assertEquals("Cargando", skeleton.getContentDescription().toString());
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, skeleton.getAccessibilityLiveRegion());
        assertTrue(skeleton.isFocusable());
    }

    @Test
    public void elStateDesdeElHiloDeLaConexionMuestraLaPantalla() throws Exception {
        View root = screen();
        idle();
        Thread network = new Thread(() -> ConnectionTestAccess.dispatch(connection, state()));
        network.start();
        network.join();
        idle();
        assertShowsSkeleton(root, false);
    }

    @Test
    public void unStateQueYaLlegoNoDejaElEsqueleto() {
        ConnectionTestAccess.dispatch(connection, state());
        View root = screen();
        idle();
        assertShowsSkeleton(root, false);
    }

    @Test
    public void aLosTresSegundosAvisaQueTarda() {
        View root = screen();
        View slow = root.findViewById(R.id.skeleton_slow);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(StateSkeleton.SLOW_AFTER_MS - 1));
        assertEquals(View.GONE, slow.getVisibility());
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1));
        assertEquals(View.VISIBLE, slow.getVisibility());
    }

    @Test
    public void conMovimientoReducidoLosBloquesQuedanFijos() {
        StateSkeleton moving = screen().findViewById(R.id.score_skeleton);
        assertTrue(moving.isShining());
        Settings.Global.putFloat(ApplicationProvider.getApplicationContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 0f);
        StateSkeleton still = screen().findViewById(R.id.score_skeleton);
        assertFalse(still.isShining());
    }
}
