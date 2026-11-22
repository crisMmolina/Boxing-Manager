package com.sigebox.juez;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class SmokeTest {

    private static final String PKG = "com.sigebox.juez";

    @Test
    public void applicationIdEsSigeboxJuez() {
        assertEquals(PKG, BuildConfig.APPLICATION_ID);
    }

    @Test
    public void appNameYClaseApplication() {
        Context context = ApplicationProvider.getApplicationContext();
        assertEquals("Box Juez", context.getString(R.string.app_name));
        assertTrue(context instanceof SigeboxApp);
    }

    @Test
    public void coreLlegaPorComposite() {
        // Gson es `api` de com.sigebox:core: si core no está en el classpath, esto no compila.
        assertEquals("{\"v\":1}", new Gson().toJson(java.util.Collections.singletonMap("v", 1)));
    }

    @Test
    public void manifestDeclaraPermisos() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PackageInfo info = context.getPackageManager()
                .getPackageInfo(PKG, PackageManager.GET_PERMISSIONS);
        List<String> pedidos = Arrays.asList(info.requestedPermissions);
        for (String permiso : new String[] {
                "android.permission.INTERNET",
                "android.permission.CAMERA",
                "android.permission.VIBRATE",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.CHANGE_NETWORK_STATE",
                "android.permission.ACCESS_WIFI_STATE"}) {
            assertTrue("falta " + permiso, pedidos.contains(permiso));
        }
    }

    @Test
    public void manifestDeclaraTodasLasActivities() throws Exception {
        PackageManager pm = ApplicationProvider.getApplicationContext().getPackageManager();
        for (String activity : new String[] {
                PKG + ".MainActivity",
                PKG + ".terms.TermsActivity",
                PKG + ".join.JoinActivity",
                PKG + ".join.ScanActivity",
                PKG + ".join.WaitingActivity",
                PKG + ".score.ScoreActivity",
                PKG + ".late.LateScoreActivity"}) {
            // Lanza NameNotFoundException si la Activity no está en el Manifest.
            pm.getActivityInfo(new ComponentName(PKG, activity), 0);
        }
    }
}
