package io.particle.android.sdk.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

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

    public SoftAPConfigRemover(Context context, WifiFacade wifiFacade) {
        this.wifiFacade = wifiFacade;
        Context ctx = context.getApplicationContext();
        prefs = ctx.getSharedPreferences(PREFS_SOFT_AP_NETWORK_REMOVER, Context.MODE_PRIVATE);
    }

    public void onSoftApConfigured(SSID newSsid) {
        // make a defensive copy of what we get back
        Set<SSID> ssids = set(loadSSIDsWithKey(KEY_SOFT_AP_SSIDS));
        ssids.add(newSsid);
        saveWithKey(KEY_SOFT_AP_SSIDS, ssids);
    }

    public void removeAllSoftApConfigs() {
        for (SSID ssid : loadSSIDsWithKey(KEY_SOFT_AP_SSIDS)) {
            wifiFacade.removeNetwork(ssid);
        }
        saveWithKey(KEY_SOFT_AP_SSIDS, set());
    }

    public void onWifiNetworkDisabled(SSID ssid) {
        log.v("onWifiNetworkDisabled() " + ssid);
        Set<SSID> ssids = set(loadSSIDsWithKey(KEY_DISABLED_WIFI_SSIDS));
        ssids.add(ssid);
        saveWithKey(KEY_DISABLED_WIFI_SSIDS, ssids);
    }

    public void reenableWifiNetworks() {
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
}
