package io.particle.mesh.setup.flow.setupsteps

import androidx.annotation.WorkerThread
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.UnableToGenerateClaimCodeException
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

        val userOwnsDevice = userOwnsDevice(ctxs)
        log.info { "User owns device?: $userOwnsDevice" }
        if (userOwnsDevice) {
            val device = cloud.getDevice(ctxs.targetDevice.deviceId!!)
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

    private fun userOwnsDevice(ctxs: SetupContexts): Boolean {
        if (ctxs.targetDevice.isClaimedLD.value == true) {
            return true
        }

        val targetDeviceId = ctxs.targetDevice.deviceId!!
        return cloud.userOwnsDevice(targetDeviceId)
    }

    private fun fetchClaimCode(ctxs: SetupContexts) {
        if (ctxs.cloud.claimCode == null) {
            log.info { "Fetching new claim code" }
            flowUi.showGlobalProgressSpinner(true)
            try {
                ctxs.cloud.claimCode = cloud.generateClaimCode().claimCode
            } catch (ex: Exception) {
                throw UnableToGenerateClaimCodeException(ex)
            }
        }
    }

}