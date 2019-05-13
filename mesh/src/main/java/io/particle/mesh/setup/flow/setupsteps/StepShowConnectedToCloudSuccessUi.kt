package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.R
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.toUserFacingName
import kotlinx.coroutines.delay


class StepShowConnectedToCloudSuccessUi(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.cloud.connectedToCloudCongratsUiShown) {
            return
        }

        ctxs.cloud.connectedToCloudCongratsUiShown = true

        val template = flowUi.getString(R.string.p_congrats_claimed)
        val nameRes = ctxs.targetDevice.deviceType?.toUserFacingName()
        val name = flowUi.getString(nameRes!!)
        val msg = template.format(name)
        flowUi.showCongratsScreen(msg)
        delay(1900)
        flowUi.popBackStack()
    }

}