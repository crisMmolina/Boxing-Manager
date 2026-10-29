package com.sigebox.juez.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNetwork;

import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class NetworkBinderTest {

    @Test
    public void ataElProcesoAlWifiDisponibleYLoSueltaAlPerderlo() {
        ConnectivityManager cm = ApplicationProvider.getApplicationContext()
                .getSystemService(ConnectivityManager.class);
        NetworkBinder binder = new NetworkBinder(cm);
        binder.start();

        ConnectivityManager.NetworkCallback callback = shadowOf(cm).getNetworkCallbacks().iterator().next();
        Network wifi = ShadowNetwork.newInstance(7);
        callback.onAvailable(wifi);
        assertEquals(wifi, cm.getBoundNetworkForProcess());

        callback.onLost(wifi);
        assertNull(cm.getBoundNetworkForProcess());

        binder.stop();
        assertEquals(0, shadowOf(cm).getNetworkCallbacks().size());
    }
}
