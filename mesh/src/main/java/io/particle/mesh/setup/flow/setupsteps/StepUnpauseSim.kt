package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import mu.KotlinLogging


class StepUnpauseSim(private val cloud: ParticleCloud) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        log.info {
            "THIS IS WHERE WE WOULD UNPAUSE AND SET LIMIT TO: " +
                    "${ctxs.cellular.newSelectedDataLimitLD.value}"
        }

        // FIXME: UNCOMMENT!
//        cloud.unpauseSim(
//            ctxs.targetDevice.iccid!!,
//            ctxs.cellular.newSelectedDataLimitLD.value!!
//        )
    }

}