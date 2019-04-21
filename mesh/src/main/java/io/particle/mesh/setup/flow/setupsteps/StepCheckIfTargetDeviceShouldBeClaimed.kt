package io.particle.mesh.setup.flow.setupsteps

import androidx.annotation.WorkerThread
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.FlowUiDelegate
import mu.KotlinLogging


class StepCheckIfTargetDeviceShouldBeClaimed(
    private val cloud: ParticleCloud,
    private val flowUi: FlowUiDelegate
) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    @WorkerThread
    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.mesh.targetJoinedSuccessfully) {
            log.info { "Skipping because we've already joined the mesh successfully" }
            return
        }

        if (ctxs.targetDevice.shouldBeClaimed == null) {
            checkTargetDeviceIsClaimed(ctxs, scopes)
        } else if (ctxs.targetDevice.shouldBeClaimed == true && ctxs.cloud.claimCode == null) {
            fetchClaimCode(ctxs)
        }
    }

    private suspend fun checkTargetDeviceIsClaimed(ctxs: SetupContexts, scopes: Scopes) {
        log.info { "checkTargetDeviceIsClaimed()" }

        val targetDeviceId = ctxs.targetDevice.deviceId!!
        val userOwnsDevice = cloud.userOwnsDevice(targetDeviceId)
        log.info { "User owns device?: $userOwnsDevice" }
        if (userOwnsDevice) {
            val device = cloud.getDevice(targetDeviceId)
            ctxs.targetDevice.currentDeviceName = device.name
            ctxs.targetDevice.updateIsClaimed(true)
            ctxs.targetDevice.shouldBeClaimed = false
            return
        }

        ctxs.targetDevice.updateIsClaimed(false)
        ctxs.targetDevice.shouldBeClaimed = true
        // run through step again...
        doRunStep(ctxs, scopes)
    }

    private fun fetchClaimCode(ctxs: SetupContexts) {
        if (ctxs.cloud.claimCode == null) {
            log.info { "Fetching new claim code" }
            try {
                flowUi.showGlobalProgressSpinner(true)
                ctxs.cloud.claimCode = cloud.generateClaimCode().claimCode
            } finally {
                flowUi.showGlobalProgressSpinner(true)
            }
        }
    }

}