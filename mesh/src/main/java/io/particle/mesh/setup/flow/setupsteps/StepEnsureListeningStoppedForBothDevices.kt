package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts


class StepEnsureListeningStoppedForBothDevices : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        ctxs.targetDevice.transceiverLD.value?.sendStopListeningMode()
        ctxs.commissioner.transceiverLD.value?.sendStopListeningMode()
    }

}