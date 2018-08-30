package io.particle.mesh.setup.flow

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.setup.flow.modules.cloudconnection.CloudConnectionModule
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import mu.KotlinLogging


class Flow(
        private val flowManager: FlowManager,
        cloud: ParticleCloud
) {

    private val cloudConnModule = CloudConnectionModule(cloud)
    private var connectingToTargetUiShown = false

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

        ensureTargetInformationGathered()
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
            TODO("FILL THIS IN")
            throw FlowException()
        }
    }

    private suspend fun ensureTargetDeviceConnected() {
        val currentTcvr = flowManager.targetDeviceTransceiverLD.value
        if (currentTcvr != null && currentTcvr.isConnected) {
            return
        }

        if (!connectingToTargetUiShown) {
            flowManager.navigate(R.id.action_global_BLEPairingProgressFragment)
            connectingToTargetUiShown = true
        }

        val ldSuspender = liveDataSuspender({ flowManager.targetDeviceTransceiverLD })
        val transceiver = withContext(UI) {
            flowManager.connectTargetDevice()
            ldSuspender.awaitResult()
        }

        if (transceiver == null) {
            TODO("FILL THIS IN")
            throw FlowException()
        }
    }

    // This method corresponds to the 
    private suspend fun ensureTargetInformationGathered() {

    }

}


class FlowException : Exception() {
    // FIXME: give this extra data
}

