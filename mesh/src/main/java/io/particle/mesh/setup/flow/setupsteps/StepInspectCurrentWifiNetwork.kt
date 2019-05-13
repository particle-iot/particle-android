package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent
import kotlinx.coroutines.delay


class StepInspectCurrentWifiNetwork(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        delay(2000) // delay to allow the device time to reconnect to wifi after listening mode

        val reply = ctxs.requireTargetXceiver()
            .sendGetCurrentWifiNetworkRequest()
            .throwOnErrorOrAbsent()

        flowUi.showInspectCurrentWifiNetworkUi(reply)
    }

}