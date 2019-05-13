package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.common.android.livedata.awaitUpdate
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.TerminateFlowException
import io.particle.mesh.setup.flow.context.SetupContexts


class StepShowSimUnpauseUi(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        flowUi.showControlPanelUnpauseUi()

        val newLimit: Int? = ctxs.cellular.newSelectedDataLimitLD
            .nonNull(scopes)
            .awaitUpdate(scopes)

        if (newLimit == null || newLimit < 1) {
            throw TerminateFlowException("No new data limit selected (value=$newLimit)")
        }
    }
}
