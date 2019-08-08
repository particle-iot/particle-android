package io.particle.mesh.setup.flow.setupsteps

import io.particle.firmwareprotos.ctrl.Config.Feature
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent


class StepGetEthernetPinStatus(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        flowUi.showGlobalProgressSpinner(true)
        val target = ctxs.targetDevice.transceiverLD.value!!

        val detectReply = target.sendGetFeature(Feature.ETHERNET_DETECTION).throwOnErrorOrAbsent()
        ctxs.device.isEthernetEnabled = detectReply.enabled
        flowUi.showGlobalProgressSpinner(false)
    }

}
