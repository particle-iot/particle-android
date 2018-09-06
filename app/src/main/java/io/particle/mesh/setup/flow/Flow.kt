package io.particle.mesh.setup.flow

import io.particle.firmwareprotos.ctrl.Network
import io.particle.firmwareprotos.ctrl.Network.InterfaceEntry
import io.particle.firmwareprotos.ctrl.Network.InterfaceType
import io.particle.mesh.common.Result
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.setup.flow.modules.bleconnection.BLEConnectionModule
import io.particle.mesh.setup.flow.modules.cloudconnection.CloudConnectionModule
import io.particle.mesh.setup.flow.modules.meshsetup.MeshSetupModule
import io.particle.mesh.setup.ui.DialogSpec
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.withContext
import mu.KotlinLogging


class Flow(
        private val flowManager: FlowManager,
        private val bleConnModule: BLEConnectionModule,
        private val meshSetupModule: MeshSetupModule,
        private val cloudConnModule: CloudConnectionModule
) {

    private val targetXceiver
        get() = bleConnModule.targetDeviceTransceiverLD.value

    private val log = KotlinLogging.logger {}

    suspend fun runFlow() {
        try {
            doRunFlow()
        } catch (ex: Exception) {
            // FIXME: or should error handling happen at the FlowManager level?
            throw ex
        }
    }

    private suspend fun doRunFlow() {
        log.debug { "doRunFlow()" }

        cloudConnModule.ensureClaimCodeFetched()

        // connect to the device
        bleConnModule.ensureBarcodeDataForTargetDevice()
        bleConnModule.ensureTargetDeviceConnected()

        // gather initial data, perform upfront checks
        cloudConnModule.ensureDeviceIsUsingEligibleFirmware()
        val targetDeviceId = bleConnModule.ensureTargetDeviceId()
        cloudConnModule.ensureCheckedIsClaimed(targetDeviceId)

        // check interfaces!
        val interfaceList = ensureGetInterfaceList()
        val hasEthernet = null != interfaceList.firstOrNull { it.type == InterfaceType.ETHERNET }
        cloudConnModule.ensureSetClaimCode()
        meshSetupModule.ensureRemovedFromExistingNetwork()
        meshSetupModule.ensureResetNetworkCredentials()

        if (hasEthernet) {
            doEthernetFlow()

        } else {
            doMeshSetupFlow()
        }

        cloudConnModule.ensureTargetDeviceClaimedByUser()
        ensureTargetDeviceSetSetupDone()  // FIXME: should this move elsewhere...?
        cloudConnModule.ensureTargetDeviceIsNamed()

        // TODO: review sub-steps in "FINISH" step of SDD

        ensureShowSetupFinishedUi()  // FIXME: should this move elsewhere...?
    }

    private suspend fun doEthernetFlow() {
        log.debug { "doEthernetFlow()" }
        targetXceiver!!.sendStopListeningMode()
        delay(500)
        cloudConnModule.ensureSetClaimCode()
        delay(500)
        cloudConnModule.ensureEthernetIsPluggedIn()
        delay(500)
        cloudConnModule.ensureEthernetConnectedToCloud()
        delay(2500)
    }

    private suspend fun doMeshSetupFlow() {
        log.debug { "doMeshSetupFlow()" }
        meshSetupModule.ensureJoinerVisibleMeshNetworksListPopulated()
        bleConnModule.ensureShowPairingSuccessful()
        meshSetupModule.ensureMeshNetworkSelected()

        bleConnModule.ensureBarcodeDataForComissioner()
        bleConnModule.ensureCommissionerConnected()

        meshSetupModule.ensureTargetMeshNetworkPasswordCollected()
        meshSetupModule.ensureMeshNetworkJoinedShown()
        meshSetupModule.ensureMeshNetworkJoined()
    }

    suspend fun ensureGetInterfaceList(): List<InterfaceEntry> {
        val ifaceListReply = targetXceiver!!.sendGetInterfaceList().throwOnErrorOrAbsent()
        return ifaceListReply.interfacesList
    }

    private suspend fun ensureTargetDeviceSetSetupDone() {
        targetXceiver!!.sendSetDeviceSetupDone().throwOnErrorOrAbsent()
    }

    private suspend fun ensureShowSetupFinishedUi() {
        flowManager.navigate(R.id.action_global_setupFinishedFragment)
    }
}


private val log = KotlinLogging.logger {}

// FIXME: feels like this belongs elsewhere.
fun <V, E> Result<V, E>.throwOnErrorOrAbsent(): V {
    return when (this) {
        is Result.Error,
        is Result.Absent -> {
            val msg = "Error making request: ${this.error}"
            log.error { msg }
            throw FlowException(msg)
        }
        is Result.Present -> this.value
    }
}


class FlowException(msg: String = "") : Exception(msg) {
    // FIXME: give this extra data
}

