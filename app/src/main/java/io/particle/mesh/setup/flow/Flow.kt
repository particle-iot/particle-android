package io.particle.mesh.setup.flow

import androidx.navigation.ui.NavigationUI
import io.particle.firmwareprotos.ctrl.Network.InterfaceEntry
import io.particle.firmwareprotos.ctrl.Network.InterfaceType
import io.particle.mesh.common.Result
import io.particle.mesh.setup.flow.modules.bleconnection.BLEConnectionModule
import io.particle.mesh.setup.flow.modules.cloudconnection.CloudConnectionModule
import io.particle.mesh.setup.flow.modules.meshsetup.MeshSetupModule
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.delay
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

        doInitialCommonSubflow()

        // check interfaces!
        val interfaceList = ensureGetInterfaceList()
        val hasEthernet = null != interfaceList.firstOrNull { it.type == InterfaceType.ETHERNET }

        if (hasEthernet) {
            doEthernetSubflow()

        } else {
            doJoinerSubflow()
        }
    }

    private suspend fun doInitialCommonSubflow() {
        cloudConnModule.ensureClaimCodeFetched()

        // connect to the device
        bleConnModule.ensureBarcodeDataForTargetDevice()
        bleConnModule.ensureTargetDeviceConnected()

        // gather initial data, perform upfront checks
        cloudConnModule.ensureDeviceIsUsingEligibleFirmware()
        val targetDeviceId = bleConnModule.ensureTargetDeviceId()
        cloudConnModule.ensureCheckedIsClaimed(targetDeviceId)
    }

    private suspend fun doEthernetSubflow() {
        log.debug { "doEthernetSubflow()" }

        // FIXME: UNCOMMENT?  Seems like that won't work; review later.
//        cloudConnModule.ensureEthernetHasIP()

        cloudConnModule.ensureSetClaimCode()
        meshSetupModule.ensureRemovedFromExistingNetwork()
        bleConnModule.ensureShowPairingSuccessful()

        // FIXME: show "check gateway" UI here!
        cloudConnModule.ensureCheckGatewayUiShown()
        // FIXME: UNCOMMENT?  Why are we doing this in so many places?
//        cloudConnModule.ensureEthernetHasIP()
        cloudConnModule.ensureConnectingToDeviceCloudUiShown()

        // FIXME: remove all unnecessary delays here



        bleConnModule.ensureListeningStoppedForBothDevices()
        delay(3000)

        // FIXME: doing this here is technically against the spec but may be required.
        cloudConnModule.ensureEthernetHasIP()
        delay(3000)

        cloudConnModule.ensureEthernetConnectedToCloud()
        delay(3000)
        cloudConnModule.ensureTargetDeviceClaimedByUser()
        cloudConnModule.ensureTargetDeviceIsNamed()
        ensureShowGatewaySetupFinishedUi()
    }

    private suspend fun doJoinerSubflow() {
        log.debug { "doJoinerSubflow()" }
        cloudConnModule.ensureSetClaimCode()
        meshSetupModule.ensureRemovedFromExistingNetwork()
        meshSetupModule.ensureJoinerVisibleMeshNetworksListPopulated()

        bleConnModule.ensureShowPairingSuccessful()

        meshSetupModule.ensureMeshNetworkSelected()

        bleConnModule.ensureBarcodeDataForComissioner()
        bleConnModule.ensureCommissionerConnected()
        meshSetupModule.ensureCommissionerIsOnNetworkToBeJoined()

        meshSetupModule.ensureTargetMeshNetworkPasswordCollected()

        // FIXME: INSERT BILLING SCREEN SUPPORT!

        meshSetupModule.ensureMeshNetworkJoinedUiShown()
        meshSetupModule.ensureMeshNetworkJoined()
        meshSetupModule.ensureCommissionerStopped()

        bleConnModule.ensureListeningStoppedForBothDevices()
        cloudConnModule.ensureTargetDeviceClaimedByUser()
        ensureTargetDeviceSetSetupDone()
        ensureShowJoinerSetupFinishedUi()
    }

    private suspend fun ensureGetInterfaceList(): List<InterfaceEntry> {
        log.info { "ensureGetInterfaceList()" }
        val ifaceListReply = targetXceiver!!.sendGetInterfaceList().throwOnErrorOrAbsent()
        return ifaceListReply.interfacesList
    }

    private suspend fun ensureTargetDeviceSetSetupDone() {
        log.info { "ensureTargetDeviceSetSetupDone()" }
        targetXceiver!!.sendSetDeviceSetupDone().throwOnErrorOrAbsent()
    }

    private suspend fun ensureShowJoinerSetupFinishedUi() {
        log.info { "ensureShowJoinerSetupFinishedUi()" }
        flowManager.navigate(R.id.action_global_setupFinishedFragment)
    }

    private suspend fun ensureShowGatewaySetupFinishedUi() {
        log.info { "ensureShowGatewaySetupFinishedUi()" }
        flowManager.navigate(R.id.action_global_gatewaySetupFinishedFragment)
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
