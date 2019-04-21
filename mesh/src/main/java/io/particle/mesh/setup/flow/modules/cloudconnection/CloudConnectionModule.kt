package io.particle.mesh.setup.flow.modules.cloudconnection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticlePricingInfo
import io.particle.android.sdk.cloud.PricingImpactAction
import io.particle.android.sdk.cloud.PricingImpactNetworkType
import io.particle.firmwareprotos.ctrl.Network
import io.particle.firmwareprotos.ctrl.Network.InterfaceType
import io.particle.firmwareprotos.ctrl.cloud.Cloud.ConnectionStatus
import io.particle.mesh.R
import io.particle.mesh.common.android.livedata.*
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.context.NetworkSetupType
import io.particle.mesh.setup.flow.modules.meshsetup.MeshNetworkToJoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.util.*


class CloudConnectionModule(
    val wifiNetworksScannerLD: WifiNetworksScannerLD,
    private val flowManager: FlowManager,
    private val cloud: ParticleCloud
) : Clearable {

    private val log = KotlinLogging.logger {}

    val argonSteps = ArgonSteps(flowManager)
    val boronSteps = BoronSteps(flowManager, cloud)

    val targetDeviceShouldBeClaimedLD: LiveData<Boolean?> = MutableLiveData()
    val targetOwnedByUserLD: LiveData<Boolean?> = MutableLiveData()
    val targetDeviceNameToAssignLD: LiveData<String?> = MutableLiveData()
    val currentDeviceName: LiveData<String?> = MutableLiveData()
    val isTargetDeviceNamedLD: LiveData<Boolean?> = MutableLiveData()
    val targetDeviceConnectedToCloud: LiveData<Boolean?> = ClearValueOnInactiveLiveData()
    val shouldConnectToDeviceCloudConfirmed: LiveData<Boolean?> = MutableLiveData()
    val pricingImpactConfirmed: LiveData<Boolean?> = MutableLiveData()

    var claimCode: String? = null
    var pricingImpact: ParticlePricingInfo? = null

    private var checkedIsTargetClaimedByUser = false
    private var connectedToMeshNetworkAndOwnedUiShown = false
    private var checkEthernetGatewayUiShown = false
    private var connectedToCloudCongratsUiShown = false
    private var paymentCardOnFile = false

    private val targetXceiver
        get() = flowManager.bleConnectionModule.targetDeviceTransceiverLD.value


    override fun clearState() {
        claimCode = null
        pricingImpact = null
        checkedIsTargetClaimedByUser = false
        connectedToMeshNetworkAndOwnedUiShown = false
        checkEthernetGatewayUiShown = false
        connectedToCloudCongratsUiShown = false
        paymentCardOnFile = false

        argonSteps.clearState()
        boronSteps.clearState()

        val setToNulls = listOf(
            targetDeviceShouldBeClaimedLD,
            targetOwnedByUserLD,
            targetDeviceNameToAssignLD,
            isTargetDeviceNamedLD,
            targetDeviceConnectedToCloud,
            currentDeviceName,
            shouldConnectToDeviceCloudConfirmed,
            pricingImpactConfirmed
        )
        for (ld in setToNulls) {
            ld.castAndPost(null)
        }
    }

    fun updateTargetOwnedByUser(owned: Boolean) {
        log.info { "updateTargetOwnedByUser(): $owned" }
        targetOwnedByUserLD.castAndPost(owned)
    }

    fun updateIsTargetDeviceNamed(named: Boolean) {
        log.info { "updateIsTargetDeviceNamed(): $named" }
        isTargetDeviceNamedLD.castAndPost(named)
    }

    fun updateTargetDeviceNameToAssign(name: String) {
        log.info { "updateTargetDeviceNameToAssign(): $name" }
        currentDeviceName.castAndPost(name)
        targetDeviceNameToAssignLD.castAndPost(name)
    }

    fun updateShouldConnectToDeviceCloudConfirmed(confirmed: Boolean) {
        log.info { "updateShouldConnectToDeviceCloudConfirmed(): $confirmed" }
        shouldConnectToDeviceCloudConfirmed.castAndPost(confirmed)
    }

    fun updatePricingImpactConfirmed(confirmed: Boolean) {
        log.info { "updatePricingImpactConfirmed(): $confirmed" }
        pricingImpactConfirmed.castAndPost(confirmed)
    }

    suspend fun ensureUnclaimed() {
        val id = flowManager.bleConnectionModule.ensureTargetDeviceId()
        val device = cloud.getDevice(id)
        device.unclaim()
        throw FlowException("Unclaimed successfully (I guess?)")
    }

    suspend fun ensureClaimCodeFetched() {
        log.info { "ensureClaimCodeFetched(), claimCode=$claimCode" }
        if (claimCode == null) {
            log.info { "Fetching new claim code" }
            try {
                flowManager.showGlobalProgressSpinner(true)
                claimCode = cloud.generateClaimCode().claimCode
            } finally {
                flowManager.showGlobalProgressSpinner(false)
            }
        }
    }

    suspend fun ensureConnectedToCloud() {
        log.info { "ensureConnectedToCloud()" }
        for (i in 0..9) { // loop for 45 seconds
            delay(5000)
            val statusReply = targetXceiver!!.sendGetConnectionStatus().throwOnErrorOrAbsent()
            log.info { "statusReply=$statusReply" }
            if (statusReply.status == ConnectionStatus.CONNECTED) {
                targetDeviceConnectedToCloud.castAndPost(true)
                return
            }
        }
        throw FlowException("Error ensuring connection to cloud")
    }

    suspend fun ensureCheckedIsClaimed(targetDeviceId: String) {
        log.info { "ensureCheckedIsClaimed()" }

        log.info { "Networks: ${Arrays.toString(cloud.getNetworks().toTypedArray())}" }

        if (checkedIsTargetClaimedByUser) {
            return
        }

        val userOwnsDevice = cloud.userOwnsDevice(targetDeviceId)
        log.warn { "User owns device?: $userOwnsDevice" }
        checkedIsTargetClaimedByUser = true
        if (userOwnsDevice) {
            targetDeviceShouldBeClaimedLD.castAndSetOnMainThread(false)
            return
        }

        // FIXME: FINISH IMPLEMENTING!


        // FIXME: REMOVE!
        targetDeviceShouldBeClaimedLD.castAndSetOnMainThread(true)
        // REMOVE ^^^

        // show dialog
//        val ldSuspender = liveDataSuspender({ flowManager.targetDeviceShouldBeClaimedLD })
//        val shouldClaim = withContext(Dispatchers.Main) {
//            flowManager.navigate()
//            ldSuspender.awaitResult()
//        }
    }

    suspend fun ensureSetClaimCode() {
        log.info { "ensureSetClaimCode()" }
        // FIXME: see above re: setting the claim code.
        // If we hit this point, we must want to claim the device
        // (if we haven't claimed it already)

//        if (!targetDeviceShouldBeClaimedLD.value.truthy()) {
//            return
//        }
        targetXceiver!!.sendSetClaimCode(claimCode!!).throwOnErrorOrAbsent()
    }

    suspend fun ensureEthernetHasIP() {
        log.info { "ensureEthernetHasIP()" }

        suspend fun findEthernetInterface(): Network.InterfaceEntry? {
            val ifaceListReply = targetXceiver!!.sendGetInterfaceList().throwOnErrorOrAbsent()
            return ifaceListReply.interfacesList.firstOrNull { it.type == InterfaceType.ETHERNET }
        }

        val ethernet = findEthernetInterface()
        requireNotNull(ethernet)

        val reply = targetXceiver!!.sendGetInterface(ethernet.index).throwOnErrorOrAbsent()
        val iface = reply.`interface`
        for (addyList in listOf(iface.ipv4Config.addressesList, iface.ipv6Config.addressesList)) {

            val address = addyList.firstOrNull {
                it.address.v4.address.truthy() || it.address.v6.address.truthy()
            }
            if (address != null) {
                log.debug { "IP address on ethernet (interface ${ethernet.index}) found: $address" }
                return
            }
        }

        val ldSuspender = liveDataSuspender({ flowManager.dialogResultLD.nonNull() })
        val result = withContext(Dispatchers.Main) {
//            flowManager.newDialogRequest(
//                ResDialogSpec(
//                    string.p_connecttocloud_xenon_gateway_needs_ethernet,
//                    android.R.string.ok
//                )
//            )
            ldSuspender.awaitResult()
        }
        log.info { "result from awaiting on 'ethernet must be plugged in dialog: $result" }
        flowManager.clearDialogResult()
        delay(500)
        throw FlowException("Ethernet connection not plugged in; user prompted.")
    }

    suspend fun ensureEthernetConnectingToDeviceCloudUiShown() {
        log.info { "ensureEthernetConnectingToDeviceCloudUiShown()" }
//        flowManager.navigate(R.id.action_global_connectingToDeviceCloudFragment)
    }

    suspend fun ensureTargetDeviceClaimedByUser() {
        log.info { "ensureTargetDeviceClaimedByUser()" }
        if (targetOwnedByUserLD.value.truthy()) {
            return
        }

        suspend fun pollDevicesForNewDevice(deviceId: String): Boolean {
            val idLower = deviceId.toLowerCase()
            for (i in 0..23) { // 45 seconds
                // FIXME: what should the timing be here?
                delay(2000)
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
            throw FlowException("Target device does not appear to be claimed")
        }

        val device = cloud.getDevice(targetDeviceId)
        currentDeviceName.castAndPost(device.name)

        updateTargetOwnedByUser(true)

        if (!connectedToMeshNetworkAndOwnedUiShown) {
            delay(2000)
            connectedToMeshNetworkAndOwnedUiShown = true
        }
    }

    suspend fun ensureTargetDeviceIsNamed() {
        log.info { "ensureTargetDeviceIsNamed()" }
        if (isTargetDeviceNamedLD.value.truthy()) {
            return
        }

        val ldSuspender = liveDataSuspender({ targetDeviceNameToAssignLD.nonNull() })
        val nameToAssign = withContext(Dispatchers.Main) {
//            flowManager.navigate(R.id.action_global_nameYourDeviceFragment)
            ldSuspender.awaitResult()
        }

        if (nameToAssign == null) {
            throw FlowException("Error ensuring target device is named")
        }

        try {
            flowManager.showGlobalProgressSpinner(true)

            val targetDeviceId = flowManager.bleConnectionModule.ensureTargetDeviceId()
            val joiner = cloud.getDevice(targetDeviceId)
            joiner.name = nameToAssign
            updateIsTargetDeviceNamed(true)
        } catch (ex: Exception) {
            throw FlowException("Unable to rename device")
        } finally {
            flowManager.showGlobalProgressSpinner(false)
        }
    }

    suspend fun ensureConnectedToCloudSuccessUi() {
        log.info { "ensureConnectedToCloudSuccessUi()" }
        if (connectedToCloudCongratsUiShown) {
            return
        }

        connectedToCloudCongratsUiShown = true
        val template = flowManager.getString(R.string.p_congrats_claimed)
        val msg = template.format(flowManager.getTypeName())
        flowManager.showCongratsScreen(msg)
        delay(2000)
    }

    suspend fun ensureCardOnFile() {
        log.info { "ensureCardOnFile()" }
        if (paymentCardOnFile) {
            return
        }

        val cardResponse = cloud.getPaymentCard()
        paymentCardOnFile = cardResponse.card?.last4 != null
        if (paymentCardOnFile) {
            return
        }

        val ldSuspender = liveDataSuspender({ flowManager.dialogResultLD.nonNull() })
        val dialogResult = withContext(Dispatchers.Main) {
//            flowManager.newDialogRequest(
//                ResDialogSpec(
//                    R.string.p_mesh_billing_please_go_to_your_browser,
//                    android.R.string.ok,
//                    R.string.p_mesh_action_exit_setup
//                )
//            )
            ldSuspender.awaitResult()
        }
        log.info { "Result for leave network confirmation dialog: $dialogResult" }
        flowManager.clearDialogResult()

//        val err = when (dialogResult) {
//            io.particle.mesh.ui.setup.DialogResult.POSITIVE -> FlowException(
//                "Restarting flow after user confirmed payment card",
//                ExceptionType.EXPECTED_FLOW
//            )
//            io.particle.mesh.ui.setup.DialogResult.NEGATIVE -> FlowException(
//                "User choosing not to enter payment card; exiting setup",
//                ExceptionType.ERROR_FATAL
//            )
//            null -> FlowException("Unknown error when asking user to enter payment card")
//        }
//        throw err
    }

    suspend fun ensurePricingImpactRetrieved() {
        log.info { "ensurePricingImpactRetrieved()" }

        if (pricingImpact != null) {
            return
        }

        val action = when (flowManager.deviceModule.networkSetupTypeLD.value) {
            NetworkSetupType.AS_GATEWAY -> PricingImpactAction.CREATE_NETWORK
            NetworkSetupType.STANDALONE -> PricingImpactAction.ADD_USER_DEVICE
            NetworkSetupType.NODE_JOINER -> throw FatalFlowException(
                "Should not be showing billing for joiners!"
            )
            null -> PricingImpactAction.ADD_NETWORK_DEVICE
            else -> TODO()  // huh?
        }

        val networkType = if (flowManager.targetDeviceType == Gen3ConnectivityType.CELLULAR) {
            PricingImpactNetworkType.CELLULAR
        } else {
            PricingImpactNetworkType.WIFI
        }

        val selectedNetwork = flowManager.meshSetupModule.targetDeviceMeshNetworkToJoinLD.value
        val networkId = when (selectedNetwork) {
            is MeshNetworkToJoin.SelectedNetwork -> selectedNetwork.networkToJoin.networkId
            is MeshNetworkToJoin.CreateNewNetwork,
            null -> null
        }

        pricingImpact = cloud.getPricingImpact(
            action = action,
            deviceId = flowManager.bleConnectionModule.targetDeviceId,
            networkId = networkId,
            networkType = networkType,
            iccid = flowManager.cloudConnectionModule.boronSteps.targetDeviceIccid.value
        )
    }

    suspend fun ensureShowPricingImpactUI() {
        log.info { "ensureShowPricingImpactUI()" }
        if (pricingImpactConfirmed.value.truthy()) {
            return
        }

        val suspender = liveDataSuspender({ pricingImpactConfirmed.nonNull() })
        withContext(Dispatchers.Main) {
//            flowManager.navigate(R.id.action_global_pricingImpactFragment)
            suspender.awaitResult()
        }
    }

    suspend fun ensureShowConnectToDeviceCloudConfirmation() {
        log.info { "ensureShowConnectToDeviceCloudConfirmation()" }

        val confirmationLD = flowManager.cloudConnectionModule.shouldConnectToDeviceCloudConfirmed
        if (confirmationLD.value.truthy()) {
            return
        }

        val suspender = liveDataSuspender({ confirmationLD.nonNull() })
        withContext(Dispatchers.Main) {
//            flowManager.navigate(R.id.action_global_argonConnectToDeviceCloudIntroFragment)
            suspender.awaitResult()
        }
    }

}
