package io.particle.mesh.setup.flow.setupsteps

import io.particle.firmwareprotos.ctrl.Config.Feature
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.context.SetupContexts


class StepSetEthernetPinStatus(val flowUi: FlowUiDelegate) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        val activate = ctxs.device.shouldEthernetBeEnabled == true
        flowUi.showGlobalProgressSpinner(true)

        val target = ctxs.requireTargetXceiver()
        target.sendSetFeature(Feature.ETHERNET_DETECTION, activate).throwOnErrorOrAbsent()
        target.sendStopListeningMode().throwOnErrorOrAbsent()

        flowUi.showGlobalProgressSpinner(false)
        val toggleString = if (activate) "activated" else "deactivated"
        flowUi.showCongratsScreen("Ethernet pins $toggleString", PostCongratsAction.RESET_TO_START)
    }

}
