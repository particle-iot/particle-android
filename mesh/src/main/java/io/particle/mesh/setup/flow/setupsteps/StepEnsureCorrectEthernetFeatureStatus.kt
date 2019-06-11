package io.particle.mesh.setup.flow.setupsteps

import io.particle.firmwareprotos.ctrl.Config.DeviceMode
import io.particle.firmwareprotos.ctrl.Config.Feature
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.context.SetupContexts
import kotlinx.coroutines.delay


class StepEnsureCorrectEthernetFeatureStatus : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        // only run this step if we've been asked to detect ethernet,
        // and we haven't already attempted to do so
        if (!ctxs.device.shouldDetectEthernet || ctxs.device.ethernetDetectionComplete) {
            return
        }

        val target = ctxs.targetDevice.transceiverLD.value!!

        val detectReply = target.sendGetFeature(Feature.ETHERNET_DETECTION).throwOnErrorOrAbsent()
        ctxs.device.ethernetDetectionComplete = true

        if (!detectReply.enabled) {
            target.sendSetFeature(Feature.ETHERNET_DETECTION, true).throwOnErrorOrAbsent()
            ctxs.device.isDetectEthernetSent = true
            target.sendStartupMode(DeviceMode.LISTENING_MODE).throwOnErrorOrAbsent()
            target.sendReset().throwOnErrorOrAbsent()

            target.disconnect()
            delay(4000)

            throw MeshSetupFlowException(
                message = "Resetting device to enable ethernet detection!",
                severity = ExceptionType.EXPECTED_FLOW
            )
        }
    }

}