package com.sigebox.juez.wait;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
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
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.function.Function;

/**
 * AND-030: capturas de Siguiente pelea (la actividad real, con NEXT_FIGHT recibido) y de Esperando (su layout con
 * la tarjeta de pelea). Solo corre con {@code SIGEBOX_SCREENSHOTS}; en CI se salta.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class FlowShotsV6Test {

    private static final String DIR = System.getenv("SIGEBOX_SCREENSHOTS");
    private final Function<Context, JudgeConnection> original = NextFightActivity.connections;
    private SigeboxDatabase db;
    private JudgeConnection connection;

    @Before
    public void setUp() throws Exception {
        assumeTrue("sin SIGEBOX_SCREENSHOTS", DIR != null && !DIR.isEmpty());
        Application app = RuntimeEnvironment.getApplication();
        // Como el arnés general: estado final sin animaciones (reducir movimiento).
        android.provider.Settings.Global.putFloat(app.getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f);
        db = Room.inMemoryDatabaseBuilder(app, SigeboxDatabase.class).allowMainThreadQueries().build();
        connection = ConnectionTestAccess.newConnection(db);
        NextFightActivity.connections = context -> connection;
    }

    @After
    public void tearDown() {
        NextFightActivity.connections = original;
        if (db != null) {
            db.close();
        }
    }

    @Test
    @Config(sdk = 36, qualifiers = "w390dp-h844dp-port-notnight-xxhdpi")
    public void claro() throws Exception {
        shots("claro");
    }

    @Test
    @Config(sdk = 36, qualifiers = "w390dp-h844dp-port-night-xxhdpi")
    public void oscuro() throws Exception {
        shots("oscuro");
    }

    private void shots(String theme) throws Exception {
        Application app = RuntimeEnvironment.getApplication();
        NextFightActivity next = Robolectric.buildActivity(NextFightActivity.class,
                new Intent(app, NextFightActivity.class)).setup().get();
        ConnectionTestAccess.dispatch(connection,
                new NextFight("F1", "F2", null, "Juan Pérez", "Luis Gómez", 3));
        shadowOf(Looper.getMainLooper()).idle();
        save(next, "siguiente-tarjeta-" + theme + ".png");

        Activity waiting = Robolectric.buildActivity(Activity.class).create().get();
        waiting.setTheme(R.style.Theme_Sigebox);
        waiting.setContentView(R.layout.activity_waiting);
        ((TextView) waiting.findViewById(R.id.waiting_seat)).setText("Juez 2 · María López");
        ((TextView) waiting.findViewById(R.id.waiting_fingerprint)).setText("4F2A · 9C1D");
        FightCard.bind(waiting.getWindow().getDecorView(), "Juan Pérez", "Luis Gómez", null);
        save(waiting, "esperando-tarjeta-" + theme + ".png");
    }

    private static void save(Activity activity, String name) throws Exception {
        View root = activity.getWindow().getDecorView();
        int w = activity.getResources().getDisplayMetrics().widthPixels;
        int h = activity.getResources().getDisplayMetrics().heightPixels;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        File out = new File(DIR, name);
        out.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        assertTrue(out.length() > 0);
    }
}
