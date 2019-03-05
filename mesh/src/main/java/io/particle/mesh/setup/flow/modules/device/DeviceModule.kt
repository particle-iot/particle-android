package io.particle.mesh.setup.flow.modules.device

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.firmwareprotos.ctrl.Config.DeviceMode
import io.particle.firmwareprotos.ctrl.Config.Feature
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.ota.FirmwareUpdateManager
import io.particle.mesh.ota.FirmwareUpdateResult
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.flow.*
import io.particle.mesh.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KotlinLogging


internal enum class NetworkSetupType {
    AS_GATEWAY,
    STANDALONE,
    JOINER
}

class DeviceModule(
    private val flowManager: FlowManager,
    private val firmwareUpdateManager: FirmwareUpdateManager
) : Clearable {

    private val log = KotlinLogging.logger {}

    val userConsentedToFirmwareUpdateLD: LiveData<Boolean?> = MutableLiveData()
    val bleUpdateProgress: LiveData<Int?> = MutableLiveData()
    internal val networkSetupTypeLD: LiveData<NetworkSetupType?> = MutableLiveData()

    var firmwareUpdateCount = 1
        private set

    private var hasLatestFirmware = false
    private var shouldDetectEthernet = false
    private var hasSetDetectEthernet = false

    override fun clearState() {
        hasLatestFirmware = false
        shouldDetectEthernet = false
        hasSetDetectEthernet = false
        firmwareUpdateCount = 1

        bleUpdateProgress.castAndSetOnMainThread(0)

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

    fun updateShouldDetectEthernet(shouldDetect: Boolean) {
        log.info { "updateShouldDetectEthernet(): $shouldDetect" }
        shouldDetectEthernet = shouldDetect
    }

    internal fun updateNetworkSetupType(networkSetupType: NetworkSetupType) {
        log.info { "updateNetworkSetupType(): $networkSetupType" }
        networkSetupTypeLD.castAndPost(networkSetupType)
    }

    suspend fun ensureDeviceIsUsingEligibleFirmware(
        xceiver: ProtocolTransceiver,
        deviceType: ParticleDeviceType
    ) {
        log.info { "ensureDeviceIsUsingEligibleFirmware()" }

        if (hasLatestFirmware) {
            log.info { "Already checked device for latest firmware; skipping" }
            flowManager.showGlobalProgressSpinner(false)
            return
        }

        val needsUpdate = firmwareUpdateManager.needsUpdate(xceiver, deviceType)
        if (!needsUpdate) {
            log.debug { "No firmware update needed!" }
            hasLatestFirmware = true
            return
        }

        val suspender = liveDataSuspender({ userConsentedToFirmwareUpdateLD.nonNull() })
        withContext(Dispatchers.Main) {
            flowManager.navigate(R.id.action_global_bleOtaIntroFragment)
            suspender.awaitResult()
        }

        bleUpdateProgress.castAndSetOnMainThread(0)
        flowManager.navigate(R.id.action_global_bleOtaFragment)

        firmwareUpdateManager.startUpdateIfNecessary(xceiver, deviceType) {
            bleUpdateProgress.castAndPost(it)
        }
        flowManager.showGlobalProgressSpinner(true)
        firmwareUpdateCount++
    }

    suspend fun ensureNetworkSetupTypeCaptured() {
        log.info { "ensureNetworkSetupTypeCaptured()" }
        if (networkSetupTypeLD.value != null) {
            return
        }

        val suspender = liveDataSuspender({ networkSetupTypeLD.nonNull() })
        withContext(Dispatchers.Main) {
            flowManager.navigate(R.id.action_global_useStandaloneOrInMeshFragment)
            suspender.awaitResult()
        }
    }

    suspend fun ensureEthernetDetectionSet() {
        log.info { "ensureEthernetDetectionSet()" }
        if (!shouldDetectEthernet || hasSetDetectEthernet) {
            return
        }

        val target = flowManager.bleConnectionModule.targetDeviceTransceiverLD.value!!
        target.sendSetFeature(Feature.ETHERNET_DETECTION, true).throwOnErrorOrAbsent()
        hasSetDetectEthernet = true
        target.sendStartupMode(DeviceMode.LISTENING_MODE).throwOnErrorOrAbsent()
        target.sendReset().throwOnErrorOrAbsent()

        delay(1000)
        target.disconnect()
        delay(4000)

        throw FlowException(
            "Resetting device to enable ethernet detection!",
            ExceptionType.EXPECTED_FLOW
        )
    }

    suspend fun ensureShowLetsGetBuildingUi() {
        log.info { "ensureShowLetsGetBuildingUi()" }
        flowManager.navigate(R.id.action_global_letsGetBuildingFragment)
    }

}