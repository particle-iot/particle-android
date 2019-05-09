package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.setup.flow.FlowIntent
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import kotlinx.coroutines.delay


class StepShowTargetPairingSuccessful(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.flowIntent != FlowIntent.FIRST_TIME_SETUP) {
            return
        }
        val deviceName = ctxs.requireTargetXceiver().bleBroadcastName
        val shouldWaitBeforeAdvancingFlow = flowUi.onTargetPairingSuccessful(deviceName)
        if (shouldWaitBeforeAdvancingFlow) {
            delay(2000)
        }
    }

}