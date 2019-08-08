package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging


class StepShowMeshInspectNetworkUi(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.mesh.currentlyJoinedNetwork == null) {
            flowUi.showMeshOptionsUi(ctxs.ble.connectingToTargetUiShown)
        } else {
            flowUi.showMeshInspectNetworkUi(ctxs.ble.connectingToTargetUiShown)
        }
    }

}
