package io.particle.mesh.setup.flow.context

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.ScanNetworksReply
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.ScanNetworksReply.Network
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.logged
import io.particle.mesh.setup.flow.Clearable
import mu.KotlinLogging


class WifiContext : Clearable {

    private val log = KotlinLogging.logger {}

    val targetWifiNetworkLD: LiveData<Network?> = MutableLiveData()
    val targetWifiNetworkPasswordLD: LiveData<String?> = MutableLiveData()
    val targetWifiNetworkJoinedLD: LiveData<Boolean?> = MutableLiveData()

    var connectingToCloudUiShown by log.logged(false)

    override fun clearState() {
        connectingToCloudUiShown = false

        val setToNulls = listOf(
            targetWifiNetworkLD,
            targetWifiNetworkPasswordLD,
            targetWifiNetworkJoinedLD
        )
        for (ld in setToNulls) {
            ld.castAndPost(null)
        }
    }

    fun updateTargetWifiNetwork(network: ScanNetworksReply.Network) {
        log.info { "updateTargetWifiNetwork(): $network" }
        targetWifiNetworkLD.castAndPost(network)
    }

    fun updateTargetWifiNetworkPassword(password: String?) {
        val pwdString = if (password == null) "(null)" else "[hidden]"
        log.info { "updateTargetWifiNetworkPassword(): $pwdString" }
        targetWifiNetworkPasswordLD.castAndPost(password)
    }

    fun updateTargetWifiNetworkJoined(joined: Boolean) {
        log.info { "updateTargetWifiNetworkJoined(): $joined" }
        targetWifiNetworkJoinedLD.castAndPost(joined)
    }

}