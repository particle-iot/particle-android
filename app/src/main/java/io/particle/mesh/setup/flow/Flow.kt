package io.particle.mesh.setup.flow

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.setup.flow.modules.cloudconnection.CloudConnectionModule

class Flow(
        private val flowManager: FlowManager,
        private val cloud: ParticleCloud
) {

    private val cloudConnModule = CloudConnectionModule(cloud)

    fun startFlow() {
        cloudConnModule.fetchClaimCode()
        flowManager.navigate(32)
        TODO("not implemented")
    }

    fun clearState() {
        // FIXME: implement!
    }


}