package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleNetworkType
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.flow.Gen3ConnectivityType
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts


class StepCreateNewMeshNetworkOnCloud(private val cloud: ParticleCloud) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.mesh.newNetworkIdLD.value.truthy()) {
            return
        }

        val networkType = when (ctxs.targetDevice.connectivityType!!) {
            Gen3ConnectivityType.WIFI -> ParticleNetworkType.MICRO_WIFI
            Gen3ConnectivityType.CELLULAR -> ParticleNetworkType.MICRO_CELLULAR
            Gen3ConnectivityType.MESH_ONLY -> ParticleNetworkType.MICRO_WIFI
        }

        val networkResponse = cloud.registerMeshNetwork(
            ctxs.targetDevice.deviceId!!,
            networkType,
            ctxs.mesh.newNetworkNameLD.value!!
        )

        // set the network ID and wait for it to update
        ctxs.mesh.newNetworkIdLD.nonNull(scopes).runBlockOnUiThreadAndAwaitUpdate(scopes) {
            ctxs.mesh.updateNewNetworkIdLD(networkResponse.id)
        }
    }

}