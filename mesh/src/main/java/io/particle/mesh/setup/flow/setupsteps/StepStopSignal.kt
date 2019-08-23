package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.common.QATool
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent


class StepStopSignal : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.targetDevice.nyanSignalingStopped) {
            return // we're done
        }

        // stopping signaling is a convenience.  If it fails, phone home about it, but don't
        // interrupt the flow and don't waste time on retries by attempting to stop it again.
        try {
            ctxs.requireTargetXceiver().sendStopNyanSignaling().throwOnErrorOrAbsent()
        } catch (ex: Exception) {
            QATool.report(ex)
        } finally {
            ctxs.targetDevice.nyanSignalingStopped = true
        }
    }

}