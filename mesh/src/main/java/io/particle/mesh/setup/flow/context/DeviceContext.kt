package io.particle.mesh.setup.flow.context

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.common.logged
import io.particle.mesh.setup.flow.Clearable
import mu.KotlinLogging


enum class NetworkSetupType {
    AS_GATEWAY,
    STANDALONE,
    NODE_JOINER,
//    GATEWAY_JOINER,
//    INTERNET_CONNECTED_JOINER,
//    CONTINUED_JOINER
}


class DeviceContext : Clearable {

    private val log = KotlinLogging.logger {}

    val userConsentedToFirmwareUpdateLD: LiveData<Boolean?> = MutableLiveData()
    val bleOtaProgress: LiveData<Int?> = MutableLiveData()
    val networkSetupTypeLD: LiveData<NetworkSetupType?> = MutableLiveData()

    var shouldDetectEthernet by log.logged(false)
    var isDetectEthernetSent by log.logged(false)
    var firmwareUpdateCount by log.logged(1)

    override fun clearState() {
        shouldDetectEthernet = false
        isDetectEthernetSent = false
        firmwareUpdateCount = 1

        bleOtaProgress.castAndSetOnMainThread(0)

        val setToNulls = listOf(
            userConsentedToFirmwareUpdateLD,
            networkSetupTypeLD
        )
        for (ld in setToNulls) {
            ld.castAndPost(null)
        }
    }

    fun updateUserConsentedToFirmwareUpdate(didConsent: Boolean) {
        log.info { "updateUserConsentedToFirmwareUpdate(): $didConsent" }
        userConsentedToFirmwareUpdateLD.castAndPost(didConsent)
    }

    fun updateBleOtaProgress(progressPercent: Int) {
        log.info { "updateBleOtaProgress(): $progressPercent" }
        bleOtaProgress.castAndPost(progressPercent)
    }

    internal fun updateNetworkSetupType(networkSetupType: NetworkSetupType) {
        log.info { "updateNetworkSetupType(): $networkSetupType" }
        networkSetupTypeLD.castAndPost(networkSetupType)
    }

}