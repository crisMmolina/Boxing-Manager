package com.sigebox.juez.shots;

import static org.junit.Assume.assumeTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.core.content.ContextCompat;
import com.sigebox.juez.R;
import com.sigebox.juez.ui.CornerCard;
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
 * AND-039: captura de la tarjeta de esquina y de la fila de arena, en reposo y con − y + presionados, en claro y
 * oscuro, a 390 × 844 dp. Solo corre con {@code SIGEBOX_SCREENSHOTS=<carpeta>}; deja {@code esquina-*.png}.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 36, qualifiers = "w390dp-h844dp-port-xxhdpi")
public class CornerCardShot {

    private static final String DIR = System.getenv("SIGEBOX_SCREENSHOTS");

    @Before
    public void setUp() {
        assumeTrue("solo con SIGEBOX_SCREENSHOTS", DIR != null && !DIR.isEmpty());
    }

    @Test
    public void claro() throws Exception {
        capture("esquina-claro", false);
    }

    @Test
    @Config(qualifiers = "+night")
    public void oscuro() throws Exception {
        capture("esquina-oscuro", false);
    }

    @Test
    public void presionadoClaro() throws Exception {
        capture("esquina-presionado-claro", true);
    }

    @Test
    @Config(qualifiers = "+night")
    public void presionadoOscuro() throws Exception {
        capture("esquina-presionado-oscuro", true);
    }

    /** Tarjeta roja y azul, y fila de arena roja y azul; con {@code pressed}, − en las rojas y + en las azules. */
    private void capture(String name, boolean pressed) throws Exception {
        ActivityController<Activity> c = Robolectric.buildActivity(Activity.class);
        Activity activity = c.get();
        activity.setTheme(R.style.Theme_Sigebox);
        c.setup();
        Settings.Global.putFloat(activity.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE,
                pressed ? 1f : 0f);
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(ContextCompat.getColor(activity, R.color.surface));
        int pad = Math.round(16 * activity.getResources().getDisplayMetrics().density);
        column.setPadding(pad, pad, pad, pad);
        // En ScrollView la columna toma su alto natural y la última fila no se recorta.
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(column);
        activity.setContentView(scroll);
        LayoutInflater inflater = LayoutInflater.from(activity);
        View[] views = new View[4];
        int i = 0;
        for (int layout : new int[] {R.layout.view_corner_card, R.layout.view_corner_row}) {
            for (boolean red : new boolean[] {true, false}) {
                View view = inflater.inflate(layout, column, false);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) view.getLayoutParams();
                lp.bottomMargin = pad;
                column.addView(view, lp);
                CornerCard card = new CornerCard(view, red, d -> { });
                card.bind(5, true);
                views[i++] = view;
            }
        }
        ShadowLooper.idleMainLooper();
        if (pressed) {
            for (int k = 0; k < views.length; k++) {
                press(views[k].findViewById(k % 2 == 0 ? R.id.corner_minus : R.id.corner_plus));
            }
            ShadowLooper.idleMainLooper(400, TimeUnit.MILLISECONDS);
        }
        shot(name, column);
        activity.finish();
    }

    private static void press(View button) {
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 10f, 10f, 0);
        button.dispatchTouchEvent(down);
        down.recycle();
        button.setPressed(true);
    }

    private static void shot(String name, View root) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        File dir = new File(DIR);
        dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}
