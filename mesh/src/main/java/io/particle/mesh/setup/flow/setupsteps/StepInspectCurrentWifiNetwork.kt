package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.common.Result.Absent
import io.particle.mesh.common.Result.Error
import io.particle.mesh.common.Result.Present
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.context.SetupContexts
import kotlinx.coroutines.delay


class StepInspectCurrentWifiNetwork(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        delay(2000) // delay to allow the device time to reconnect to wifi after listening mode

        val reply = ctxs.requireTargetXceiver().sendGetCurrentWifiNetworkRequest()
        when (reply) {
            is Present -> flowUi.showInspectCurrentWifiNetworkUi(reply.value)
            is Absent -> throw IllegalStateException(
                "Received blank reply from device when requesting current Wi-Fi network"
            )
            is Error -> {
                flowUi.dialogTool.newSnackbarRequest(
                    "${ctxs.targetDevice.currentDeviceName} is not currently on any Wi-Fi network"
                )
                flowUi.popBackStack()
            }
        }
    }

}