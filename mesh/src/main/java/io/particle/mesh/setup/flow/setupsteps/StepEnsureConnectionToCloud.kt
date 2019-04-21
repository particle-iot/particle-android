package io.particle.mesh.setup.flow.setupsteps

import io.particle.firmwareprotos.ctrl.cloud.Cloud.ConnectionStatus
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.context.SetupContexts
import kotlinx.coroutines.delay
import mu.KotlinLogging


class StepEnsureConnectionToCloud : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        // FIXME: these delays should probably live in the flows themselves,
        // using a "StepDelay" setup step
        delay(1000)

        var millis = 0
        val limitMillis = 1000 * 45 // 45 seconds
        while (millis < limitMillis) {
            delay(5000)
            millis += 5000
            val reply = ctxs.requireTargetXceiver().sendGetConnectionStatus().throwOnErrorOrAbsent()
            log.info { "reply=$reply" }
            if (reply.status == ConnectionStatus.CONNECTED) {
                ctxs.targetDevice.updateDeviceConnectedToCloudLD(true)
                return
            }
        }

        throw MeshSetupFlowException("Error ensuring connection to cloud")
    }

}