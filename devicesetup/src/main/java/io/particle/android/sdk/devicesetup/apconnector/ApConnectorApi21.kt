package io.particle.android.sdk.devicesetup.apconnector

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build.VERSION
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import io.particle.android.sdk.devicesetup.SimpleReceiver
import io.particle.android.sdk.devicesetup.apconnector.ApConnector.Client
import io.particle.android.sdk.utils.*
import java.util.concurrent.atomic.AtomicInteger

@MainThread
class ApConnectorApi21(
    ctx: Context,
    private val softAPConfigRemover: SoftAPConfigRemover,
    private val wifiFacade: WifiFacade
) : ApConnector {

    private val appContext: Context = ctx.applicationContext
    private val client: DecoratingClient = DecoratingClient { clearState() }
    private val wifiLogger: SimpleReceiver
    private val mainThreadHandler: Handler = Handler(Looper.getMainLooper())
    private val setupRunnables = mutableListOf<() -> Unit>()
    private var wifiStateChangeListener: SimpleReceiver? = null
    private var onTimeoutRunnable: (() -> Unit)? = null

    /**
     * Connect this Android device to the specified AP.
     *
     * @param config the WifiConfiguration defining which AP to connect to
     */
    override fun connectToAP(config: WifiConfiguration, client: Client) {
        wifiLogger.register()
        this.client.wrappedClient = client

        // cancel any currently running timeout, etc
        clearState()
        val configSSID = SSID.from(config)
        val currentConnectionInfo = wifiFacade.connectionInfo
        // are we already connected to the right AP?  (this could happen on retries)
        if (isAlreadyConnectedToTargetNetwork(currentConnectionInfo, configSSID)) {
            // we're already connected to this AP, nothing to do.
            client.onApConnectionSuccessful(config)
            return
        }
        scheduleTimeoutCheck(ApConnector.CONNECT_TO_DEVICE_TIMEOUT_MILLIS, config)
        wifiStateChangeListener = SimpleReceiver.newRegisteredReceiver(
            appContext, WIFI_STATE_CHANGE_FILTER
        ) { ctx: Context?, intent: Intent -> onWifiChangeBroadcastReceived(intent, config) }
        val useMoreComplexConnectionProcess = VERSION.SDK_INT < 18


        // we don't need this for its atomicity, we just need it as a 'final' reference to an
        // integer which can be shared by a couple of the Runnables below
        val networkID = AtomicInteger(-1)

        // everything below is created in Runnables and scheduled on the runloop to avoid some
        // wonkiness I ran into when trying to do every one of these steps one right after
        // the other on the same thread.
        val alreadyConfiguredId = wifiFacade.getIdForConfiguredNetwork(configSSID)
        if (alreadyConfiguredId != -1 && !useMoreComplexConnectionProcess) {
            // For some unexplained (and probably sad-trombone-y) reason, if the AP specified was
            // already configured and had been connected to in the past, it will often get to
            // the "CONNECTING" event, but just before firing the "CONNECTED" event, the
            // WifiManager appears to change its mind and reconnects to whatever configured and
            // available AP it feels like.
            //
            // As a remedy, we pre-emptively remove that config.  *shakes fist toward Mountain View*
            setupRunnables.add {
                if (wifiFacade.removeNetwork(alreadyConfiguredId)) {
                    log.d("Removed already-configured $configSSID network successfully")
                } else {
                    log.e("Somehow failed to remove the already-configured network!?")
                    // not calling this state an actual failure, since it might succeed anyhow,
                    // and if it doesn't, the worst case is a longer wait to find that out.
                }
            }
        }
        if (alreadyConfiguredId == -1 || !useMoreComplexConnectionProcess) {
            setupRunnables.add {
                log.d("Adding network $configSSID")
                networkID.set(wifiFacade.addNetwork(config))
                if (networkID.get() == -1) {
                    val configuration = wifiFacade.getWifiConfiguration(configSSID)
                    if (configuration != null) {
                        networkID.set(configuration.networkId)
                    }
                }
                if (networkID.get() == -1) {
                    log.e("Adding network $configSSID failed.")
                    client.onApConnectionFailed(config)
                } else {
                    log.i("Added network with ID $networkID successfully")
                }
            }
        }
        if (useMoreComplexConnectionProcess) {
            setupRunnables.add {
                log.d("Disconnecting from networks; reconnecting momentarily.")
                wifiFacade.disconnect()
            }
        }
        setupRunnables.add {
            log.i("Enabling network " + configSSID + " with network ID " + networkID.get())
            wifiFacade.enableNetwork(networkID.get(), !useMoreComplexConnectionProcess)
        }
        if (useMoreComplexConnectionProcess) {
            setupRunnables.add {
                log.d("Disconnecting from networks; reconnecting momentarily.")
                wifiFacade.reconnect()
            }
        }
        val currentlyConnectedSSID = wifiFacade.currentlyConnectedSSID
        softAPConfigRemover.onWifiNetworkDisabled(currentlyConnectedSSID)
        var timeout: Long = 0
        for (runnable in setupRunnables) {
            EZ.runOnMainThreadDelayed(timeout, runnable)
            timeout += 1500
        }
    }

    override fun stop() {
        client.wrappedClient = null
        clearState()
        wifiLogger.unregister()
    }

    private fun scheduleTimeoutCheck(timeoutInMillis: Long, config: WifiConfiguration) {
        onTimeoutRunnable = {
            log.e("AP connection attempt timed out")
            client.onApConnectionFailed(config)
        }
        mainThreadHandler.postDelayed(onTimeoutRunnable!!, timeoutInMillis)
    }

    private fun clearState() {
        if (onTimeoutRunnable != null) {
            mainThreadHandler.removeCallbacks(onTimeoutRunnable!!)
            onTimeoutRunnable = null
        }
        if (wifiStateChangeListener != null) {
            appContext.unregisterReceiver(wifiStateChangeListener)
            wifiStateChangeListener = null
        }
        for (runnable in setupRunnables) {
            mainThreadHandler.removeCallbacks(runnable)
        }
        setupRunnables.clear()
    }

    private fun onWifiChangeBroadcastReceived(intent: Intent, config: WifiConfiguration) {
        // this will only be present if the new state is CONNECTED
        val wifiInfo = intent.getParcelableExtra<WifiInfo>(WifiManager.EXTRA_WIFI_INFO)
        if (wifiInfo == null || wifiInfo.ssid == null) {
            // no WifiInfo or SSID means we're not interested.
            return
        }
        val newlyConnectedSSID = SSID.from(wifiInfo)
        log.i("Connected to: $newlyConnectedSSID")
        if (newlyConnectedSSID == SSID.from(config)) {
            client.onApConnectionSuccessful(config)
        }
    }

    companion object {
        private val log = TLog.get(ApConnectorApi21::class.java)
        private val WIFI_STATE_CHANGE_FILTER = IntentFilter(
            WifiManager.NETWORK_STATE_CHANGED_ACTION
        )

        fun isAlreadyConnectedToTargetNetwork(
            currentConnectionInfo: WifiInfo?,
            targetNetworkSsid: SSID
        ): Boolean {
            return (isCurrentlyConnectedToAWifiNetwork(currentConnectionInfo)
                    && targetNetworkSsid == SSID.from(currentConnectionInfo))
        }

        private fun isCurrentlyConnectedToAWifiNetwork(currentConnectionInfo: WifiInfo?): Boolean {
            return (currentConnectionInfo != null && Py.truthy(currentConnectionInfo.ssid)
                    && currentConnectionInfo.networkId != -1 // yes, this happens.  Thanks, Android.
                    && "0x" != currentConnectionInfo.ssid)
        }
    }

    init {
        wifiLogger = SimpleReceiver.newReceiver(
            appContext,
            IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)) { _: Context?, intent: Intent ->
            log.d("Received " + WifiManager.NETWORK_STATE_CHANGED_ACTION)
            log.d("EXTRA_NETWORK_INFO: " + intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO))
            // this will only be present if the new state is CONNECTED
            val wifiInfo = intent.getParcelableExtra<WifiInfo>(WifiManager.EXTRA_WIFI_INFO)
            log.d("WIFI_INFO: $wifiInfo")
        }
    }
}