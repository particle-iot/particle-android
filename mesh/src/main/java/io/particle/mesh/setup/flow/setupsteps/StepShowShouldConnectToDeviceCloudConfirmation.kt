package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.modules.FlowUiDelegate


class StepShowShouldConnectToDeviceCloudConfirmation(
    private val flowUi: FlowUiDelegate
) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        val confirmationLD = ctxs.cloud.shouldConnectToDeviceCloudConfirmed
        if (confirmationLD.value == true) {
            return
        }

        confirmationLD.nonNull(scopes).runBlockOnUiThreadAndAwaitUpdate(scopes) {
            flowUi.showInternetConnectedConnectToDeviceCloudIntroUi()
        }
    }

}