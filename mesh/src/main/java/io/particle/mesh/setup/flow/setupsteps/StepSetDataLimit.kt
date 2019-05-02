package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import mu.KotlinLogging


class StepSetDataLimit(private val cloud: ParticleCloud) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        val iccid = ctxs.targetDevice.iccid!!
        val limit = ctxs.cellular.newSelectedDataLimitLD.value!!
        log.info { "Setting data limit for $iccid to $limit" }
        cloud.setDataLimit(iccid, limit)
    }
}