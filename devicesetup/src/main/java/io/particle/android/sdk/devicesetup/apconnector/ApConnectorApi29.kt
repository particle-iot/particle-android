package io.particle.android.sdk.devicesetup.apconnector

import android.annotation.SuppressLint
import android.content.Context
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build.VERSION_CODES
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import io.particle.android.sdk.devicesetup.apconnector.ApConnector.Client
import io.particle.android.sdk.utils.SSID
import io.particle.android.sdk.utils.WifiFacade
import mu.KotlinLogging


private val log = KotlinLogging.logger {}


@RequiresApi(VERSION_CODES.LOLLIPOP)
class ApConnectorApi29(
    ctx: Context,
    private val wifiFacade: WifiFacade
) : ApConnector {

    private val connectivityManager: ConnectivityManager =
        ctx.applicationContext.getSystemService()!!
    private val decoratingClient: DecoratingClient = DecoratingClient { clearState() }
    private lateinit var networkCallbacks: ConnectivityManager.NetworkCallback

    @SuppressLint("NewApi")
    @MainThread
    override fun connectToAP(config: WifiConfiguration, client: Client) {
        val configSSID = SSID.from(config)

        log.info { "preparing to connect to $configSSID..." }

        decoratingClient.wrappedClient = client
        networkCallbacks = buildNetworkCallbacks(config)

        val currentConnectionInfo = wifiFacade.connectionInfo
        // are we already connected to the right AP?  (this could happen on retries)
        if (ApConnectorApi21.isAlreadyConnectedToTargetNetwork(currentConnectionInfo, configSSID)) {
            // we're already connected to this AP, nothing to do.
            log.info { "we're already connected to $configSSID, nothing to do." }
            decoratingClient.onApConnectionSuccessful(config)
            return
        }

        val wifiNetworkSpecifier: WifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(configSSID.toString())
            .build()

        val request: NetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(wifiNetworkSpecifier)
            .build()

        log.info { "Requesting to connect to $configSSID" }
        connectivityManager.requestNetwork(request, networkCallbacks)
    }

    @MainThread
    override fun stop() {
        decoratingClient.wrappedClient = null
        clearState()
    }

    private fun buildNetworkCallbacks(
        config: WifiConfiguration
    ): ConnectivityManager.NetworkCallback {
        return object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                log.info { "onAvailable: $network" }
                decoratingClient.onApConnectionSuccessful(config)
            }

            override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                log.info { "onBlockedStatusChanged: $network, blocked=$blocked" }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                log.info { "onCapabilitiesChanged: $network, caps: $networkCapabilities" }
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                log.info { "onLinkPropertiesChanged: $network" }
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                log.info { "onLosing: $network" }
            }

            override fun onLost(network: Network) {
                log.info { "onLost: $network" }
            }

            override fun onUnavailable() {
                log.info { "onUnavailable" }
            }
        }
    }

    private fun clearState() {
        try {
            log.info { "clearState()" }
            log.error { "NEED TO CLEAR THESE CALLBACKS!" }
            // TODO: clear these callbacks once we're managing this correctly
//            connectivityManager.unregisterNetworkCallback(networkCallbacks)
        } catch (ex: IllegalArgumentException) {
            // don't worry if we weren't registered.
        }
    }

}
