package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.common.truthy
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.context.SetupContexts


class StepCreateNewMeshNetworkOnLocalDevice : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.mesh.networkCreatedOnLocalDeviceLD.value.truthy()) {
            return
        }

        val name = ctxs.mesh.newNetworkNameLD.value!!
        val password = ctxs.mesh.newNetworkPasswordLD.value!!
        val networkId = ctxs.mesh.newNetworkIdLD.value!!

        val tx = ctxs.requireTargetXceiver()
        val reply = tx.sendCreateNetwork(name, password, networkId).throwOnErrorOrAbsent()

        if (reply.network.networkId.toUpperCase() != networkId.toUpperCase()) {
            throw MeshSetupFlowException("Network ID received from CreateNetwork does not match")
        }

        ctxs.mesh.updateNetworkCreatedOnLocalDeviceLD(true)
    }

}