package io.particle.android.sdk.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build.VERSION_CODES;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.utils.Funcy.Predicate;

import static io.particle.android.sdk.utils.Py.truthy;


@ParametersAreNonnullByDefault
public class WifiFacade {

    private static final TLog log = TLog.get(WifiFacade.class);


    public static Predicate<ScanResult> is24Ghz = scanResult -> {
        // this approach lifted from the ScanResult source
        return scanResult.frequency > 2300 && scanResult.frequency < 2500;
    };


    // "truthy": see Py.truthy() javadoc
    // tl;dr:  not null or "" (empty string)
    public static Predicate<ScanResult> isWifiNameTruthy = scanResult -> truthy(scanResult.SSID);


    public static WifiFacade get(Context ctx) {
        Context appCtx = ctx.getApplicationContext();
        return new WifiFacade(
                (WifiManager) appCtx.getSystemService(Context.WIFI_SERVICE),
                (ConnectivityManager) appCtx.getSystemService(Context.CONNECTIVITY_SERVICE)
        );
    }

    private final WifiManager wifiManager;
    private final ConnectivityManager connectivityManager;

    private WifiFacade(WifiManager wifiManager, ConnectivityManager connectivityManager) {
        this.wifiManager = wifiManager;
        this.connectivityManager = connectivityManager;
    }

    public int getIdForConfiguredNetwork(SSID ssid) {
        WifiConfiguration configuredNetwork = Funcy.findFirstMatch(
                getConfiguredNetworks(),
                wifiConfiguration -> SSID.from(wifiConfiguration).equals(ssid));
        if (configuredNetwork == null) {
            log.d("No network found (returning -1) for SSID: " + ssid);
            return -1;
        } else {
            return configuredNetwork.networkId;
        }
    }

    public boolean reenableNetwork(SSID ssid) {
        int networkId = getIdForConfiguredNetwork(ssid);
        if (networkId == -1) {
            log.w("reenableNetwork(): no network found for SSID?? " + ssid);
            return false;
        } else {
            log.d("Reenabling network configuration for:" + ssid);
            return wifiManager.enableNetwork(networkId, false);
        }
    }

    public boolean removeNetwork(SSID ssid) {
        int networkId = getIdForConfiguredNetwork(ssid);
        if (networkId == -1) {
            log.w("No network found for SSID " + ssid);
            return false;
        } else {
            log.d("Removing network configuration for:" + ssid);
            return removeNetwork(networkId);
        }
    }

    @Nullable
    public SSID getCurrentlyConnectedSSID() {
        WifiInfo connectionInfo = getConnectionInfo();
        if (connectionInfo == null) {
            log.w("getCurrentlyConnectedSSID(): WifiManager.getConnectionInfo() returned null");
            return null;
        } else {
            SSID ssid = SSID.from(connectionInfo);
            log.d("Currently connected to: " + ssid +
                    ", supplicant state: " + connectionInfo.getSupplicantState());
            return ssid;
        }
    }

    @Nullable
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    public Network getNetworkForSSID(SSID ssid) {
        // Android doesn't have any means of directly asking
        // "I want the Network obj for the Wi-Fi network with SSID <foo>".
        // Instead, you have to infer it based on a field.  Let's hope that
        // the behavior of "NetworkInfo.getExtraInfo()" doesn't ever change...
        return Funcy.findFirstMatch(
                Arrays.asList(connectivityManager.getAllNetworks()),
                network -> {
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                    return Py.truthy(networkInfo.getExtraInfo()) && SSID.from(networkInfo.getExtraInfo()).equals(ssid);
                }
        );
    }

    public int addNetwork(WifiConfiguration config) {
        log.d("addNetwork with SSID " + config.SSID + ": " + config);
        return wifiManager.addNetwork(config);
    }

    public boolean disconnect() {
        log.d("disconnect()");
        return wifiManager.disconnect();
    }

    public boolean enableNetwork(int networkId, boolean disableOthers) {
        log.d("enableNetwork for networkID " + networkId);
        return wifiManager.enableNetwork(networkId, disableOthers);
    }

    public WifiConfiguration getWifiConfiguration(SSID ssid) {
        List<WifiConfiguration> wifiConfigurations = getConfiguredNetworks();
        for (WifiConfiguration configuration : wifiConfigurations) {
            log.d("Found configured wifi: " + configuration.SSID);
            if (configuration.SSID.equals(ssid.inQuotes())) {
                return configuration;
            }
        }
        return null;
    }

    @Nullable
    public WifiInfo getConnectionInfo() {
        return wifiManager.getConnectionInfo();
    }

    public List<ScanResult> getScanResults() {
        // per the WifiManager docs, this seems like it should never return null, but I've
        // gotten null back before, possibly on an older API level.
        List<ScanResult> results = wifiManager.getScanResults();
        return (results == null) ? Collections.emptyList() : results;
    }

    public boolean isWifiEnabled() {
        return wifiManager.isWifiEnabled();
    }

    public void reassociate() {
        log.d("reassociate");
        wifiManager.reassociate();
    }

    public boolean reconnect() {
        log.d("reconnect");
        return wifiManager.reconnect();
    }

    public boolean removeNetwork(int networkId) {
        log.d("Removing network configuration for networkId: " + networkId);
        return wifiManager.removeNetwork(networkId);
    }

    public boolean setWifiEnabled(boolean enabled) {
        log.d("setWifiEnabled: " + enabled);
        return wifiManager.setWifiEnabled(enabled);
    }

    public boolean startScan() {
        log.d("startScan()");
        return wifiManager.startScan();
    }

    private List<WifiConfiguration> getConfiguredNetworks() {
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        return (configuredNetworks == null)
                ? Collections.emptyList()
                : configuredNetworks;
    }
}
