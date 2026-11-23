package com.sigebox.juez.shots;

import static org.junit.Assume.assumeTrue;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import com.sigebox.juez.R;
import com.sigebox.juez.ui.ConfirmSheet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;

/**
 * AND-038: captura de la hoja «Mantén para enviar», en reposo y a media pulsación, en claro y oscuro, a 390 × 844 dp.
 * Solo corre con {@code SIGEBOX_SCREENSHOTS=<carpeta>}; deja {@code envia-*.png}.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 36, qualifiers = "w390dp-h844dp-port-xxhdpi")
public class ConfirmSheetShot {

    private static final String DIR = System.getenv("SIGEBOX_SCREENSHOTS");

    @Before
    public void setUp() {
        assumeTrue("solo con SIGEBOX_SCREENSHOTS", DIR != null && !DIR.isEmpty());
    }

    // Cada tema en su propia prueba: cambiar de tema a mitad de una prueba deja colores en caché en Robolectric.
    @Test
    public void claro() throws Exception {
        capture("envia-claro", false);
    }

    @Test
    @Config(qualifiers = "+night")
    public void oscuro() throws Exception {
        capture("envia-oscuro", false);
    }

    @Test
    public void manteniendoClaro() throws Exception {
        capture("envia-manteniendo-claro", true);
    }

    @Test
    @Config(qualifiers = "+night")
    public void manteniendoOscuro() throws Exception {
        capture("envia-manteniendo-oscuro", true);
    }

    private void capture(String name, boolean holding) throws Exception {
        ActivityController<Activity> c = Robolectric.buildActivity(Activity.class);
        Activity activity = c.get();
        activity.setTheme(R.style.Theme_Sigebox);
        c.setup();
        Settings.Global.putFloat(activity.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE,
                holding ? 1f : 0f);
        Dialog dialog = ConfirmSheet.show(activity, 3, 10, 9, () -> { });
        ShadowLooper.idleMainLooper();
        if (holding) {
            View button = dialog.findViewById(R.id.confirm_send);
            long now = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 10f, 10f, 0);
            button.dispatchTouchEvent(down);
            down.recycle();
            ShadowLooper.idleMainLooper(400, TimeUnit.MILLISECONDS);
        }
        shot(name, activity, dialog);
        dialog.dismiss();
        activity.finish();
    }

    /** La pantalla, el velo y la hoja pegada abajo, como se ven en el teléfono. */
    private static void shot(String name, Activity activity, Dialog dialog) throws IOException {
        View root = activity.getWindow().getDecorView().getRootView();
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        root.draw(canvas);
        canvas.drawColor(Color.argb(82, 0, 0, 0));
        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        canvas.save();
        canvas.translate((root.getWidth() - sheet.getWidth()) / 2f, root.getHeight() - sheet.getHeight());
        sheet.draw(canvas);
        canvas.restore();
        File dir = new File(DIR);
        dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}
