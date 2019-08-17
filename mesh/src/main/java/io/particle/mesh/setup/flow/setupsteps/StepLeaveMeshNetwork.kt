package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent


class StepLeaveMeshNetwork(
    private val cloud: ParticleCloud,
    private val flowUi: FlowUiDelegate,
    // if true, send LeaveNetworkRequest to the local device.  If false, the device will only be
    // removed from the cloud's picture of any mesh the device is part of
    private val leaveMeshOnLocalDevice: Boolean
) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        flowUi.showGlobalProgressSpinner(true)
        try {
            cloud.removeDeviceFromAnyMeshNetwork(ctxs.targetDevice.deviceId!!)
            if (leaveMeshOnLocalDevice) {
                ctxs.requireTargetXceiver().sendLeaveNetwork().throwOnErrorOrAbsent()
            }
        } finally {
            flowUi.showGlobalProgressSpinner(false)
        }
    }

}
