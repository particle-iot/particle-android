package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts

class StepPopBackStack(
    private val flowUi: FlowUiDelegate,
    private val popToRoot: Boolean = false
) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (!popToRoot) {
            flowUi.popBackStack()
            flowUi.showGlobalProgressSpinner(false)
            return
        }

        var popped = true
        while (popped) {
            popped = flowUi.popBackStack()
        }
        flowUi.showGlobalProgressSpinner(false)
    }

}