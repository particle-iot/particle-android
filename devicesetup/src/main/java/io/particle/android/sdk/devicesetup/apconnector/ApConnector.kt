package io.particle.android.sdk.devicesetup.apconnector

import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiConfiguration.KeyMgmt
import io.particle.android.sdk.devicesetup.apconnector.ApConnector.Client
import io.particle.android.sdk.utils.SSID
import java.util.concurrent.TimeUnit.SECONDS

interface ApConnector {

    interface Client {
        fun onApConnectionSuccessful(config: WifiConfiguration)
        fun onApConnectionFailed(config: WifiConfiguration)
    }

    /**
     * Connect this Android device to the specified AP.
     *
     * @param config the WifiConfiguration defining which AP to connect to
     */
    fun connectToAP(config: WifiConfiguration, client: Client)

    /**
     * Stop attempting to connect
     */
    fun stop()

    companion object {

        @JvmStatic
        fun buildUnsecuredConfig(ssid: SSID): WifiConfiguration {
            val config = WifiConfiguration()
            config.SSID = ssid.inQuotes()
            config.hiddenSSID = false
            config.allowedKeyManagement.set(KeyMgmt.NONE)
            // have to set a very high number in order to ensure that Android doesn't
            // immediately drop this connection and reconnect to the a different AP
            config.priority = 999999
            return config
        }

        @JvmField
        val CONNECT_TO_DEVICE_TIMEOUT_MILLIS = SECONDS.toMillis(20)
    }
}


class DecoratingClient(
    private val onClearState: () -> Unit
) : Client {

    var wrappedClient: Client? = null

    override fun onApConnectionSuccessful(config: WifiConfiguration) {
        onClearState()
        wrappedClient?.onApConnectionSuccessful(config)
    }

    override fun onApConnectionFailed(config: WifiConfiguration) {
        onClearState()
        wrappedClient?.onApConnectionFailed(config)
    }

}
