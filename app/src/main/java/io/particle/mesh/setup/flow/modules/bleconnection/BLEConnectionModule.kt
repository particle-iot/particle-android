package io.particle.mesh.setup.flow.modules.bleconnection

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.setup.*
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.ui.BarcodeData
import io.particle.sdk.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KotlinLogging


class BLEConnectionModule(
    private val flowManager: FlowManager,
    private val btConnectionManager: BluetoothConnectionManager,
    private val transceiverFactory: ProtocolTransceiverFactory
) : Clearable {

    private val log = KotlinLogging.logger {}

    val targetDeviceBarcodeLD: LiveData<BarcodeData?> = MutableLiveData()
    val targetDeviceTransceiverLD: LiveData<ProtocolTransceiver?> = MutableLiveData()
    // FIXME: having 2 LDs, representing the transceiver & the uninitialized transceiver isn't great
    val targetDeviceConnectedLD: LiveData<Boolean?> = MutableLiveData()

    var targetDeviceId: String? = null
    val commissionerBarcodeLD: LiveData<BarcodeData?> = MutableLiveData()
    val commissionerTransceiverLD: LiveData<ProtocolTransceiver?> = MutableLiveData()
    val commissionerDeviceConnectedLD: LiveData<Boolean?> = MutableLiveData()

    private var connectingToTargetUiShown = false
    private var connectingToAssistingDeviceUiShown = false
    private var shownTargetInitialIsConnectedScreen = false
    private var shownAssistingDeviceInitialIsConnectedScreen = false

    private val targetXceiver
        get() = targetDeviceTransceiverLD.value


    override fun clearState() {
        targetDeviceId = null
        connectingToTargetUiShown = false
        connectingToAssistingDeviceUiShown = false
        shownTargetInitialIsConnectedScreen = false
        shownAssistingDeviceInitialIsConnectedScreen = false

        targetDeviceTransceiverLD.value?.disconnect()
        commissionerTransceiverLD.value?.disconnect()

        val setToNulls = listOf(
            targetDeviceBarcodeLD,
            targetDeviceTransceiverLD,
            targetDeviceConnectedLD,
            commissionerBarcodeLD,
            commissionerTransceiverLD
        )
        for (ld in setToNulls) {
            ld.castAndPost(null)
        }
    }

    fun updateCommissionerBarcode(barcodeData: BarcodeData?) {
        log.info { "updateCommissionerBarcode()" }
        (commissionerBarcodeLD as MutableLiveData).postValue(barcodeData)
    }

    fun updateTargetDeviceBarcode(barcodeData: BarcodeData?) {
        log.info { "updateTargetDeviceBarcode(): barcodeData=$barcodeData" }
        (targetDeviceBarcodeLD as MutableLiveData).postValue(barcodeData)
    }

    fun updateTargetDeviceConnectionInitialized(initialized: Boolean) {
        log.info { "updateTargetDeviceConnectionInitialized(): $initialized" }
        targetDeviceConnectedLD.castAndPost(initialized)
    }

    fun updateAssistingDeviceConnectionInitialized(initialized: Boolean) {
        log.info { "updateAssistingDeviceConnectionInitialized(): $initialized" }
        commissionerDeviceConnectedLD.castAndPost(initialized)
    }

    suspend fun ensureBarcodeDataForTargetDevice() {
        log.info { "ensureBarcodeDataForTargetDevice()" }
        if (targetDeviceBarcodeLD.value != null) {
            return
        }

        val userSpecifiedDeviceType = flowManager.targetDeviceType
        val liveDataSuspender = liveDataSuspender({ targetDeviceBarcodeLD.nonNull() })
        val barcodeData = withContext(Dispatchers.Main) {
            flowManager.navigate(R.id.action_global_getReadyForSetupFragment)
            liveDataSuspender.awaitResult()
        }

        if (barcodeData == null) {
            throw FlowException("Error getting barcode data for target device")
        }

        if (barcodeData.deviceType != userSpecifiedDeviceType) {
            flowManager.targetDeviceType = barcodeData.deviceType
            throw FlowException(
                "User scanned a different device than they originally specified.  Restarting flow.",
                ExceptionType.EXPECTED_FLOW
            )
        }
    }

    suspend fun ensureTargetDeviceConnected() {
        log.info { "ensureTargetDeviceConnected()" }
        if (targetXceiver != null && targetXceiver!!.isConnected) {
            return
        }

        if (!connectingToTargetUiShown) {
            flowManager.navigate(R.id.action_global_BLEPairingProgressFragment)
            connectingToTargetUiShown = true
        }


        // FIXME: consider what states we should be resetting here


        val ldSuspender = liveDataSuspender({ targetDeviceTransceiverLD })
        val transceiver = withContext(Dispatchers.Main) {
            connectTargetDevice()
            ldSuspender.awaitResult()
        }

        if (transceiver == null) {
            throw FlowException("Error ensuring target connected")
        }
    }

    suspend fun ensureTargetDeviceId(): String {
        log.info { "ensureTargetDeviceId()" }
        if (targetDeviceId != null) {
            return targetDeviceId!!
        }

        // get device ID
        val deviceIdReply = targetXceiver!!.sendGetDeviceId().throwOnErrorOrAbsent()
        targetDeviceId = deviceIdReply.id
        return deviceIdReply.id
    }

    suspend fun ensureShowTargetPairingSuccessful() {
        log.info { "ensureShowTargetPairingSuccessful()" }
        if (shownTargetInitialIsConnectedScreen) {
            return
        }
        shownTargetInitialIsConnectedScreen = true

        // FIXME: i18n this!
        flowManager.showCongratsScreen(
            "Successfully paired with " +
                    targetDeviceTransceiverLD.value?.deviceName
        )

        delay(2000)
    }

    suspend fun ensureShowAssistingDevicePairingSuccessful() {

        // FIXME: look into why this isn't being used...

        log.info { "ensureShowAssistingDevicePairingSuccessful()" }
        if (shownAssistingDeviceInitialIsConnectedScreen) {
            return
        }
        shownAssistingDeviceInitialIsConnectedScreen = true

        flowManager.showCongratsScreen(
            "Successfully paired with " +
                    commissionerTransceiverLD.value?.deviceName
        )

        delay(2000)
    }

    suspend fun ensureBarcodeDataForComissioner() {
        log.info { "ensureBarcodeDataForComissioner()" }
        if (commissionerBarcodeLD.value != null) {
            return
        }

        log.debug { "No commissioner barcode found; showing UI" }
        val liveDataSuspender = liveDataSuspender({ commissionerBarcodeLD.nonNull() })
        val barcodeData = withContext(Dispatchers.Main) {
            flowManager.navigate(R.id.action_global_manualCommissioningAddToNetworkFragment)
            liveDataSuspender.awaitResult()
        }

        if (barcodeData == null) {
            throw FlowException("Error getting barcode data for commissioner")
        }
    }

    suspend fun ensureCommissionerConnected() {
        log.info { "ensureCommissionerConnected()" }
        var commissioner = commissionerTransceiverLD.value
        if (commissioner != null && commissioner.isConnected) {
            return
        }


        if (!connectingToAssistingDeviceUiShown) {
            flowManager.navigate(R.id.action_global_assistingDevicePairingProgressFragment)
            connectingToAssistingDeviceUiShown = true
        }


        // FIXME: consider what states we should be resetting here


        val xceiverSuspender = liveDataSuspender({ commissionerTransceiverLD })
        commissioner = withContext(Dispatchers.Main) {
            connectCommissioner()
            xceiverSuspender.awaitResult()
        }

        flowManager.showCongratsScreen(
            "Successfully paired with " +
                    commissionerTransceiverLD.value?.deviceName
        )

        delay(2000)

        if (commissioner == null) {
            throw FlowException("Error ensuring commissioner connected")
        }
    }

    suspend fun ensureListeningStoppedForBothDevices() {
        log.info { "ensureListeningStoppedForBothDevices()" }
        targetXceiver?.sendStopListeningMode()
        flowManager.bleConnectionModule.commissionerTransceiverLD.value?.sendStopListeningMode()
    }


    @MainThread
    private suspend fun connectTargetDevice() {
        log.info { "connectTargetDevice()" }
        val barcode = targetDeviceBarcodeLD.value!!
        val targetTransceiver = connect(barcode, "target")

        if (targetTransceiver == null) {
            throw FlowException("Error connecting target")
        }

        log.debug { "Target device connected!" }
        targetDeviceTransceiverLD.castAndSetOnMainThread(targetTransceiver)
    }

    @MainThread
    private suspend fun connectCommissioner() {
        log.info { "connectCommissioner()" }
        val barcode = commissionerBarcodeLD.value!!
        val commissioner = connect(barcode, "commissioner")

        if (commissioner == null) {
            throw FlowException("Error connecting commissioner")
        }

        log.debug { "Commissioner connected!" }
        commissionerTransceiverLD.castAndSetOnMainThread(commissioner)
    }

    @MainThread
    private suspend fun connect(barcode: BarcodeData, connName: String): ProtocolTransceiver? {
        val device = btConnectionManager.connectToDevice(barcode.toDeviceName())
            ?: return null
        return transceiverFactory.buildProtocolTransceiver(device, connName, barcode.mobileSecret)
    }

}


private const val BT_NAME_ID_LENGTH = 6


private fun BarcodeData.toDeviceName(): String {

    // FIXME: convert this to return ParticleDeviceType?
    fun getDeviceTypeName(serialNumber: String): String {
        val first4 = serialNumber.substring(0, 4)
        return when (first4) {
            ARGON_SERIAL_PREFIX1,
            ARGON_SERIAL_PREFIX2,
            ARGON_SERIAL_PREFIX3 -> "Argon"
            XENON_SERIAL_PREFIX1,
            XENON_SERIAL_PREFIX2 -> "Xenon"
            BORON_LTE_SERIAL_PREFIX1,
            BORON_LTE_SERIAL_PREFIX2,
            BORON_3G_SERIAL_PREFIX1,
            BORON_3G_SERIAL_PREFIX2 -> "Boron"
            else -> "UNKNOWN"
        }
    }

    val deviceType = getDeviceTypeName(this.serialNumber)
    val lastSix = serialNumber.substring(serialNumber.length - BT_NAME_ID_LENGTH).toUpperCase()

    return "$deviceType-$lastSix"
}

internal val BarcodeData.deviceType: MeshDeviceType
    get() {
        val first4 = this.serialNumber.substring(0, 4)
        return when (first4) {
            ARGON_SERIAL_PREFIX1,
            ARGON_SERIAL_PREFIX2,
            ARGON_SERIAL_PREFIX3 -> MeshDeviceType.ARGON
            XENON_SERIAL_PREFIX1,
            XENON_SERIAL_PREFIX2 -> MeshDeviceType.XENON
            BORON_LTE_SERIAL_PREFIX1,
            BORON_LTE_SERIAL_PREFIX2,
            BORON_3G_SERIAL_PREFIX1,
            BORON_3G_SERIAL_PREFIX2 -> MeshDeviceType.BORON
            else -> throw IllegalArgumentException("Invalid serial number from barcode: $this")
        }
    }
