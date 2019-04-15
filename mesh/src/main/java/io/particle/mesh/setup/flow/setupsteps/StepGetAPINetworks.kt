package io.particle.mesh.setup.flow.setupsteps

import androidx.annotation.WorkerThread
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts


class StepGetAPINetworks(private val cloud: ParticleCloud) : MeshSetupStep() {

    @WorkerThread
    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        val networks = cloud.getNetworks()
        ctxs.cloud.apiNetworks = networks
    }

}