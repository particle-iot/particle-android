package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.TerminateFlowException
import io.particle.mesh.setup.flow.context.SetupContexts


class StepShowSimReactivateUi(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        val clicked = ctxs.cellular.changeSimStatusButtonClickedLD
            .nonNull(scopes)
            .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                flowUi.showControlPanelSimReactivateUi()
            }

        if (clicked != true) {
            throw TerminateFlowException("User did not click 'reactivate' button")
        }
    }

}
