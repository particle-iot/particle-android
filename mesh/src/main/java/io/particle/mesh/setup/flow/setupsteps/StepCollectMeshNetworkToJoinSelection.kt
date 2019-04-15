package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.modules.FlowUiDelegate


class StepCollectMeshNetworkToJoinSelection(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.mesh.targetDeviceMeshNetworkToJoinLD.value != null) {
            return
        }

        flowUi.getMeshNetworkToJoin()
        ctxs.mesh.targetDeviceMeshNetworkToJoinLD
            .nonNull(scopes)
            .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                // no-op; we're just awaiting the update.
            }
    }

}
