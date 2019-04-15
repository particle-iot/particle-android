package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.ExceptionType.ERROR_FATAL
import io.particle.mesh.setup.flow.ExceptionType.ERROR_RECOVERABLE
import io.particle.mesh.setup.flow.context.SetupContexts

class StepEnsureSimActivated(private val cloud: ParticleCloud) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.cellular.isSimActivatedLD.value == true) {
            return
        }

        for (i in 0..2) {

            val statusCode = doActivateSim(ctxs)
            if (statusCode == 200) {
                ctxs.cellular.updateIsSimActivated(true)
                return

            } else if (statusCode == 504) {
                continue

            } else {
                throw FailedToActivateSimException()
            }
        }

        throw FailedToActivateSimException(ERROR_FATAL)
    }

    private fun doActivateSim(ctxs: SetupContexts): Int {
        val response = cloud.activateSim(ctxs.cellular.targetDeviceIccid.value!!)
        return response.status
    }

}
