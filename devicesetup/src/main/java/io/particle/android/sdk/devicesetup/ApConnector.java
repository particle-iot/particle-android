package io.particle.android.sdk.devicesetup;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.MainThread;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.SoftAPConfigRemover;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.WifiFacade;

import static io.particle.android.sdk.utils.Py.list;
import static io.particle.android.sdk.utils.Py.truthy;


@MainThread
public class ApConnector {

    public interface Client {

        void onApConnectionSuccessful(WifiConfiguration config);

        void onApConnectionFailed(WifiConfiguration config);

    }

    public static WifiConfiguration buildUnsecuredConfig(SSID ssid) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = ssid.inQuotes();
        config.hiddenSSID = false;
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        // have to set a very high number in order to ensure that Android doesn't
        // immediately drop this connection and reconnect to the a different AP
        config.priority = 999999;
        return config;
    }


    private static final TLog log = TLog.get(ApConnector.class);

    public static final long CONNECT_TO_DEVICE_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(20);

    private static final IntentFilter WIFI_STATE_CHANGE_FILTER = new IntentFilter(
            WifiManager.NETWORK_STATE_CHANGED_ACTION);

    private final DecoratedClient client;
    private final WifiFacade wifiFacade;
    private final SimpleReceiver wifiLogger;
    private final Context appContext;
    private final SoftAPConfigRemover softAPConfigRemover;
    private final Handler mainThreadHandler;
    private final List<Runnable> setupRunnables = list();

    private SimpleReceiver wifiStateChangeListener;
    private Runnable onTimeoutRunnable;

    public ApConnector(Context ctx, SoftAPConfigRemover softAPConfigRemover, WifiFacade wifiFacade) {
        this.appContext = ctx.getApplicationContext();
        this.client = new DecoratedClient();
        this.wifiFacade = wifiFacade;
        this.softAPConfigRemover = softAPConfigRemover;
        this.mainThreadHandler = new Handler(Looper.getMainLooper());
        this.wifiLogger = SimpleReceiver.newReceiver(
                appContext, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION),
                (context, intent) -> {
                    log.d("Received " + WifiManager.NETWORK_STATE_CHANGED_ACTION);
                    log.d("EXTRA_NETWORK_INFO: " + intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO));
                    // this will only be present if the new state is CONNECTED
                    WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                    log.d("WIFI_INFO: " + wifiInfo);
                });
    }

    /**
     * Connect this Android device to the specified AP.
     *
     * @param config the WifiConfiguration defining which AP to connect to
     * @return the SSID that was connected prior to calling this method.  Will be null if
     * there was no network connected, or if already connected to the target network.
     */
    public SSID connectToAP(final WifiConfiguration config, Client client) {
        wifiLogger.register();
        this.client.setDecoratedClient(client);

        // cancel any currently running timeout, etc
        clearState();

        SSID configSSID = SSID.from(config);
        final WifiInfo currentConnectionInfo = wifiFacade.getConnectionInfo();
        // are we already connected to the right AP?  (this could happen on retries)
        if (isAlreadyConnectedToTargetNetwork(currentConnectionInfo, configSSID)) {
            // we're already connected to this AP, nothing to do.
            client.onApConnectionSuccessful(config);
            return null;
        }

        scheduleTimeoutCheck(CONNECT_TO_DEVICE_TIMEOUT_MILLIS, config);
        wifiStateChangeListener = SimpleReceiver.newRegisteredReceiver(
                appContext, WIFI_STATE_CHANGE_FILTER,
                (ctx, intent) -> onWifiChangeBroadcastReceived(intent, config));
        final boolean useMoreComplexConnectionProcess = Build.VERSION.SDK_INT < 18;


        // we don't need this for its atomicity, we just need it as a 'final' reference to an
        // integer which can be shared by a couple of the Runnables below
        final AtomicInteger networkID = new AtomicInteger(-1);

        // everything below is created in Runnables and scheduled on the runloop to avoid some
        // wonkiness I ran into when trying to do every one of these steps one right after
        // the other on the same thread.

        final int alreadyConfiguredId = wifiFacade.getIdForConfiguredNetwork(configSSID);
        if (alreadyConfiguredId != -1 && !useMoreComplexConnectionProcess) {
            // For some unexplained (and probably sad-trombone-y) reason, if the AP specified was
            // already configured and had been connected to in the past, it will often get to
            // the "CONNECTING" event, but just before firing the "CONNECTED" event, the
            // WifiManager appears to change its mind and reconnects to whatever configured and
            // available AP it feels like.
            //
            // As a remedy, we pre-emptively remove that config.  *shakes fist toward Mountain View*

            setupRunnables.add(() -> {
                if (wifiFacade.removeNetwork(alreadyConfiguredId)) {
                    log.d("Removed already-configured " + configSSID + " network successfully");
                } else {
                    log.e("Somehow failed to remove the already-configured network!?");
                    // not calling this state an actual failure, since it might succeed anyhow,
                    // and if it doesn't, the worst case is a longer wait to find that out.
                }
            });
        }

        if (alreadyConfiguredId == -1 || !useMoreComplexConnectionProcess) {
            setupRunnables.add(() -> {
                log.d("Adding network " + configSSID);
                networkID.set(wifiFacade.addNetwork(config));

                if (networkID.get() == -1) {
                    WifiConfiguration configuration = wifiFacade.getWifiConfiguration(configSSID);
                    if (configuration != null) {
                        networkID.set(configuration.networkId);
                    }
                }

                if (networkID.get() == -1) {
                    log.e("Adding network " + configSSID + " failed.");
                    client.onApConnectionFailed(config);

                } else {
                    log.i("Added network with ID " + networkID + " successfully");
                }
            });
        }

        if (useMoreComplexConnectionProcess) {
            setupRunnables.add(() -> {
                log.d("Disconnecting from networks; reconnecting momentarily.");
                wifiFacade.disconnect();
            });
        }

        setupRunnables.add(() -> {
            log.i("Enabling network " + configSSID + " with network ID " + networkID.get());
            wifiFacade.enableNetwork(networkID.get(), !useMoreComplexConnectionProcess);
        });

        if (useMoreComplexConnectionProcess) {
            setupRunnables.add(() -> {
                log.d("Disconnecting from networks; reconnecting momentarily.");
                wifiFacade.reconnect();
            });
        }

        SSID currentlyConnectedSSID = wifiFacade.getCurrentlyConnectedSSID();
        softAPConfigRemover.onWifiNetworkDisabled(currentlyConnectedSSID);

        long timeout = 0;
        for (Runnable runnable : setupRunnables) {
            EZ.runOnMainThreadDelayed(timeout, runnable);
            timeout += 1500;
        }

        return SSID.from(currentConnectionInfo);
    }

    public void stop() {
        client.setDecoratedClient(null);
        clearState();
        wifiLogger.unregister();
    }


    private static boolean isAlreadyConnectedToTargetNetwork(WifiInfo currentConnectionInfo,
                                                             SSID targetNetworkSsid) {
        return (isCurrentlyConnectedToAWifiNetwork(currentConnectionInfo)
                && targetNetworkSsid.equals(SSID.from(currentConnectionInfo))
        );
    }

    private static boolean isCurrentlyConnectedToAWifiNetwork(WifiInfo currentConnectionInfo) {
        return (currentConnectionInfo != null
                && truthy(currentConnectionInfo.getSSID())
                && currentConnectionInfo.getNetworkId() != -1
                // yes, this happens.  Thanks, Android.
                && !"0x".equals(currentConnectionInfo.getSSID()));
    }

    private void scheduleTimeoutCheck(long timeoutInMillis, final WifiConfiguration config) {
        onTimeoutRunnable = () -> client.onApConnectionFailed(config);
        mainThreadHandler.postDelayed(onTimeoutRunnable, timeoutInMillis);
    }

    private void clearState() {
        if (onTimeoutRunnable != null) {
            mainThreadHandler.removeCallbacks(onTimeoutRunnable);
            onTimeoutRunnable = null;
        }

        if (wifiStateChangeListener != null) {
            appContext.unregisterReceiver(wifiStateChangeListener);
            wifiStateChangeListener = null;
        }

        for (Runnable runnable : setupRunnables) {
            mainThreadHandler.removeCallbacks(runnable);
        }
        setupRunnables.clear();
    }

    private void onWifiChangeBroadcastReceived(Intent intent, WifiConfiguration config) {
        // this will only be present if the new state is CONNECTED
        WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
        if (wifiInfo == null || wifiInfo.getSSID() == null) {
            // no WifiInfo or SSID means we're not interested.
            return;
        }
        SSID newlyConnectedSSID = SSID.from(wifiInfo);
        log.i("Connected to: " + newlyConnectedSSID);
        if (newlyConnectedSSID.equals(SSID.from(config))) {
            // FIXME: find a way to record success in memory in case this happens to happen
            // during a config change (etc)?
            client.onApConnectionSuccessful(config);
        }
    }


    // a Client decorator to ensure clearState() is called every time
    private class DecoratedClient implements Client {

        Client decoratedClient;

        @Override
        public void onApConnectionSuccessful(WifiConfiguration config) {
            clearState();
            if (decoratedClient != null) {
                decoratedClient.onApConnectionSuccessful(config);
            }
        }

        @Override
        public void onApConnectionFailed(WifiConfiguration config) {
            clearState();
            if (decoratedClient != null) {
                decoratedClient.onApConnectionFailed(config);
            }
        }

        void setDecoratedClient(Client decoratedClient) {
            this.decoratedClient = decoratedClient;
        }

    }

}
