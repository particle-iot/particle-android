package io.particle.mesh.setup.flow

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleEventVisibility
import io.particle.firmwareprotos.ctrl.Network.InterfaceEntry
import io.particle.firmwareprotos.ctrl.Network.InterfaceType
import io.particle.mesh.R
import io.particle.mesh.common.Result
import io.particle.mesh.setup.flow.modules.bleconnection.BLEConnectionModule
import io.particle.mesh.setup.flow.modules.cloudconnection.CloudConnectionModule
import io.particle.mesh.setup.flow.modules.device.DeviceModule
import io.particle.mesh.setup.flow.modules.device.NetworkSetupType
import io.particle.mesh.setup.flow.modules.device.NetworkSetupType.AS_GATEWAY
import io.particle.mesh.setup.flow.modules.device.NetworkSetupType.JOINER
import io.particle.mesh.setup.flow.modules.device.NetworkSetupType.STANDALONE
import io.particle.mesh.setup.flow.modules.meshsetup.MeshNetworkToJoin.CreateNewNetwork
import io.particle.mesh.setup.flow.modules.meshsetup.MeshNetworkToJoin.SelectedNetwork
import io.particle.mesh.setup.flow.modules.meshsetup.MeshSetupModule
import kotlinx.coroutines.delay
import mu.KotlinLogging
import java.util.concurrent.TimeUnit


enum class Gen3ConnectivityType {
    WIFI,
    CELLULAR,
    MESH_ONLY
}


class Flow(
    private val flowManager: FlowManager,
    private val bleConnModule: BLEConnectionModule,
    private val meshSetupModule: MeshSetupModule,
    private val cloudConnModule: CloudConnectionModule,
    private val deviceModule: DeviceModule,
    private val cloud: ParticleCloud
) {

    private val targetXceiver
        get() = bleConnModule.targetDeviceTransceiverLD.value

    private val log = KotlinLogging.logger {}

    suspend fun runFlow() {
        log.debug { "runFlow()" }

        doInitialCommonSubflow()

        val interfaceList = ensureGetInterfaceList()
        val hasEthernet = null != interfaceList.firstOrNull { it.type == InterfaceType.ETHERNET }

        if (hasEthernet) {
            doEthernetSubflow()
        } else {
            if (flowManager.targetDeviceType != Gen3ConnectivityType.MESH_ONLY) {
                meshSetupModule.showNewNetworkOptionInScanner = true
            }
            val flowType = getFlowType()
            when (flowType) {
                Gen3ConnectivityType.WIFI -> doArgonFlow()
                Gen3ConnectivityType.CELLULAR -> doBoronFlow()
                Gen3ConnectivityType.MESH_ONLY -> doJoinerSubflow()
            }
        }

        cloud.publishEvent(
            "mesh-device-setup-complete",
            bleConnModule.targetDeviceId!!,
            ParticleEventVisibility.PRIVATE,
            TimeUnit.HOURS.toSeconds(1).toInt()
        )
    }

    private suspend fun getFlowType(): Gen3ConnectivityType {
        if (flowManager.targetDeviceType == Gen3ConnectivityType.MESH_ONLY) {
            return Gen3ConnectivityType.MESH_ONLY
        }

        deviceModule.ensureNetworkSetupTypeCaptured()
        return if (deviceModule.networkSetupTypeLD.value!! == NetworkSetupType.JOINER) {
            Gen3ConnectivityType.MESH_ONLY
        } else {
            meshSetupModule.showNewNetworkOptionInScanner = true
            flowManager.targetDeviceType
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

    private suspend fun doBoronFlow() {
        deviceModule.ensureNetworkSetupTypeCaptured()

        try {
            flowManager.showGlobalProgressSpinner(true)
            cloudConnModule.ensureCardOnFile()
            cloudConnModule.boronSteps.ensureIccidRetrieved()
            cloudConnModule.boronSteps.ensureSimActivationStatusUpdated()
            cloudConnModule.ensurePricingImpactRetrieved()
        } finally {
            flowManager.showGlobalProgressSpinner(false)
        }

        cloudConnModule.ensureShowPricingImpactUI()
        cloudConnModule.ensureShowConnectToDeviceCloudConfirmation()

        cloudConnModule.boronSteps.ensureConnectingToCloudUiShown()
        cloudConnModule.boronSteps.ensureSimActivated()
        ensureTargetDeviceSetSetupDone(true)
        bleConnModule.ensureListeningStoppedForBothDevices()
        cloudConnModule.ensureConnectedToCloud()
        cloudConnModule.ensureTargetDeviceClaimedByUser()
        cloudConnModule.ensureConnectedToCloudSuccessUi()
        cloudConnModule.ensureTargetDeviceIsNamed()

        val setupType = deviceModule.networkSetupTypeLD.value!!
        when (setupType) {
            NetworkSetupType.AS_GATEWAY -> doCreateNetworkFlow()
            NetworkSetupType.STANDALONE -> deviceModule.ensureShowLetsGetBuildingUi()
        }
    }

    private suspend fun doArgonFlow() {
        deviceModule.ensureNetworkSetupTypeCaptured()

        try {
            flowManager.showGlobalProgressSpinner(true)
//            cloudConnModule.ensureCardOnFile()
            cloudConnModule.ensurePricingImpactRetrieved()
        } finally {
            flowManager.showGlobalProgressSpinner(false)
        }

        cloudConnModule.ensureShowPricingImpactUI()
        cloudConnModule.ensureShowConnectToDeviceCloudConfirmation()
        cloudConnModule.argonSteps.ensureTargetWifiNetworkCaptured()
        cloudConnModule.argonSteps.ensureTargetWifiNetworkPasswordCaptured()

        cloudConnModule.argonSteps.ensureTargetWifiNetworkJoined()
        ensureTargetDeviceSetSetupDone(true)
        bleConnModule.ensureListeningStoppedForBothDevices()

        cloudConnModule.argonSteps.ensureConnectingToCloudUiShown()
        cloudConnModule.ensureConnectedToCloud()
        cloudConnModule.ensureTargetDeviceClaimedByUser()
        cloudConnModule.ensureConnectedToCloudSuccessUi()
        cloudConnModule.ensureTargetDeviceIsNamed()

        when (deviceModule.networkSetupTypeLD.value!!) {
            AS_GATEWAY -> doCreateNetworkFlow()
            STANDALONE -> deviceModule.ensureShowLetsGetBuildingUi()
            JOINER -> throw IllegalStateException("Should not be in a JOINER flow here!")
        }
    }

    private suspend fun doMeshFlowForGatewayDevice() {
        // await the device
        meshSetupModule.ensureMeshNetworkSelected()

        val toJoin = meshSetupModule.targetDeviceMeshNetworkToJoinLD.value!!
        when (toJoin) {
            is SelectedNetwork -> doJoinerSubflow()
            is CreateNewNetwork -> doCreateNetworkFlow()
        }
    }

    private suspend fun doInitialCommonSubflow() {
        // connect to the device
        bleConnModule.ensureBarcodeDataForTargetDevice()
        bleConnModule.ensureTargetDeviceConnected()

        cloudConnModule.ensureClaimCodeFetched()

        // gather initial data, perform upfront checks
        deviceModule.ensureDeviceIsUsingEligibleFirmware(
            bleConnModule.targetDeviceTransceiverLD.value!!,
            flowManager.targetPlatformDeviceType
        )

        deviceModule.ensureEthernetDetectionSet()

        val targetDeviceId = bleConnModule.ensureTargetDeviceId()

        if (!meshSetupModule.targetJoinedSuccessfully) {
            cloudConnModule.ensureCheckedIsClaimed(targetDeviceId)
            meshSetupModule.ensureRemovedFromExistingNetwork()
            cloudConnModule.ensureSetClaimCode()
        }

        bleConnModule.ensureShowTargetPairingSuccessful()
    }

    private suspend fun doEthernetSubflow() {
        log.debug { "doEthernetSubflow()" }
        deviceModule.ensureNetworkSetupTypeCaptured()

        try {
            flowManager.showGlobalProgressSpinner(true)
//            cloudConnModule.ensureCardOnFile()
            cloudConnModule.ensurePricingImpactRetrieved()
        } finally {
            flowManager.showGlobalProgressSpinner(false)
        }

        cloudConnModule.ensureShowPricingImpactUI()

        cloudConnModule.ensureEthernetConnectingToDeviceCloudUiShown()
        ensureTargetDeviceSetSetupDone(true)
        bleConnModule.ensureListeningStoppedForBothDevices()
        delay(3000)  // give it a moment to get an IP
        cloudConnModule.ensureEthernetHasIP()
        cloudConnModule.ensureConnectedToCloud()
        cloudConnModule.ensureTargetDeviceClaimedByUser()
        cloudConnModule.ensureConnectedToCloudSuccessUi()
        cloudConnModule.ensureTargetDeviceIsNamed()

        val setupType = deviceModule.networkSetupTypeLD.value!!
        when (setupType) {
            NetworkSetupType.AS_GATEWAY -> doCreateNetworkFlow()
            NetworkSetupType.STANDALONE -> deviceModule.ensureShowLetsGetBuildingUi()
        }
    }

    private suspend fun doJoinerSubflow() {
        log.debug { "doJoinerSubflow()" }
        meshSetupModule.ensureJoinerVisibleMeshNetworksListPopulated()

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
        meshSetupModule.ensureNewNetworkCreatedOnCloud()
        meshSetupModule.ensureCreateNewNetworkMessageSent()

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
        flowManager.navigate(R.id.action_global_newMeshNetworkFinishedFragment)
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
