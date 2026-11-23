package com.sigebox.juez.join;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.juez.R;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.net.ConnectionTestAccess;
import com.sigebox.juez.net.JudgeConnection;
import com.sigebox.juez.terms.TermsStore;

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
import java.io.IOException;

/**
 * AND-037: capturas de «Unirse» con el código agrupado 3+3 (claro y oscuro) y con el aviso de nombre al sexto dígito.
 * Solo con {@code SIGEBOX_SCREENSHOTS=<carpeta>}.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 36, qualifiers = "w390dp-h844dp-port-xxhdpi")
public class JoinCodeShot {

    private static final String DIR = System.getenv("SIGEBOX_SCREENSHOTS");

    private SigeboxDatabase db;
    private JudgeConnection connection;

    @Before
    public void setUp() throws Exception {
        assumeTrue("solo con SIGEBOX_SCREENSHOTS", DIR != null && !DIR.isEmpty());
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SigeboxDatabase.class)
                .allowMainThreadQueries()
                .build();
        connection = ConnectionTestAccess.newConnection(db);
        JoinActivity.connections = context -> connection;
        new TermsStore(ApplicationProvider.getApplicationContext()).accept();
    }

    @After
    public void tearDown() {
        JoinActivity.connections = context -> com.sigebox.juez.net.JudgeRuntime.get(context).connection();
        if (connection != null) {
            connection.stop();
        }
        if (db != null) {
            db.close();
        }
    }

    @Test
    public void codigoAgrupadoEnClaroYOscuroYAvisoDeNombre() throws Exception {
        for (String theme : new String[] {"claro", "oscuro"}) {
            RuntimeEnvironment.setQualifiers(theme.equals("oscuro") ? "+night" : "+notnight");
            Activity grouped = open();
            fill(grouped, R.id.join_name, "María López");
            fill(grouped, R.id.join_host, "192.168.43.1");
            fill(grouped, R.id.join_code, "48291");
            shot("unirse-codigo-" + theme, grouped);

            Activity noName = open();
            fill(noName, R.id.join_host, "192.168.43.1");
            fill(noName, R.id.join_code, "482913");
            shot("unirse-sin-nombre-" + theme, noName);
        }
        assertTrue(new File(DIR, "unirse-codigo-claro.png").length() > 0);
    }

    private static Activity open() {
        Activity activity = Robolectric.buildActivity(JoinActivity.class).setup().get();
        idle();
        return activity;
    }

    private static void fill(Activity activity, int id, String value) {
        EditText field = activity.findViewById(id);
        field.setText(value);
        idle();
    }

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static void shot(String name, Activity activity) throws IOException {
        View root = activity.getWindow().getDecorView().getRootView();
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        File dir = new File(DIR);
        dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}
