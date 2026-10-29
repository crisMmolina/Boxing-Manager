package com.sigebox.juez.net;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

/**
 * Ata el proceso al Wi‑Fi del hotspot aunque no tenga internet. Sin esto Android puede mandar los sockets por datos
 * móviles o marcar la red como "sin internet" y abandonarla (IDEA §7; spike AND-002).
 */
public final class NetworkBinder {
    private final ConnectivityManager connectivity;
    private ConnectivityManager.NetworkCallback callback;

    public NetworkBinder(ConnectivityManager connectivity) {
        this.connectivity = connectivity;
    }

    public void start() {
        if (callback != null) {
            return;
        }
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                connectivity.bindProcessToNetwork(network);
            }

            @Override
            public void onLost(Network network) {
                if (network.equals(connectivity.getBoundNetworkForProcess())) {
                    connectivity.bindProcessToNetwork(null);
                }
            }
        };
        connectivity.requestNetwork(request, callback);
    }

    public void stop() {
        if (callback != null) {
            connectivity.unregisterNetworkCallback(callback);
            connectivity.bindProcessToNetwork(null);
            callback = null;
        }
    }
}
