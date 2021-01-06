package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.setup.flow.ExceptionType.ERROR_FATAL
import io.particle.mesh.setup.flow.ExceptionType.ERROR_RECOVERABLE
import io.particle.mesh.setup.flow.FailedToActivateSimException
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.retrySimAction

class StepEnsureSimActivated(private val cloud: ParticleCloud) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.targetDevice.isSimActivatedLD.value == true) {
            return
        }

        retrySimAction {
            doActivateSim(ctxs)
        }
    }

    private fun doActivateSim(ctxs: SetupContexts): Int {
        val response = cloud.activateSim(ctxs.targetDevice.iccid!!)
        return response.status
    }

    override fun wrapException(cause: Exception): Exception {
        return FailedToActivateSimException(ERROR_FATAL, cause)
    }
}
