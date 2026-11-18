package com.sigebox.juez.shots;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Looper;
import android.view.View;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.net.DeviceSigner;
import com.sigebox.juez.net.JudgeRuntime;
import com.sigebox.juez.terms.TermsStore;
import com.sigebox.juez.wait.NextFightActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;
import static org.robolectric.Shadows.shadowOf;

/**
 * AND-032: captura del diálogo «¿La pelea sigue?» sobre Siguiente pelea, en claro y oscuro, a 390 × 844 dp. Como el
 * arnés de AND-026, solo corre con {@code SIGEBOX_SCREENSHOTS=<carpeta>}; deja {@code sigue-claro.png} y
 * {@code sigue-oscuro.png}.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 36, qualifiers = "w390dp-h844dp-port-xxhdpi")
public class StillThereShot {

    private static final String DIR = System.getenv("SIGEBOX_SCREENSHOTS");

    private SigeboxDatabase db;
    private JudgeRuntime runtime;

    @Before
    public void setUp() throws Exception {
        assumeTrue("solo con SIGEBOX_SCREENSHOTS", DIR != null && !DIR.isEmpty());
        Context app = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(app, SigeboxDatabase.class).allowMainThreadQueries().build();
        DeviceSigner signer = ReflectionHelpers.callConstructor(
                Class.forName("com.sigebox.juez.net.JudgeSessionTest$TestSigner").asSubclass(DeviceSigner.class));
        runtime = ReflectionHelpers.callConstructor(JudgeRuntime.class,
                ClassParameter.from(Context.class, app),
                ClassParameter.from(SigeboxDatabase.class, db),
                ClassParameter.from(DeviceSigner.class, signer));
        ReflectionHelpers.setStaticField(JudgeRuntime.class, "instance", runtime);
        SessionEntity session = new SessionEntity();
        session.host = "192.168.43.1";
        session.port = 8765;
        session.fightId = "F1";
        session.seat = 2;
        session.token = "482913";
        session.judgeName = "María López";
        session.approved = true;
        db.sessionDao().save(session);
        new TermsStore(app).accept();
        android.provider.Settings.Global.putFloat(app.getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f);
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(JudgeRuntime.class, "instance", null);
        if (db != null) {
            db.close();
        }
    }

    @Test
    public void capturaElDialogoEnClaroYOscuro() throws Exception {
        for (String theme : new String[] {"claro", "oscuro"}) {
            RuntimeEnvironment.setQualifiers(theme.equals("oscuro") ? "+night" : "+notnight");
            // Diez minutos sin mensajes de la central: la última vez fue en la época 0.
            ReflectionHelpers.setField(runtime.silence(), "lastContact", 0L);
            Intent intent = new Intent(ApplicationProvider.getApplicationContext(), NextFightActivity.class)
                    .putExtra(NextFightActivity.EXTRA_JUDGE_NAME, "María López");
            Activity activity = Robolectric.buildActivity(NextFightActivity.class, intent).setup().get();
            shadowOf(Looper.getMainLooper()).idleFor(31, TimeUnit.SECONDS);
            Dialog dialog = ShadowDialog.getLatestDialog();
            assertNotNull("a los 10 min sin la central aparece «¿La pelea sigue?»", dialog);
            shot("sigue-" + theme, activity, dialog);
            dialog.dismiss();
            activity.finish();
        }
    }

    /** La pantalla, el velo del diálogo y el diálogo centrado, como se ven en el teléfono. */
    private static void shot(String name, Activity activity, Dialog dialog) throws IOException {
        View root = activity.getWindow().getDecorView().getRootView();
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        root.draw(canvas);
        canvas.drawColor(Color.argb(82, 0, 0, 0));
        View box = dialog.getWindow().getDecorView();
        canvas.save();
        canvas.translate((root.getWidth() - box.getWidth()) / 2f, (root.getHeight() - box.getHeight()) / 2f);
        box.draw(canvas);
        canvas.restore();
        File dir = new File(DIR);
        dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}
