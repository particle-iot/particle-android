package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.DialogSpec.StringDialogSpec
import io.particle.mesh.setup.flow.context.SetupContexts


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


        if (nameToAssign.isNullOrBlank()) {
            val error = NameTooShortException()
            flowUi.dialogTool.dialogResultLD
                .nonNull(scopes)
                .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                    flowUi.dialogTool.newDialogRequest(StringDialogSpec(error.userFacingMessage!!))
                }
            throw error
        }

        try {
            flowUi.showGlobalProgressSpinner(true)

            val targetDeviceId = ctxs.targetDevice.deviceId!!
            val joiner = cloud.getDevice(targetDeviceId)
            joiner.name = nameToAssign
            ctxs.cloud.updateIsTargetDeviceNamed(true)

        } catch (ex: Exception) {
            throw MeshSetupFlowException(ex, userFacingMessage = "Unable to rename device")

        } finally {
            flowUi.showGlobalProgressSpinner(true)
        }
    }

    override fun wrapException(cause: Exception): Exception {
        return UnableToRenameDeviceException(cause)
    }
}