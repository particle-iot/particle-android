package io.particle.mesh.setup.flow.setupsteps

import io.particle.firmwareprotos.ctrl.Network.InterfaceType
import io.particle.mesh.common.Result.Absent
import io.particle.mesh.common.Result.Error
import io.particle.mesh.common.Result.Present
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent
import mu.KotlinLogging


class StepCheckTargetDeviceHasThreadInterface : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.mesh.hasThreadInterface != null) {
            return
        }

        val response = ctxs.requireTargetXceiver().sendGetInterfaceList().throwOnErrorOrAbsent()
        val hasThreadInterface = null != response.interfacesList.firstOrNull {
            it.type == InterfaceType.THREAD
        }

        ctxs.mesh.hasThreadInterface = hasThreadInterface
    }

}