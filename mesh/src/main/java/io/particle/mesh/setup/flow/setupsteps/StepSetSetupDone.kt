package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent

class StepSetSetupDone : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        val xceiver = ctxs.requireTargetXceiver()
        xceiver.sendSetDeviceSetupDone(true).throwOnErrorOrAbsent()
    }

}