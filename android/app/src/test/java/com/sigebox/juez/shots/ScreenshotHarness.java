package com.sigebox.juez.shots;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.EstadoRound;
import com.sigebox.core.model.EstadoTarjeta;
import com.sigebox.core.protocol.Ack;
import com.sigebox.core.protocol.Message;
import com.sigebox.core.protocol.State;
import com.sigebox.juez.R;
import com.sigebox.juez.data.OutboxEntity;
import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.data.SigeboxDatabase;
import com.sigebox.juez.join.JoinActivity;
import com.sigebox.juez.late.LateScoreActivity;
import com.sigebox.juez.net.ConnectionTestAccess;
import com.sigebox.juez.net.DeviceSigner;
import com.sigebox.juez.net.JudgeConnection;
import com.sigebox.juez.net.JudgeRuntime;
import com.sigebox.juez.score.ScoreActivity;
import com.sigebox.juez.terms.TermsActivity;
import com.sigebox.juez.terms.TermsStore;
import com.sigebox.juez.wait.NextFightActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.robolectric.Shadows.shadowOf;

/**
 * AND-026 (spec 007): capturas de cada pantalla del juez en claro y oscuro, a 390 × 844 dp, con estado falso.
 * No corre en CI: solo con {@code SIGEBOX_SCREENSHOTS=<carpeta>}. Deja {@code <pantalla>-claro.png} y
 * {@code <pantalla>-oscuro.png}.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 36, qualifiers = "w390dp-h844dp-port-xxhdpi")
public class ScreenshotHarness {

    private static final String DIR = System.getenv("SIGEBOX_SCREENSHOTS");

    private SigeboxDatabase db;
    private JudgeConnection connection;

    @Before
    public void setUp() throws Exception {
        assumeTrue("solo con SIGEBOX_SCREENSHOTS", DIR != null && !DIR.isEmpty());
        Context app = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(app, SigeboxDatabase.class).allowMainThreadQueries().build();
        DeviceSigner signer = ReflectionHelpers.callConstructor(
                Class.forName("com.sigebox.juez.net.JudgeSessionTest$TestSigner").asSubclass(DeviceSigner.class));
        JudgeRuntime runtime = ReflectionHelpers.callConstructor(JudgeRuntime.class,
                ClassParameter.from(Context.class, app),
                ClassParameter.from(SigeboxDatabase.class, db),
                ClassParameter.from(DeviceSigner.class, signer));
        ReflectionHelpers.setStaticField(JudgeRuntime.class, "instance", runtime);
        connection = runtime.connection();
        freshSession();
    }

    /** Base vacía con la sesión aprobada de María López (juez 2, pelea F1). */
    private void freshSession() {
        db.clearAllTables();
        SessionEntity session = new SessionEntity();
        session.host = "192.168.43.1";
        session.port = 8765;
        session.fightId = "F1";
        session.seat = 2;
        session.token = "482913";
        session.judgeName = "María López";
        session.approved = true;
        db.sessionDao().save(session);
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(JudgeRuntime.class, "instance", null);
        if (db != null) {
            db.close();
        }
    }

    @Test
    public void capturaCadaPantallaEnClaroYOscuro() throws Exception {
        Context app = ApplicationProvider.getApplicationContext();
        android.provider.Settings.Global.putFloat(app.getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f); // estado final, sin entradas a medias
        new TermsStore(app).accept();
        StringBuilder failed = new StringBuilder();
        for (String theme : new String[] {"claro", "oscuro"}) {
            RuntimeEnvironment.setQualifiers(theme.equals("oscuro") ? "+night" : "+notnight");
            freshSession();
            capture("terminos-" + theme, () -> start(TermsActivity.class, new Intent()), failed);
            capture("unirse-" + theme, () -> start(JoinActivity.class, new Intent()), failed);
            // WaitingActivity lee la llave del AndroidKeyStore, que Robolectric no tiene: queda fuera del arnés.
            capture("puntuar-" + theme, this::score, failed);
            capture("puntuar-puntos-" + theme, this::scored, failed);
            capture("enviado-" + theme, this::sent, failed);
            freshSession();
            capture("tardio-" + theme, () -> start(LateScoreActivity.class, new Intent()
                    .putExtra(ScoreActivity.EXTRA_ROUND, 3)), failed);
            freshSession();
            capture("tardio-puntos-" + theme, this::lateScored, failed);
            capture("siguiente-" + theme, () -> start(NextFightActivity.class, new Intent()
                    .putExtra(NextFightActivity.EXTRA_JUDGE_NAME, "María López")), failed);
            capture("fin-evento-" + theme, () -> start(NextFightActivity.class, new Intent()
                    .putExtra(NextFightActivity.EXTRA_EVENT_ENDED, true)
                    .putExtra(NextFightActivity.EXTRA_JUDGE_NAME, "María López")), failed);
        }
        System.out.println("capturas fallidas: " + (failed.length() == 0 ? "ninguna" : failed));
        assertTrue(new File(DIR, "puntuar-claro.png").length() > 0);
    }

    private interface Screen {
        Activity open() throws Exception;
    }

    private static void capture(String name, Screen screen, StringBuilder failed) {
        try {
            shot(name, screen.open());
        } catch (Exception | Error e) {
            failed.append(name).append(" (").append(e).append(") ");
        }
    }

    /** Round 3 de 12 abierto, sin calificar. */
    private Activity score() {
        Activity activity = start(ScoreActivity.class, new Intent());
        dispatch(state(EstadoRound.ABIERTO, 3));
        return activity;
    }

    /** AND-031: Round 3 calificado 10–9, aún sin enviar: el número grande y la píldora dorada habilitada. */
    private Activity scored() {
        Activity activity = score();
        activity.findViewById(R.id.score_red_card).findViewById(R.id.corner_plus).performClick();
        activity.findViewById(R.id.score_blue_card).findViewById(R.id.corner_minus).performClick();
        settle();
        return activity;
    }

    /** AND-031: Round 3 tardío calificado 10–9, aún sin enviar. */
    private Activity lateScored() {
        Activity activity = start(LateScoreActivity.class, new Intent().putExtra(ScoreActivity.EXTRA_ROUND, 3));
        activity.findViewById(R.id.late_red_card).findViewById(R.id.corner_plus).performClick();
        activity.findViewById(R.id.late_blue_card).findViewById(R.id.corner_minus).performClick();
        settle();
        return activity;
    }

    /** Round 3 calificado 10–9 y confirmado por la central. */
    private Activity sent() {
        Activity activity = score();
        activity.findViewById(R.id.score_red_card).findViewById(R.id.corner_plus).performClick();
        activity.findViewById(R.id.score_blue_card).findViewById(R.id.corner_minus).performClick();
        settle();
        activity.findViewById(R.id.score_send).performClick();
        settle();
        // La hoja de confirmación es un diálogo: su botón principal envía.
        android.app.Dialog sheet = ShadowDialog.getLatestDialog();
        if (sheet != null) {
            sheet.findViewById(R.id.confirm_send).performClick();
            settle();
        }
        for (OutboxEntity pending : db.outboxDao().unacked()) {
            dispatch(new Ack(pending.messageId, true, EstadoTarjeta.ENVIADA, null));
        }
        return activity;
    }

    private static State state(EstadoRound round, int number) {
        return new State("F1", 2, "María López", "Roja", "Azul", 12, EstadoPelea.EN_CURSO, number, round,
                Collections.emptyList(), false);
    }

    private void dispatch(Message message) {
        ConnectionTestAccess.dispatch(connection, message);
        settle();
    }

    private <T extends Activity> Activity start(Class<T> type, Intent extras) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), type).putExtras(extras);
        Activity activity = Robolectric.buildActivity(type, intent).setup().get();
        settle();
        return activity;
    }

    /** Deja correr el hilo de E/S de la pantalla (lee la sesión) y el hilo principal. */
    private static void settle() {
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);
        }
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
