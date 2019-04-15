package io.particle.mesh.setup.flow.setupsteps

import androidx.annotation.WorkerThread
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.DeviceConnector
import io.particle.mesh.setup.flow.FailedToConnectException
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.modules.FlowUiDelegate
import kotlinx.coroutines.delay


class StepConnectToTargetDevice(
    private val flowUi: FlowUiDelegate,
    private val deviceConnector: DeviceConnector
) : MeshSetupStep() {

    @WorkerThread
    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.ble.targetDevice.transceiverLD.value?.isConnected == true) {
            return
        }

        if (!ctxs.ble.connectingToTargetUiShown) {
            flowUi.showTargetPairingProgressUi()
            ctxs.ble.connectingToTargetUiShown = true
        }

        val transceiver = scopes.withMain {
            try {
                deviceConnector.connect(ctxs.ble.targetDevice.barcode.value!!, "target", scopes)
            } catch (ex: Exception) {
                return@withMain null
            }
        }

        if (transceiver == null) {
            throw FailedToConnectException()
        } else {
            // don't move any further until the value is set on the LiveData
            ctxs.ble.targetDevice.transceiverLD
                .nonNull(scopes)
                .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                    ctxs.ble.targetDevice.updateDeviceTransceiver(transceiver)
                }
            delay(5000)
        }
    }

}