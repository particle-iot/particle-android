package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.models.ParticleSimStatus
import io.particle.mesh.setup.flow.ExceptionType.ERROR_RECOVERABLE
import io.particle.mesh.setup.flow.FlowException
import io.particle.mesh.setup.flow.MeshSetupFlowException
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts


class StepEnsureSimActivationStatusUpdated(private val cloud: ParticleCloud) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.cellular.isSimActivatedLD.value == true) {
            return
        }

        // is SIM activated
        val statusAndMsg = cloud.checkSim(ctxs.cellular.targetDeviceIccid.value!!)

        val isActive = when (statusAndMsg.first) {
            ParticleSimStatus.READY_TO_ACTIVATE -> false

            ParticleSimStatus.ACTIVATED_FREE,
            ParticleSimStatus.ACTIVATED -> true

            ParticleSimStatus.NOT_FOUND,
            ParticleSimStatus.NOT_OWNED_BY_USER,
            ParticleSimStatus.ERROR -> throw MeshSetupFlowException(statusAndMsg.second)
        }

        ctxs.cellular.updateIsSimActivated(isActive)
    }

}
