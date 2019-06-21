package io.particle.mesh.setup.flow.setupsteps

import androidx.annotation.WorkerThread
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.context.SetupContexts
import mu.KotlinLogging


class StepConnectToTargetDevice(
    private val flowUi: FlowUiDelegate,
    private val deviceConnector: DeviceConnector
) : MeshSetupStep() {

    @WorkerThread
    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.targetDevice.transceiverLD.value?.isConnected == true) {
            return
        }

        if (!ctxs.ble.connectingToTargetUiShown) {
            flowUi.showTargetPairingProgressUi()
            ctxs.ble.showingConnectingToTargetUi = true
            ctxs.ble.connectingToTargetUiShown = true
        } else if (!ctxs.ble.showingConnectingToTargetUi)  {
            flowUi.showGlobalProgressSpinner(true)
        }

        var error: Exception? = null
        val transceiver: ProtocolTransceiver? = scopes.withMain(12000) {
            try {
                deviceConnector.connect(ctxs.targetDevice.barcode.value!!, "target", scopes)
            } catch (ex: Exception) {
                error = ex
                null
            }
        }

        if (transceiver == null) {
            if (error is MeshSetupFlowException) {
                throw error!!
            } else {
                throw FailedToConnectException(error)
            }
        } else {
            // don't move any further until the value is set on the LiveData
            ctxs.targetDevice.transceiverLD
                .nonNull(scopes)
                .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                    ctxs.targetDevice.updateDeviceTransceiver(transceiver)
                }
        }

        ctxs.ble.showingConnectingToTargetUi = false
    }

}