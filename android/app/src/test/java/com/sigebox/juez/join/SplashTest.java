package com.sigebox.juez.join;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.VectorDrawable;

import androidx.test.core.app.ApplicationProvider;

import com.sigebox.juez.MainActivity;
import com.sigebox.juez.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Pantalla de carga con el logo (spec 004 §Arranque): tema de arranque, sin dependencias ni espera. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class SplashTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void laAppAbreConElTemaDeArranque() throws Exception {
        ActivityInfo info = context.getPackageManager()
                .getActivityInfo(new ComponentName(context, MainActivity.class), PackageManager.GET_META_DATA);
        assertEquals(R.style.Theme_Sigebox_Splash, info.theme);
    }

    @Test
    public void elFondoDelTemaEsElLogoSobreBg() {
        Context themed = new android.view.ContextThemeWrapper(context, R.style.Theme_Sigebox_Splash);
        TypedArray a = themed.obtainStyledAttributes(new int[] {android.R.attr.windowBackground});
        int id = a.getResourceId(0, 0);
        a.recycle();
        assertEquals(R.drawable.splash, id);

        Drawable splash = themed.getDrawable(R.drawable.splash);
        LayerDrawable layers = (LayerDrawable) splash;
        assertEquals(2, layers.getNumberOfLayers());
        assertTrue("el logo es un vector dentro de la app", layers.getDrawable(1) instanceof VectorDrawable);
        assertTrue("tamaño fijo en dp, centrado", layers.getLayerWidth(1) > 0 && layers.getLayerHeight(1) > 0);
    }
}
