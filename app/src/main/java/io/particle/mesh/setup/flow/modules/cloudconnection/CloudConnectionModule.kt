package io.particle.mesh.setup.flow.modules.cloudconnection

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.firmwareprotos.ctrl.Network
import io.particle.firmwareprotos.ctrl.Network.InterfaceType
import io.particle.firmwareprotos.ctrl.cloud.Cloud.ConnectionStatus
import io.particle.mesh.common.android.livedata.*
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.flow.Clearable
import io.particle.mesh.setup.flow.FlowException
import io.particle.mesh.setup.flow.FlowManager
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent
import io.particle.mesh.setup.ui.DialogSpec.ResDialogSpec
import io.particle.mesh.setup.ui.DialogSpec.StringDialogSpec
import io.particle.mesh.setup.utils.safeToast
import io.particle.sdk.app.R
import io.particle.sdk.app.R.id
import io.particle.sdk.app.R.string
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.withContext
import mu.KotlinLogging


class CloudConnectionModule(
        private val flowManager: FlowManager,
        private val cloud: ParticleCloud
) : Clearable {

    private val log = KotlinLogging.logger {}

    val targetDeviceShouldBeClaimedLD: LiveData<Boolean?> = MutableLiveData()
    val targetOwnedByUserLD: LiveData<Boolean?> = MutableLiveData()
    val targetDeviceNameToAssignLD: LiveData<String?> = MutableLiveData()
    val currentDeviceName: LiveData<String?> = MutableLiveData()
    val isTargetDeviceNamedLD: LiveData<Boolean?> = MutableLiveData()
    val targetDeviceConnectedToCloud: LiveData<Boolean?> = ClearValueOnInactiveLiveData()
    val meshRegisteredWithCloud: LiveData<Boolean?> = MutableLiveData()

    var claimCode: String? = null

    private var checkedIsTargetClaimedByUser = false
    private var connectedToMeshNetworkAndOwnedUiShown = false
    private var checkEthernetGatewayUiShown = false

    private val targetXceiver
        get() = flowManager.bleConnectionModule.targetDeviceTransceiverLD.value


    suspend fun ensureUnclaimed() {
        val id = flowManager.bleConnectionModule.ensureTargetDeviceId()
        val device = cloud.getDevice(id)
        if (device == null) {
            throw FlowException("No device found with ID $id")
        }
        device.unclaim()
        throw FlowException("Unclaimed successfully (I guess?)")
    }

    override fun clearState() {
        claimCode = null
        checkedIsTargetClaimedByUser = false
        connectedToMeshNetworkAndOwnedUiShown = false
        checkEthernetGatewayUiShown = false

        val setToNulls = listOf(
                targetDeviceShouldBeClaimedLD,
                targetOwnedByUserLD,
                targetDeviceNameToAssignLD,
                isTargetDeviceNamedLD,
                targetDeviceConnectedToCloud,
                meshRegisteredWithCloud,
                currentDeviceName
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

    suspend fun ensureClaimCodeFetched() {
        log.info { "ensureClaimCodeFetched(), claimCode=$claimCode" }
        if (claimCode == null) {
            log.info { "Fetching new claim code" }
            val ctx = flowManager.everythingNeedsAContext
            ctx.safeToast("Fetching claim code...")
            claimCode = cloud.generateClaimCode().claimCode
            delay(5000)
            ctx.safeToast("${claimCode?.length}-char claim code fetched: $claimCode")
        }
    }

    suspend fun ensureConnectedToCloud() {
        log.info { "ensureConnectedToCloud()" }
        for (i in 0..9) { // loop for 45 seconds
            delay(5000)
            val statusReply = targetXceiver!!.sendGetConnectionStatus().throwOnErrorOrAbsent()
            if (statusReply.status == ConnectionStatus.CONNECTED) {
                targetDeviceConnectedToCloud.castAndPost(true)
                return
            }
        }
        throw FlowException("Error ensuring connection to cloud")
    }

    // FIXME: where does this belong?
    suspend fun ensureDeviceIsUsingEligibleFirmware() {
        log.info { "ensureDeviceIsUsingEligibleFirmware()" }
        // TODO: TO BE IMPLEMENTED
    }

    suspend fun ensureCheckedIsClaimed(targetDeviceId: String) {
        log.info { "ensureCheckedIsClaimed()" }
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
//        val shouldClaim = withContext(UI) {
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
        val ctx = flowManager.everythingNeedsAContext
        ctx.safeToast("Setting ${claimCode?.length ?: "0"}-char claim code: $claimCode")
        val result = targetXceiver!!.sendSetClaimCode(claimCode!!)
        try {
            result.throwOnErrorOrAbsent()
        } catch (ex: Exception) {
            flowManager.newDialogRequest(
                    StringDialogSpec("Error sending claim code!  Error code: '${result.error?.name}'"))
            flowManager.clearDialogResult()
            throw ex
        }
        delay(2000)
        ctx.safeToast("Claim code sent successfully!  Response code: 'OK'", Toast.LENGTH_LONG)
        delay(3000)
    }

    suspend fun ensureEthernetHasIP() {
        log.info { "ensureEthernetHasIP()" }

        suspend fun findEthernetInterface(): Network.InterfaceEntry? {
            val ifaceListReply = targetXceiver!!.sendGetInterfaceList().throwOnErrorOrAbsent()
            return ifaceListReply.interfacesList.firstOrNull { it.type == InterfaceType.ETHERNET }
        }

        val ethernet = findEthernetInterface()
        requireNotNull(ethernet)

        val reply = targetXceiver!!.sendGetInterface(ethernet!!.index).throwOnErrorOrAbsent()
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
        val result = withContext(UI) {
            flowManager.newDialogRequest(ResDialogSpec(
                    string.p_connecttocloud_xenon_gateway_needs_ethernet,
                    android.R.string.ok
            ))
            ldSuspender.awaitResult()
        }
        log.info { "result from awaiting on 'ethernet must be plugged in dialog: $result" }
        flowManager.clearDialogResult()
        delay(500)
        throw FlowException("Ethernet connection not plugged in; user prompted.")
    }

    suspend fun ensureConnectingToDeviceCloudUiShown() {
        flowManager.navigate(R.id.action_global_connectingToDeviceCloudFragment)
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

        val ldSuspender = liveDataSuspender({ targetDeviceNameToAssignLD })
        val nameToAssign = withContext(UI) {
            flowManager.navigate(id.action_global_nameYourDeviceFragment)
            ldSuspender.awaitResult()
        }

        if (nameToAssign == null) {
            throw FlowException("Error ensuring target device is named")
        }

        try {
            flowManager.showGlobalProgressSpinner(true)

            val targetDeviceId = flowManager.bleConnectionModule.ensureTargetDeviceId()
            val joiner = cloud.getDevice(targetDeviceId)
            joiner.setName(nameToAssign)
            updateIsTargetDeviceNamed(true)
        } catch (ex: Exception) {
            throw FlowException("Unable to rename device")
        } finally {
            flowManager.showGlobalProgressSpinner(false)
        }
    }

    suspend fun ensureNetworkIsRegisteredWithCloud() {
        // Implement when we have this API
        delay(3000)
        meshRegisteredWithCloud.castAndPost(true)
    }

}
