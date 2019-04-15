package io.particle.mesh.setup.flow.setupsteps

import androidx.annotation.WorkerThread
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent


class StepFetchDeviceId : MeshSetupStep() {

    @WorkerThread
    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.ble.targetDevice.deviceId != null) {
            return
        }

        val deviceIdReply = ctxs.requireTargetXceiver().sendGetDeviceId().throwOnErrorOrAbsent()
        ctxs.ble.targetDevice.deviceId = deviceIdReply.id
    }

}