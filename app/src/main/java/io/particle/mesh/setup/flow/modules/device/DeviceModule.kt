package io.particle.mesh.setup.flow.modules.device

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.ota.FirmwareUpdateManager
import io.particle.mesh.ota.FirmwareUpdateResult
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.flow.Clearable
import io.particle.mesh.setup.flow.FlowManager
import io.particle.mesh.setup.flow.MeshDeviceType
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.withContext
import mu.KotlinLogging


// FIXME: where does this belong?
internal enum class NetworkSetupType {
    AS_GATEWAY,
    STANDALONE
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

    override fun clearState() {
        hasLatestFirmware = false
        firmwareUpdateCount = 1
        userConsentedToFirmwareUpdateLD.castAndSetOnMainThread(null)
        bleUpdateProgress.castAndSetOnMainThread(0)
        networkSetupTypeLD.castAndSetOnMainThread(null)
    }

    fun updateUserConsentedToFirmwareUpdate(didConsent: Boolean) {
        log.info { "updateUserConsentedToFirmwareUpdate(): $didConsent" }
        userConsentedToFirmwareUpdateLD.castAndPost(didConsent)
    }

    internal fun updateNetworkSetupType(networkSetupType: NetworkSetupType) {
        log.info { "updateNetworkSetupType(): $networkSetupType" }
        networkSetupTypeLD.castAndPost(networkSetupType)
    }

    suspend fun ensureDeviceIsUsingEligibleFirmware(
        xceiver: ProtocolTransceiver,
        deviceType: MeshDeviceType
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
        withContext(UI) {
            flowManager.navigate(R.id.action_global_bleOtaIntroFragment)
            suspender.awaitResult()
        }

        bleUpdateProgress.castAndSetOnMainThread(0)
        flowManager.navigate(R.id.action_global_bleOtaFragment)



        // FIXME: RE-ENABLE!



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
        withContext(UI) {
            flowManager.navigate(R.id.action_global_useStandaloneOrInMeshFragment)
            suspender.awaitResult()
        }
    }

    suspend fun ensureShowLetsGetBuildingUi() {
        log.info { "ensureShowLetsGetBuildingUi()" }
        flowManager.navigate(R.id.action_global_letsGetBuildingFragment)
    }


}