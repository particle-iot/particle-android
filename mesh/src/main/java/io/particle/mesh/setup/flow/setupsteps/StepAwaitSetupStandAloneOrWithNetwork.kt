package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.ExpectedFlowException
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.FlowUiDelegate


class StepAwaitSetupStandAloneOrWithNetwork(
    val cloud: ParticleCloud,
    val flowUi: FlowUiDelegate
) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.device.networkSetupTypeLD.value != null) {
            return
        }

        ctxs.device.networkSetupTypeLD.nonNull(scopes).runBlockOnUiThreadAndAwaitUpdate(scopes) {
            flowUi.getNetworkSetupType()
        }

        // reset flow again!
        val networkSetupType = ctxs.device.networkSetupTypeLD.value
        throw ExpectedFlowException("Network setup type selected: $networkSetupType")
    }

}
