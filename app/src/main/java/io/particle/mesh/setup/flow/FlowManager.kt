package io.particle.mesh.setup.flow

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.IdRes
import androidx.navigation.NavController
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
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
        // FIXME: finish implementing!
    }

    fun navigate(@IdRes idRes: Int) {
        runOnMainThread { navController?.navigate(idRes) }
    }

    fun updateTargetDeviceBarcode(barcodeData: BarcodeData?) {
        log.debug { "updateTargetDeviceBarcode(): barcodeData=$barcodeData" }
        (targetDeviceBarcodeLD as MutableLiveData).postValue(barcodeData)
    }

    fun connectTargetDevice() {
        log.info { "connectTargetDevice()" }
        launch {
            val targetTransceiver = withContext(UI) {

                val barcode = targetDeviceBarcodeLD.value!!
                val targetDevice = btConnectionManager.connectToDevice(barcode.toDeviceName())
                    ?: return@withContext null

                return@withContext transceiverFactory.buildProtocolTransceiver(
                        targetDevice,
                        "target",
                        barcode.mobileSecret
                )
            } ?: throw FlowException()

            log.debug { "Target device connected!" }
            (targetDeviceTransceiverLD as MutableLiveData).setOnMainThread(targetTransceiver)
        }
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
