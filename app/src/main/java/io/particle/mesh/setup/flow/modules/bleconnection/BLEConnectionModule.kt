package io.particle.mesh.setup.flow.modules.bleconnection

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
import io.particle.mesh.setup.flow.FlowException
import io.particle.mesh.setup.flow.FlowManager
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent
import io.particle.mesh.setup.ui.BarcodeData
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import mu.KotlinLogging


class BLEConnectionModule(
        private val flowManager: FlowManager,
        private val btConnectionManager: BluetoothConnectionManager,
        private val transceiverFactory: ProtocolTransceiverFactory
) {

    private val log = KotlinLogging.logger {}

    val targetDeviceBarcodeLD: LiveData<BarcodeData?> = MutableLiveData()
    val targetDeviceTransceiverLD: LiveData<ProtocolTransceiver?> = MutableLiveData()
    // FIXME: having 2 LDs, representing the transceiver & the uninitialized transceiver isn't great
    val targetDeviceConnectedLD: LiveData<Boolean?> = MutableLiveData()

    val commissionerBarcodeLD: LiveData<BarcodeData?> = MutableLiveData()
    val commissionerTransceiverLD: LiveData<ProtocolTransceiver?> = MutableLiveData()

    private var targetDeviceId: String? = null

    private var connectingToTargetUiShown = false
    private var shownTargetInitialIsConnectedScreen = false

    private val targetXceiver
        get() = targetDeviceTransceiverLD.value


    fun clearState() {
        TODO("IMPLEMENT ME")
    }

    fun updateCommissionerBarcode(barcodeData: BarcodeData) {
        (commissionerBarcodeLD as MutableLiveData).postValue(barcodeData)
    }

    fun updateTargetDeviceBarcode(barcodeData: BarcodeData?) {
        log.debug { "updateTargetDeviceBarcode(): barcodeData=$barcodeData" }
        (targetDeviceBarcodeLD as MutableLiveData).postValue(barcodeData)
    }

    fun updateTargetDeviceConnectionInitialized(initialized: Boolean) {
        (targetDeviceConnectedLD as MutableLiveData).postValue(initialized)
    }

    suspend fun ensureBarcodeDataForTargetDevice() {
        log.info { "ensureBarcodeDataForTargetDevice()" }
        if (targetDeviceBarcodeLD.value != null) {
            return
        }

        val liveDataSuspender = liveDataSuspender({ targetDeviceBarcodeLD })
        val barcodeData = withContext(UI) {
            flowManager.navigate(R.id.action_global_getReadyForSetupFragment)
            liveDataSuspender.awaitResult()
        }

        if (barcodeData == null) {
            throw FlowException()
        }
    }

    suspend fun ensureTargetDeviceConnected() {
        if (targetXceiver != null && targetXceiver!!.isConnected) {
            return
        }

        // FIXME: don't track showing the UI, track if we've gathered the data from the UI.
        if (targetDeviceConnectedLD.value != null) {
            flowManager.navigate(R.id.action_global_BLEPairingProgressFragment)
            connectingToTargetUiShown = true
        }

        val ldSuspender = liveDataSuspender({ targetDeviceTransceiverLD })
        val transceiver = withContext(UI) {
            connectTargetDevice()
            ldSuspender.awaitResult()
        }

        if (transceiver == null) {
            throw FlowException()
        }
    }

    suspend fun ensureTargetDeviceId(): String {
        if (targetDeviceId != null) {
            return targetDeviceId!!
        }

        // get device ID
        val deviceIdReply = targetXceiver!!.sendGetDeviceId().throwOnErrorOrAbsent()
        targetDeviceId = deviceIdReply.id
        return deviceIdReply.id
    }

    suspend fun ensureShowPairingSuccessful() {
        if (shownTargetInitialIsConnectedScreen) {
            return
        }
        delay(2000)
        shownTargetInitialIsConnectedScreen = true
        updateTargetDeviceConnectionInitialized(true)
    }

    suspend fun ensureBarcodeDataForComissioner() {
        log.info { "ensureBarcodeDataForComissioner()" }
        if (commissionerBarcodeLD.value != null) {
            return
        }

        val liveDataSuspender = liveDataSuspender({ commissionerBarcodeLD })
        val barcodeData = withContext(UI) {
            flowManager.navigate(R.id.action_global_manualCommissioningAddToNetworkFragment)
            liveDataSuspender.awaitResult()
        }

        if (barcodeData == null) {
            throw FlowException()
        }
    }

    suspend fun ensureCommissionerConnected() {
        var commissioner = commissionerTransceiverLD.value
        if (commissioner != null && commissioner.isConnected) {
            return
        }

        val xceiverSuspender = liveDataSuspender({ commissionerTransceiverLD })
        commissioner = withContext(UI) {
            connectCommissioner()
            xceiverSuspender.awaitResult()
        }

        if (commissioner == null) {
            throw FlowException()
        }

        // FIXME: handle case of mismatched commissioner mesh vs target mesh!
        // FIXME: Split this out into another "step" method!

        val reply = commissioner.sendGetNetworkInfo().throwOnErrorOrAbsent()
        val commissionerNetworkExtPanId = reply.network.extPanId
        // network info == mesh network to connect to?


//        val targetMeshExtPanId = targetDeviceMeshNetworkToJoinLD.value!!.extPanId
//        if (commissionerNetworkExtPanId != targetMeshExtPanId) {
//        }
    }

    private fun connectTargetDevice() {
        log.info { "connectTargetDevice()" }
        launch {
            val targetTransceiver = withContext(UI) {
                val barcode = targetDeviceBarcodeLD.value!!
                return@withContext connect(barcode, "target")
            } ?: throw FlowException()

            log.debug { "Target device connected!" }
            (targetDeviceTransceiverLD as MutableLiveData).setOnMainThread(targetTransceiver)
        }
    }

    private fun connectCommissioner() {
        log.info { "connectCommissioner()" }
        launch {
            val commissioner = withContext(UI) {
                val barcode = commissionerBarcodeLD.value!!
                return@withContext connect(barcode, "commissioner")
            } ?: throw FlowException()

            log.debug { "Commissioner connected!" }
            (commissionerTransceiverLD as MutableLiveData).setOnMainThread(commissioner)
        }
    }

    private suspend fun connect(barcode: BarcodeData, connName: String): ProtocolTransceiver? {
        val device = btConnectionManager.connectToDevice(barcode.toDeviceName())
            ?: return null
        return transceiverFactory.buildProtocolTransceiver(device, connName, barcode.mobileSecret)
    }

}



private const val BT_NAME_ID_LENGTH = 6


private fun BarcodeData.toDeviceName(): String {

    fun getDeviceTypeName(serialNumber: String): String {
        val first4 = serialNumber.substring(0, 4)
        return when (first4) {
            "ARGH" -> "Argon"
            "XENH" -> "Xenon"
            "R40K",
            "R31K" -> "Boron"
            else -> "UNKNOWN"
        }
    }

    val deviceType = getDeviceTypeName(this.serialNumber)
    val lastSix = serialNumber.substring(serialNumber.length - BT_NAME_ID_LENGTH).toUpperCase()

    return "$deviceType-$lastSix"
}
