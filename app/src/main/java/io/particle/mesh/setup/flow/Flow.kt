package io.particle.mesh.setup.flow

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.firmwareprotos.ctrl.Common.ResultCode
import io.particle.firmwareprotos.ctrl.Common.ResultCode.NOT_FOUND
import io.particle.firmwareprotos.ctrl.Network.InterfaceEntry
import io.particle.firmwareprotos.ctrl.Network.InterfaceType
import io.particle.firmwareprotos.ctrl.mesh.Mesh.GetNetworkInfoReply
import io.particle.mesh.common.QATool
import io.particle.mesh.common.Result
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.flow.modules.cloudconnection.CloudConnectionModule
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import mu.KotlinLogging


class Flow(
        private val flowManager: FlowManager,
        private val cloud: ParticleCloud
) {

    private val cloudConnModule = CloudConnectionModule(cloud)
    private var connectingToTargetUiShown = false
    private var checkedIsTargetClaimedByUser = false
    private var shownTargetInitialIsConnectedScreen = false
    private var connectedToMeshNetworkAndOwnedUiShown = false

    // FIXME: these feel like it should live elsewhere
    private var targetDeviceId: String? = null

    private val targetXceiver
        get() = flowManager.targetDeviceTransceiverLD.value

    private val log = KotlinLogging.logger {}

    fun runFlow() {
        launch {
            try {
                doRunFlow()
            } catch (ex: Exception) {
                TODO("HAND OFF TO ERROR HANDLER!")
            }
        }
    }

    fun clearState() {
        // FIXME: implement!
    }

    private suspend fun doRunFlow() {
        ensureClaimCodeFetched()

        // connect to the device
        ensureBarcodeDataForTargetDevice()
        ensureTargetDeviceConnected()

        // gather initial data, perform upfront checks
        ensureDeviceIsUsingEligibleFirmware()
        ensureTargetDeviceId()
        ensureCheckedIsClaimed()

        // check interfaces!
        val interfaceList = ensureGetInterfaceList()
        val hasEthernet = null != interfaceList.firstOrNull { it.type == InterfaceType.ETHERNET }
        ensureSetClaimCode()
        ensureRemovedFromExistingNetwork()
        ensureResetNetworkCredentials()

        if (hasEthernet) {
            TODO("FEATHERWING FLOW!")
        } else {
            ensureJoinerVisibleMeshNetworksListPopulated()
            ensureShowPairingSuccessful()
            ensureMeshNetworkSelected()

            ensureBarcodeDataForComissioner()
            ensureCommissionerConnected()

            ensureTargetMeshNetworkPasswordCollected()
            ensureMeshNetworkJoinedShown()
            ensureMeshNetworkJoined()

            ensureTargetDeviceClaimedByUser()
            ensureTargetDeviceSetSetupDone()
            ensureTargetDeviceIsNamed()
            // TODO: review sub-steps in "FINISH" step of SDD
        }

        ensureShowSetupFinishedUi()
    }

    private fun ensureClaimCodeFetched() {
        if (cloudConnModule.claimCode == null) {
            cloudConnModule.fetchClaimCode()
        }
    }

    private suspend fun ensureBarcodeDataForTargetDevice() {
        log.info { "ensureBarcodeDataForTargetDevice()" }
        if (flowManager.targetDeviceBarcodeLD.value != null) {
            return
        }

        val liveDataSuspender = liveDataSuspender({ flowManager.targetDeviceBarcodeLD })
        val barcodeData = withContext(UI) {
            flowManager.navigate(R.id.action_global_getReadyForSetupFragment)
            liveDataSuspender.awaitResult()
        }

        if (barcodeData == null) {
            throw FlowException()
        }
    }

    private suspend fun ensureTargetDeviceConnected() {
        if (targetXceiver != null && targetXceiver!!.isConnected) {
            return
        }

        // FIXME: don't track showing the UI, track if we've gathered the data from the UI.
        if (flowManager.targetDeviceConnectedLD.value != null) {
            flowManager.navigate(R.id.action_global_BLEPairingProgressFragment)
            connectingToTargetUiShown = true
        }

        val ldSuspender = liveDataSuspender({ flowManager.targetDeviceTransceiverLD })
        val transceiver = withContext(UI) {
            flowManager.connectTargetDevice()
            ldSuspender.awaitResult()
        }

        if (transceiver == null) {
            throw FlowException()
        }
    }

    private suspend fun ensureDeviceIsUsingEligibleFirmware() {
        // TODO: TO BE IMPLEMENTED
    }

    private suspend fun ensureTargetDeviceId() {
        // get device ID
        val deviceIdReply = targetXceiver!!.sendGetDeviceId().throwOnErrorOrAbsent()
        targetDeviceId = deviceIdReply.id
    }

    private suspend fun ensureCheckedIsClaimed() {
        if (checkedIsTargetClaimedByUser) {
            return
        }

        val userOwnsDevice = cloud.userOwnsDevice(targetDeviceId!!)
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

    private suspend fun ensureGetInterfaceList(): List<InterfaceEntry> {
        val ifaceListReply = targetXceiver!!.sendGetInterfaceListRequest().throwOnErrorOrAbsent()
        return ifaceListReply.interfacesList
    }

    private suspend fun ensureSetClaimCode() {
        if (!flowManager.targetDeviceShouldBeClaimedLD.value!!) {
            return
        }

        targetXceiver!!.sendSetClaimCode(cloudConnModule.claimCode!!).throwOnErrorOrAbsent()
    }

    private suspend fun ensureRemovedFromExistingNetwork() {
        val reply: Result<GetNetworkInfoReply, ResultCode> = targetXceiver!!.sendGetNetworkInfo()
        when(reply) {
            is Result.Present -> {


                // FIXME: PROMPT USER


                targetXceiver!!.sendLeaveNetwork().throwOnErrorOrAbsent()
            }
            is Result.Absent -> throw FlowException()
            is Result.Error -> {
                if (reply.error == NOT_FOUND) {
                    return
                } else {
                    throw FlowException()
                }
            }
        }
    }

    private suspend fun ensureResetNetworkCredentials() {
        targetXceiver!!.sendResetNetworkCredentialsRequest().throwOnErrorOrAbsent()
    }

    private suspend fun ensureJoinerVisibleMeshNetworksListPopulated() {
        flowManager.targetDeviceVisibleMeshNetworksLD.forceSingleScan()
    }

    private suspend fun ensureShowPairingSuccessful() {
        if (shownTargetInitialIsConnectedScreen) {
            return
        }
        delay(2000)
        shownTargetInitialIsConnectedScreen = true
        flowManager.updateTargetDeviceConnectionInitialized(true)
    }

    private suspend fun ensureMeshNetworkSelected() {
        if (flowManager.targetDeviceMeshNetworkToJoin.value != null) {
            return
        }

        flowManager.navigate(R.id.action_global_scanForMeshNetworksFragment)
        val ldSuspender = liveDataSuspender({ flowManager.targetDeviceMeshNetworkToJoin })
        val meshNetworkToJoin = withContext(UI) {
            ldSuspender.awaitResult()
        }
    }

    private suspend fun ensureBarcodeDataForComissioner() {
        log.info { "ensureBarcodeDataForComissioner()" }
        if (flowManager.commissionerBarcodeLD.value != null) {
            return
        }

        val liveDataSuspender = liveDataSuspender({ flowManager.commissionerBarcodeLD })
        val barcodeData = withContext(UI) {
            flowManager.navigate(R.id.action_global_manualCommissioningAddToNetworkFragment)
            liveDataSuspender.awaitResult()
        }

        if (barcodeData == null) {
            throw FlowException()
        }
    }

    private suspend fun ensureCommissionerConnected() {
        var commissioner = flowManager.commissionerTransceiverLD.value
        if (commissioner != null && commissioner.isConnected) {
            return
        }

        val xceiverSuspender = liveDataSuspender({ flowManager.commissionerTransceiverLD })
        commissioner = withContext(UI) {
            flowManager.connectCommissioner()
            xceiverSuspender.awaitResult()
        }

        if (commissioner == null) {
            throw FlowException()
        }

        val reply = commissioner.sendGetNetworkInfo().throwOnErrorOrAbsent()
        val commissionerNetworkExtPanId = reply.network.extPanId
        // network info == mesh network to connect to?
        val targetMeshExtPanId = flowManager.targetDeviceMeshNetworkToJoin.value!!.extPanId
        // FIXME: handle case of mismatched commissioner mesh vs target mesh!
//        if (commissionerNetworkExtPanId != targetMeshExtPanId) {
//        }
    }

    private suspend fun ensureTargetMeshNetworkPasswordCollected() {
        if (flowManager.targetDeviceMeshNetworkToJoinCommissionerPassword.value.truthy()) {
            return
        }

        val ld = flowManager.targetDeviceMeshNetworkToJoinCommissionerPassword
        val ldSuspender = liveDataSuspender({ ld })
        val password = withContext(UI) {
            flowManager.navigate(R.id.action_global_enterNetworkPasswordFragment)
            ldSuspender.awaitResult()
        }

        if (password == null) {
            throw FlowException()
        }
    }

    private suspend fun ensureMeshNetworkJoinedShown() {
        flowManager.navigate(R.id.action_global_joiningMeshNetworkProgressFragment)
    }

    private suspend fun ensureMeshNetworkJoined() {
        val joiner = targetXceiver!!
        val commish = flowManager.commissionerTransceiverLD.value!!

        val password = flowManager.targetDeviceMeshNetworkToJoinCommissionerPassword.value!!
        commish.sendAuth(password).throwOnErrorOrAbsent()
        commish.sendStartCommissioner().throwOnErrorOrAbsent()
        flowManager.updateCommissionerStarted(true)

        val networkToJoin = flowManager.targetDeviceMeshNetworkToJoin.value!!
        val prepJoinerData = joiner.sendPrepareJoiner(networkToJoin).throwOnErrorOrAbsent()
        commish.sendAddJoiner(prepJoinerData.eui64, prepJoinerData.password).throwOnErrorOrAbsent()
        flowManager.updateTargetJoinedMeshNetwork(true)


        // FIXME: joining (sometimes?) fails here without a delay.  Revisit this value/try removing later?
        delay(2000)
        joiner.sendJoinNetwork().throwOnErrorOrAbsent()

        commish.sendStopCommissioner()

        commish.sendStopListeningMode()
        joiner.sendStopListeningMode()
    }

    private suspend fun ensureTargetDeviceClaimedByUser() {
        if (flowManager.targetOwnedByUserLD.value.truthy()) {
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

        val isInList = pollDevicesForNewDevice(targetDeviceId!!)
        if (!isInList) {
            throw FlowException()
        }

        flowManager.updateTargetOwnedByUser(true)

        if (!connectedToMeshNetworkAndOwnedUiShown) {
            delay(2000)
            connectedToMeshNetworkAndOwnedUiShown = true
        }
    }

    private suspend fun ensureTargetDeviceSetSetupDone() {
        targetXceiver!!.sendSetDeviceSetupDone().throwOnErrorOrAbsent()
    }

    private suspend fun ensureTargetDeviceIsNamed() {
        if (flowManager.isTargetDeviceNamedLD.value.truthy()) {
            return
        }

        val ldSuspender = liveDataSuspender({ flowManager.targetDeviceNameToAssignLD })
        val nameToAssign = withContext(UI) {
            flowManager.navigate(R.id.action_global_nameYourDeviceFragment)
            ldSuspender.awaitResult()
        }

        if (nameToAssign == null) {
            throw FlowException()
        }

        val joiner = cloud.getDevice(targetDeviceId!!)
        joiner.setName(nameToAssign)

        flowManager.updateIsTargetDeviceNamed(true)
    }

    private suspend fun ensureShowSetupFinishedUi() {
        flowManager.navigate(R.id.action_global_setupFinishedFragment)
    }

    private fun <V, E> Result<V, E>.throwOnErrorOrAbsent(): V {
        return when(this) {
            is Result.Error,
            is Result.Absent -> {
                log.error { "Error making request: ${this.error}" }
                throw FlowException()
            }
            is Result.Present -> this.value
        }
    }

}


class FlowException : Exception() {
    // FIXME: give this extra data
}

