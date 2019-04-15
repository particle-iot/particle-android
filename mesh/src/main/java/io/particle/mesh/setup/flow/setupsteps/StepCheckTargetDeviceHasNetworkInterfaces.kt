package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts


class StepCheckTargetDeviceHasNetworkInterfaces : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {

        TODO()

//        if (context.targetDevice.activeInternetInterface == nil) {
//            getActiveInternetInterface()
//        } else if (context.targetDevice.activeInternetInterface! == .ppp && context.targetDevice.externalSim == nil) {
//            getTargetDeviceActiveSim()
//        } else if (context.targetDevice.activeInternetInterface! == .ppp && context.targetDevice.deviceICCID == nil) {
//            getTargetDeviceICCID()
//        } else if (context.targetDevice.activeInternetInterface! == .ppp && context.targetDevice.simActive == nil) {
//            getSimInfo()
//        } else {
//            stepCompleted()
//        }
    }

}