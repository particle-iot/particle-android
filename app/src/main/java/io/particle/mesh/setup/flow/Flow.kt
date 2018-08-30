package io.particle.mesh.setup.flow

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.setup.flow.modules.cloudconnection.CloudConnectionModule
import io.particle.mesh.setup.ui.BarcodeData
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext


class Flow(
        private val flowManager: FlowManager,
        cloud: ParticleCloud
) {

    private val cloudConnModule = CloudConnectionModule(cloud)

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
        fetchClaimCode()
        connectToTargetDevice()
    }

    private fun fetchClaimCode() {
        if (cloudConnModule.claimCode == null) {
            cloudConnModule.fetchClaimCode()
        }
    }

    private suspend fun ensureBarcodeDataForTarget() {
        if (flowManager.targetDeviceBarcodeLD.value != null) {
            return
        }

        val liveDataSuspender = liveDataSuspender({ flowManager.targetDeviceBarcodeLD })
        val result = withContext(UI) {
            liveDataSuspender.awaitResult()
            flowManager.navigate()
        }

    }

    private fun connectToTargetDevice() {
        // 1. ensure we have the barcode data for target device


        // 2. check if we have the connection already (or do this first...?)
        // 3. if not 2, connect to the device... and set it on FlowManager?
    }


}


class FlowException : Exception() {
    // FIXME: give this extra data
}


private const val BT_NAME_ID_LENGTH = 6


private fun BarcodeData.toDeviceName(): String {

    fun getDeviceTypeName(serialNumber: String): String {
        val first4 = serialNumber.substring(0, 4)
        return when(first4) {
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
