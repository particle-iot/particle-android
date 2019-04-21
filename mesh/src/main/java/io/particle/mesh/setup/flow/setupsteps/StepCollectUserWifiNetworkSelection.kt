package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.FlowUiDelegate
import mu.KotlinLogging


class StepCollectUserWifiNetworkSelection(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.wifi.targetWifiNetworkLD.value != null) {
            return
        }

        val network = ctxs.wifi.targetWifiNetworkLD
            .nonNull(scopes)
            .runBlockOnUiThreadAndAwaitUpdate(scopes) {
            flowUi.showScanForWifiNetworksUi()
        }

        log.info { "Selected network: $network" }
    }

}