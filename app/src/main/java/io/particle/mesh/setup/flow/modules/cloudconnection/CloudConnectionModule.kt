package io.particle.mesh.setup.flow.modules.cloudconnection

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.firmwareprotos.ctrl.cloud.Cloud.ConnectionStatus
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
import io.particle.mesh.setup.flow.FlowException
import io.particle.mesh.setup.flow.FlowManager
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent
import io.particle.sdk.app.R.id
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.withContext
import mu.KotlinLogging


class CloudConnectionModule(
        private val flowManager: FlowManager,
        private val cloud: ParticleCloud
) {

    private val log = KotlinLogging.logger {}

    var claimCode: String? = null // FIXME: make this a LiveData?  Where is it used?
    val targetDeviceShouldBeClaimedLD: LiveData<Boolean?> = MutableLiveData()
    val targetOwnedByUserLD: LiveData<Boolean?> = MutableLiveData()
    val targetDeviceNameToAssignLD: LiveData<String?> = MutableLiveData()
    val isTargetDeviceNamedLD: LiveData<Boolean?> = MutableLiveData()

    private var checkedIsTargetClaimedByUser = false
    private var connectedToMeshNetworkAndOwnedUiShown = false

    private val targetXceiver
        get() = flowManager.bleConnectionModule.targetDeviceTransceiverLD.value


    fun clearState() {
        TODO("IMPLEMENT ME")
    }

    fun updateTargetOwnedByUser(owned: Boolean) {
        (targetOwnedByUserLD as MutableLiveData).postValue(owned)
    }

    fun updateIsTargetDeviceNamed(named: Boolean) {
        (isTargetDeviceNamedLD as MutableLiveData).postValue(named)
    }

    fun updateTargetDeviceNameToAssign(name: String) {
        (targetDeviceNameToAssignLD as MutableLiveData).postValue(name)
    }

    suspend fun ensureClaimCodeFetched() {
        if (claimCode == null) {
            log.info { "Fetching new claim code" }
            claimCode = cloud.generateClaimCode().claimCode
        }
    }

    suspend fun ensureEthernetConnected() {
        for (i in 0..14) { // 30 seconds
            delay(500)
            val statusReply = targetXceiver!!.sendGetConnectionStatus().throwOnErrorOrAbsent()
            if (statusReply.status == ConnectionStatus.CONNECTED) {
                return
            }
        }
        throw FlowException()
    }

    // FIXME: where does this belong?
    suspend fun ensureDeviceIsUsingEligibleFirmware() {
        // TODO: TO BE IMPLEMENTED
    }

    suspend fun ensureCheckedIsClaimed(targetDeviceId: String) {
        if (checkedIsTargetClaimedByUser) {
            return
        }

        val userOwnsDevice = cloud.userOwnsDevice(targetDeviceId)
        checkedIsTargetClaimedByUser = true
        if (userOwnsDevice) {
            return
        }

        // FIXME: FINISH IMPLEMENTING!
        // show dialog
//        val ldSuspender = liveDataSuspender({ flowManager.targetDeviceShouldBeClaimedLD })
//        val shouldClaim = withContext(UI) {
//            flowManager.navigate()
//            ldSuspender.awaitResult()
//        }
    }

    suspend fun ensureSetClaimCode() {
        if (!targetDeviceShouldBeClaimedLD.value!!) {
            return
        }

        targetXceiver!!.sendSetClaimCode(claimCode!!).throwOnErrorOrAbsent()
    }

    suspend fun ensureTargetDeviceClaimedByUser() {
        if (targetOwnedByUserLD.value.truthy()) {
            return
        }

        suspend fun pollDevicesForNewDevice(deviceId: String): Boolean {
            val idLower = deviceId.toLowerCase()
            for (i in 0..14) { // 30 seconds
                // FIXME: what should the timing be here?
                delay(500)
                val userOwnsDevice = try {
                    cloud.userOwnsDevice(idLower)
                } catch (ex: Exception) {
                    false
                }
                if (userOwnsDevice) {
                    log.info { "Found assigned to user device with ID $deviceId" }
                    return true
                }
                log.info { "No device with ID $deviceId found yet assigned to user" }
            }
            log.warn { "Timed out waiting for user to own a device with ID $deviceId" }
            return false
        }

        // FIXME: is this how we want to allow access to things like the target's device ID?
        val targetDeviceId = flowManager.bleConnectionModule.ensureTargetDeviceId()
        val isInList = pollDevicesForNewDevice(targetDeviceId)
        if (!isInList) {
            throw FlowException()
        }

        updateTargetOwnedByUser(true)

        if (!connectedToMeshNetworkAndOwnedUiShown) {
            delay(2000)
            connectedToMeshNetworkAndOwnedUiShown = true
        }
    }

    suspend fun ensureTargetDeviceIsNamed() {
        if (isTargetDeviceNamedLD.value.truthy()) {
            return
        }

        val ldSuspender = liveDataSuspender({ targetDeviceNameToAssignLD })
        val nameToAssign = withContext(UI) {
            flowManager.navigate(id.action_global_nameYourDeviceFragment)
            ldSuspender.awaitResult()
        }

        if (nameToAssign == null) {
            throw FlowException()
        }

        val targetDeviceId = flowManager.bleConnectionModule.ensureTargetDeviceId()
        val joiner = cloud.getDevice(targetDeviceId)
        joiner.setName(nameToAssign)

        updateIsTargetDeviceNamed(true)
    }



}
