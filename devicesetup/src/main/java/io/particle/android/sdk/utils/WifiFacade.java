package io.particle.android.sdk.utils;

import static android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET;
import static android.net.NetworkCapabilities.NET_CAPABILITY_WIFI_P2P;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Build.VERSION_CODES;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import io.particle.android.sdk.utils.Funcy.Predicate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static io.particle.android.sdk.utils.Py.truthy;


@ParametersAreNonnullByDefault
public class WifiFacade {

    private static final TLog log = TLog.get(WifiFacade.class);

    @NonNull
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
                wifiConfiguration -> {
                    if (wifiConfiguration.SSID == null) {
                        return false;
                    } else {
                        return SSID.from(wifiConfiguration).equals(ssid);
                    }
                });
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
    public Network getNetworkObjectForCurrentWifiConnection() {
        // Android doesn't have any means of directly asking
        // "I want the Network obj for the Wi-Fi network with SSID <foo>".
        // Instead, you have to infer it based on the fact that you can only
        // have one connected Wi-Fi connection at a time.
        // (Update: one *regular* Wi-Fi connection, anyway.  See below.)

        return Funcy.findFirstMatch(
                Arrays.asList(connectivityManager.getAllNetworks()),
                network -> {
                    NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                    if (capabilities == null) {
                        return false;
                    }
                    // Don't try using the P2P Wi-Fi interfaces on recent Samsung devices
                    if (capabilities.hasCapability(NET_CAPABILITY_WIFI_P2P)) {
                        return false;
                    }
                    return capabilities.hasTransport(TRANSPORT_WIFI);
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
    private WifiInfo getConnectionInfo() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ) {
            return wifiManager.getConnectionInfo();
        }

        final CountDownLatch waitForNetwork = new CountDownLatch(1);
        final class AnswerBox {
            public WifiInfo wifiInfo;
        }
        final AnswerBox answerBox = new AnswerBox();
        final NetworkCallback networkCallback =
                new NetworkCallback(NetworkCallback.FLAG_INCLUDE_LOCATION_INFO) {
                    @Override
                    public void onCapabilitiesChanged(@NonNull Network network,
                                                      @NonNull NetworkCapabilities networkCapabilities) {
                        if (Build.VERSION.SDK_INT >= VERSION_CODES.Q) {
                            answerBox.wifiInfo = (WifiInfo) networkCapabilities.getTransportInfo();
                        }
                        waitForNetwork.countDown();
                    }
                };
        connectivityManager.registerNetworkCallback(
                new NetworkRequest.Builder()
                        .addTransportType(TRANSPORT_WIFI)
                        //.addCapability(NET_CAPABILITY_WIFI_P2P)
                        .build(), networkCallback);
        try {
            if (!waitForNetwork.await(5, TimeUnit.SECONDS)) {
                log.e("Timed out waiting for network to connect");
                return null;
            }
            log.v( "Network SSID found: " +  answerBox.wifiInfo.getSSID());
            return answerBox.wifiInfo;
        } catch (InterruptedException e) {
            log.e("Waiting for onAvailable failed", e);
            return null;
        } finally {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
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
        @SuppressLint("MissingPermission")
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        return (configuredNetworks == null)
                ? Collections.emptyList()
                : configuredNetworks;
    }
}
