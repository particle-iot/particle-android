package io.particle.mesh.setup.flow.setupsteps

import io.particle.firmwareprotos.ctrl.wifi.WifiNew.GetKnownNetworksReply
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent


class StepRetrieveWifiNetworks(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        flowUi.showGlobalProgressSpinner(true)

        val response = ctxs.requireTargetXceiver()
            .sendGetKnownNetworksRequest()
            .throwOnErrorOrAbsent()

        val networksList: List<GetKnownNetworksReply.Network> = response.networksList
        ctxs.wifi.updateTargetKnownWifiNetworks(networksList)

        flowUi.showGlobalProgressSpinner(false)
    }

}
