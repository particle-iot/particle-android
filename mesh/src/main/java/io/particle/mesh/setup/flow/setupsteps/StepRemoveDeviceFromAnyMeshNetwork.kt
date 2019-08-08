package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.UnableToLeaveNetworkException
import io.particle.mesh.setup.flow.context.SetupContexts


class StepRemoveDeviceFromAnyMeshNetwork(
    private val cloud: ParticleCloud,
    private val flowUi: FlowUiDelegate
) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        ctxs.mesh.checkedForExistingNetwork = true

        try {
            flowUi.showGlobalProgressSpinner(true)
            cloud.removeDeviceFromAnyMeshNetwork(ctxs.targetDevice.deviceId!!)
        } catch (ex: Exception) {
            throw UnableToLeaveNetworkException(ex)
        }
    }
}