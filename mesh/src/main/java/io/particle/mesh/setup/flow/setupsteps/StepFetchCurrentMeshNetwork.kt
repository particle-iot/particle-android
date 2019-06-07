package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent


class StepFetchCurrentMeshNetwork(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        flowUi.showGlobalProgressSpinner(true)

        try {
            val reply = ctxs.requireTargetXceiver().sendGetNetworkInfo().throwOnErrorOrAbsent()
            ctxs.mesh.currentlyJoinedNetwork = reply.network

        } catch (ex: Exception) {
            ctxs.mesh.currentlyJoinedNetwork = null
        }
    }

}
