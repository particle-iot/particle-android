package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.PostCongratsAction


class StepShowSingleTaskCongratsScreen(
    private val flowUi: FlowUiDelegate,
    private val message: String? = null
) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (message != null) {
            ctxs.singleStepCongratsMessage = message
        }
        flowUi.showCongratsScreen(ctxs.singleStepCongratsMessage, PostCongratsAction.RESET_TO_START)
    }

}