package io.particle.mesh.setup.flow

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.IdRes
import androidx.navigation.NavController
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
import io.particle.mesh.setup.flow.modules.meshsetup.TargetDeviceMeshNetworksScanner
import io.particle.mesh.setup.ui.BarcodeData
import io.particle.mesh.setup.utils.runOnMainThread
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import mu.KotlinLogging


class FlowManager(
        val targetDeviceType: ParticleDeviceType,
        cloud: ParticleCloud,
        private val navControllerRef: LiveData<NavController?>,
        private val btConnectionManager: BluetoothConnectionManager,
        private val transceiverFactory: ProtocolTransceiverFactory
) {

    val targetDeviceBarcodeLD: LiveData<BarcodeData?> = MutableLiveData()
    val targetDeviceTransceiverLD: LiveData<ProtocolTransceiver?> = MutableLiveData()
    val targetDeviceShouldBeClaimedLD: LiveData<Boolean?> = MutableLiveData()
    val targetDeviceVisibleMeshNetworksLD: TargetDeviceMeshNetworksScanner =
            TargetDeviceMeshNetworksScanner(this)
    // FIXME: having 2 LDs, representing the transceiver & the uninitialized transceiver isn't great
    val targetDeviceConnectedLD: LiveData<Boolean?> = MutableLiveData()
    val targetDeviceMeshNetworkToJoin: LiveData<Mesh.NetworkInfo?> = MutableLiveData()

    val commissionerBarcodeLD: LiveData<BarcodeData?> = MutableLiveData()
    val commissionerTransceiverLD: LiveData<ProtocolTransceiver?> = MutableLiveData()

    val targetDeviceMeshNetworkToJoinCommissionerPassword: LiveData<String?> = MutableLiveData()

    val commissionerStartedLD: LiveData<Boolean?> = MutableLiveData()
    val targetJoinedMeshNetworkLD: LiveData<Boolean?> = MutableLiveData()
    val targetOwnedByUserLD: LiveData<Boolean?> = MutableLiveData()

    val targetDeviceNameToAssignLD: LiveData<String?> = MutableLiveData()
    val isTargetDeviceNamedLD: LiveData<Boolean?> = MutableLiveData()

    private var flow: Flow = Flow(this, cloud)

    private val navController: NavController?
        get() = navControllerRef.value


    private val log = KotlinLogging.logger {}


    fun startFlow() {
        flow.runFlow()
    }

    fun clearState() {
        flow.clearState()
        targetDeviceTransceiverLD.value?.disconnect()
        (targetDeviceBarcodeLD as MutableLiveData).postValue(null)
        (targetDeviceTransceiverLD as MutableLiveData).postValue(null)
        commissionerTransceiverLD.value?.disconnect()
        // FIXME: finish implementing!
    }

    fun navigate(@IdRes idRes: Int) {
        runOnMainThread { navController?.navigate(idRes) }
    }

    fun updateTargetDeviceBarcode(barcodeData: BarcodeData?) {
        log.debug { "updateTargetDeviceBarcode(): barcodeData=$barcodeData" }
        (targetDeviceBarcodeLD as MutableLiveData).postValue(barcodeData)
    }

    fun updateTargetDeviceConnectionInitialized(initialized: Boolean) {
        (targetDeviceConnectedLD as MutableLiveData).postValue(initialized)
    }

    fun updateSelectedMeshNetworkToJoin(meshNetworkToJoin: Mesh.NetworkInfo) {
        (targetDeviceMeshNetworkToJoin as MutableLiveData).postValue(meshNetworkToJoin)
    }

    fun updateCommissionerBarcode(barcodeData: BarcodeData) {
        (commissionerBarcodeLD as MutableLiveData).postValue(barcodeData)
    }

    fun updateTargetMeshNetworkCommissionerPassword(password: String) {
        (targetDeviceMeshNetworkToJoinCommissionerPassword as MutableLiveData).postValue(password)
    }

    fun updateCommissionerStarted(started: Boolean) {
        (commissionerStartedLD as MutableLiveData).postValue(started)
    }

    fun updateTargetJoinedMeshNetwork(joined: Boolean) {
        (targetJoinedMeshNetworkLD as MutableLiveData).postValue(joined)
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

    fun connectTargetDevice() {
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

    fun connectCommissioner() {
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

    fun startNewFlow() {
        TODO("IMPLEMENT THIS")
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
