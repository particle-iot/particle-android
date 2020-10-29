package io.particle.android.sdk.devicesetup.ui;


import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import java.security.PublicKey;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import io.particle.android.sdk.utils.SSID;


public class DeviceSetupState {

    static final Set<String> claimedDeviceIds = new ConcurrentSkipListSet<>();
    public static volatile SSID previouslyConnectedWifiNetwork;
    static volatile String claimCode;
    static volatile PublicKey publicKey;
    static volatile String deviceToBeSetUpId;
    public static volatile ConnectivityManager.NetworkCallback networkCallbacks;

    static void reset(Context context) {
        claimCode = null;
        claimedDeviceIds.clear();
        publicKey = null;
        deviceToBeSetUpId = null;
        previouslyConnectedWifiNetwork = null;
        if (networkCallbacks != null) {
            ConnectivityManager connMan = (ConnectivityManager) context.getSystemService(
                    Context.CONNECTIVITY_SERVICE
            );
            if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                connMan.unregisterNetworkCallback(networkCallbacks);
            }
            networkCallbacks = null;
        }
    }
}
