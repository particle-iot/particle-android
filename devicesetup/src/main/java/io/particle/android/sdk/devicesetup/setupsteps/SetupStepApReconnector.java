package io.particle.android.sdk.devicesetup.setupsteps;


import android.net.wifi.WifiConfiguration;
import android.os.Handler;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.particle.android.sdk.devicesetup.ApConnector;
import io.particle.android.sdk.devicesetup.ApConnector.Client;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.WifiFacade;


public class SetupStepApReconnector {

    private final WifiFacade wifiFacade;
    private final ApConnector apConnector;
    private final Handler mainThreadHandler;
    private final SSID softApSSID;
    private final WifiConfiguration config;

    public SetupStepApReconnector(WifiFacade wifiFacade, ApConnector apConnector,
                                  Handler mainThreadHandler, SSID softApSSID) {
        this.wifiFacade = wifiFacade;
        this.apConnector = apConnector;
        this.mainThreadHandler = mainThreadHandler;
        this.softApSSID = softApSSID;
        this.config = ApConnector.buildUnsecuredConfig(softApSSID);
    }

    void ensureConnectionToSoftAp() throws IOException {
        if (isConnectedToSoftAp()) {
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean gotConnected = new AtomicBoolean(false);

        mainThread(() ->
                apConnector.connectToAP(config, new Client() {
                    @Override
                    public void onApConnectionSuccessful(WifiConfiguration config) {
                        gotConnected.set(true);
                        latch.countDown();
                    }

                    @Override
                    public void onApConnectionFailed(WifiConfiguration config) {
                        latch.countDown();
                    }
                }));

        // 50ms is an arbitrary number; just give the ApConnector time to do its work and allow for
        // a slight delay for overhead, etc.
        awaitCountdown(latch, ApConnector.CONNECT_TO_DEVICE_TIMEOUT_MILLIS + 50);

        // throw if it didn't work, otherwise assume success
        if (!gotConnected.get()) {
            throw new IOException("ApConnector did not finish in time; could not reconnect to soft AP");
        }
    }

    private boolean isConnectedToSoftAp() {
        return softApSSID.equals(wifiFacade.getCurrentlyConnectedSSID());
    }

    private CountDownLatch mainThread(final Runnable runnable) {
        final CountDownLatch latch = new CountDownLatch(1);
        mainThreadHandler.post(() -> {
            runnable.run();
            latch.countDown();
        });
        return latch;
    }

    private boolean awaitCountdown(CountDownLatch latch, long awaitMs) {
        try {
            return latch.await(awaitMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

}
