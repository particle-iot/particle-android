package io.particle.mesh.setup.flow

import io.particle.firmwareprotos.ctrl.Network.InterfaceEntry
import io.particle.firmwareprotos.ctrl.Network.InterfaceType
import io.particle.mesh.common.Result
import io.particle.mesh.setup.flow.modules.bleconnection.BLEConnectionModule
import io.particle.mesh.setup.flow.modules.cloudconnection.CloudConnectionModule
import io.particle.mesh.setup.flow.modules.meshsetup.MeshNetworkToJoin.CreateNewNetwork
import io.particle.mesh.setup.flow.modules.meshsetup.MeshNetworkToJoin.SelectedNetwork
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

    suspend fun runMeshFlowForGatewayDevice() {
        meshSetupModule.showNewNetworkOptionInScanner = true
        try {
            doMeshFlowForGatewayDevice()
        } catch (ex: Exception) {
            // FIXME: or should error handling happen at the FlowManager level?
            throw ex
        } finally {
            meshSetupModule.showNewNetworkOptionInScanner = false
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

    private suspend fun doMeshFlowForGatewayDevice() {
        // await the device
        meshSetupModule.ensureMeshNetworkSelected()

        val toJoin = meshSetupModule.targetDeviceMeshNetworkToJoinLD.value!!
        when(toJoin) {
            is SelectedNetwork -> doJoinerSubflow()
            is CreateNewNetwork -> doCreateNetworkFlow()
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

        if (!meshSetupModule.targetJoinedSuccessfully) {
            cloudConnModule.ensureCheckedIsClaimed(targetDeviceId)
            cloudConnModule.ensureSetClaimCode()
            meshSetupModule.ensureRemovedFromExistingNetwork()
        }
    }

    private suspend fun doEthernetSubflow() {
        log.debug { "doEthernetSubflow()" }
        bleConnModule.ensureShowTargetPairingSuccessful()

        cloudConnModule.ensureConnectingToDeviceCloudUiShown()
        ensureTargetDeviceSetSetupDone(true)
        bleConnModule.ensureListeningStoppedForBothDevices()
        delay(3000)  // give it a moment to get an IP
        cloudConnModule.ensureEthernetHasIP()
        cloudConnModule.ensureConnectedToCloud()
        cloudConnModule.ensureTargetDeviceClaimedByUser()
        cloudConnModule.ensureTargetDeviceIsNamed()
        ensureShowGatewaySetupFinishedUi()
    }

    private suspend fun doJoinerSubflow() {
        log.debug { "doJoinerSubflow()" }
        meshSetupModule.ensureJoinerVisibleMeshNetworksListPopulated()

        bleConnModule.ensureShowTargetPairingSuccessful()

        meshSetupModule.ensureMeshNetworkSelected()

        bleConnModule.ensureBarcodeDataForComissioner()
        bleConnModule.ensureCommissionerConnected()
        meshSetupModule.ensureCommissionerIsOnNetworkToBeJoined()

        meshSetupModule.ensureTargetMeshNetworkPasswordCollected()
        meshSetupModule.ensureMeshNetworkJoinedUiShown()
        meshSetupModule.ensureMeshNetworkJoined()
        ensureTargetDeviceSetSetupDone(true)
        meshSetupModule.ensureCommissionerStopped()

        bleConnModule.ensureListeningStoppedForBothDevices()
        cloudConnModule.ensureConnectedToCloud()
        cloudConnModule.ensureTargetDeviceClaimedByUser()
        cloudConnModule.ensureTargetDeviceIsNamed()
        ensureShowJoinerSetupFinishedUi()
    }

    private suspend fun doCreateNetworkFlow() {
        meshSetupModule.ensureNewNetworkName()
        meshSetupModule.ensureNewNetworkPassword()

        meshSetupModule.ensureCreatingNewNetworkUiShown()
        meshSetupModule.ensureCreateNewNetworkMessageSent()
        cloudConnModule.ensureConnectedToCloud()
        cloudConnModule.ensureNetworkIsRegisteredWithCloud()

        ensureShowCreateNetworkFinished()
    }

    private suspend fun ensureGetInterfaceList(): List<InterfaceEntry> {
        log.info { "ensureGetInterfaceList()" }
        val ifaceListReply = targetXceiver!!.sendGetInterfaceList().throwOnErrorOrAbsent()
        return ifaceListReply.interfacesList
    }

    private suspend fun ensureTargetDeviceSetSetupDone(done: Boolean) {
        log.info { "ensureTargetDeviceSetSetupDone() done=$done" }
        targetXceiver!!.sendSetDeviceSetupDone(done).throwOnErrorOrAbsent()
    }

    private suspend fun ensureShowJoinerSetupFinishedUi() {
        log.info { "ensureShowJoinerSetupFinishedUi()" }
        flowManager.navigate(R.id.action_global_setupFinishedFragment)
    }

    private suspend fun ensureShowGatewaySetupFinishedUi() {
        log.info { "ensureShowGatewaySetupFinishedUi()" }
        flowManager.navigate(R.id.action_global_gatewaySetupFinishedFragment)
    }

    private suspend fun ensureShowCreateNetworkFinished() {
        log.info { "ensureShowCreateNetworkFinished()" }
        flowManager.navigate(R.id.action_global_newMeshNetworkFinishedFragment)    }
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
