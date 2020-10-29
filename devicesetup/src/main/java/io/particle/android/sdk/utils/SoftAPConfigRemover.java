package io.particle.android.sdk.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import java.util.Set;

import io.particle.android.sdk.devicesetup.ui.DeviceSetupState;

import static io.particle.android.sdk.utils.Funcy.transformSet;
import static io.particle.android.sdk.utils.Py.set;


public class SoftAPConfigRemover {

    private static final TLog log = TLog.get(SoftAPConfigRemover.class);

    private static final String
            PREFS_SOFT_AP_NETWORK_REMOVER = "PREFS_SOFT_AP_NETWORK_REMOVER",
            KEY_SOFT_AP_SSIDS = "KEY_SOFT_AP_SSIDS",
            KEY_DISABLED_WIFI_SSIDS = "KEY_DISABLED_WIFI_SSIDS";


    private final SharedPreferences prefs;
    private final WifiFacade wifiFacade;
    private final Context context;

    public SoftAPConfigRemover(Context context, WifiFacade wifiFacade) {
        this.wifiFacade = wifiFacade;
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREFS_SOFT_AP_NETWORK_REMOVER, Context.MODE_PRIVATE);
    }

    public void onSoftApConfigured(SSID newSsid) {
        if (VERSION.SDK_INT >= VERSION_CODES.Q) return; // not applicable here

        // make a defensive copy of what we get back
        Set<SSID> ssids = set(loadSSIDsWithKey(KEY_SOFT_AP_SSIDS));
        ssids.add(newSsid);
        saveWithKey(KEY_SOFT_AP_SSIDS, ssids);
    }

    public void removeAllSoftApConfigs() {
        unregisterNetworkCallbacks();

        for (SSID ssid : loadSSIDsWithKey(KEY_SOFT_AP_SSIDS)) {
            wifiFacade.removeNetwork(ssid);
        }
        saveWithKey(KEY_SOFT_AP_SSIDS, set());
    }

    public void onWifiNetworkDisabled(SSID ssid) {
        if (VERSION.SDK_INT >= VERSION_CODES.Q) return; // not applicable here

        log.v("onWifiNetworkDisabled() " + ssid);
        Set<SSID> ssids = set(loadSSIDsWithKey(KEY_DISABLED_WIFI_SSIDS));
        ssids.add(ssid);
        saveWithKey(KEY_DISABLED_WIFI_SSIDS, ssids);
    }

    public void reenableWifiNetworks() {
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            unregisterNetworkCallbacks();
            return;
        }

        log.v("reenableWifiNetworks()");
        for (SSID ssid : loadSSIDsWithKey(KEY_DISABLED_WIFI_SSIDS)) {
            wifiFacade.reenableNetwork(ssid);
        }
        saveWithKey(KEY_DISABLED_WIFI_SSIDS, set());
    }

    private Set<SSID> loadSSIDsWithKey(String key) {
        return Funcy.transformSet(prefs.getStringSet(key, set()), SSID::from);
    }

    @SuppressLint("CommitPrefEdits")
    private void saveWithKey(String key, Set<SSID> ssids) {
        Set<String> asStrings = transformSet(ssids, SSID::toString);
        prefs.edit()
                .putStringSet(key, asStrings)
                .apply();
    }

    private void unregisterNetworkCallbacks() {
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            ConnectivityManager connMan = (ConnectivityManager) context.getSystemService(
                    Context.CONNECTIVITY_SERVICE
            );

            if (DeviceSetupState.networkCallbacks != null) {
                connMan.unregisterNetworkCallback(DeviceSetupState.networkCallbacks);
                DeviceSetupState.networkCallbacks = null;
            }
        }
    }
}
