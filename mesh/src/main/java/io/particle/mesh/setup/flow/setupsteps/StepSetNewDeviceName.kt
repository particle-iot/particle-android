package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.FlowException
import io.particle.mesh.setup.flow.MeshSetupFlowException
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.modules.FlowUiDelegate


class StepSetNewDeviceName(
    private val flowUi: FlowUiDelegate,
    private val cloud: ParticleCloud
) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.cloud.isTargetDeviceNamedLD.value == true) {
            return
        }

        val nameToAssign = ctxs.cloud.targetDeviceNameToAssignLD
            .nonNull(scopes)
            .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                flowUi.showNameDeviceUi()
            }

        if (nameToAssign == null) {
            throw MeshSetupFlowException("Error ensuring target device is named")
        }

        try {
            flowUi.showGlobalProgressSpinner(true)

            val targetDeviceId = ctxs.ble.targetDevice.deviceId!!
            val joiner = cloud.getDevice(targetDeviceId)
            joiner.name = nameToAssign
            ctxs.cloud.updateIsTargetDeviceNamed(true)

        } catch (ex: Exception) {
            throw MeshSetupFlowException("Unable to rename device", ex)

        } finally {
            flowUi.showGlobalProgressSpinner(true)
        }
    }
}