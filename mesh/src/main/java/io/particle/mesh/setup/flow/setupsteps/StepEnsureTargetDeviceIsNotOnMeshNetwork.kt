package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.firmwareprotos.ctrl.mesh.Mesh.GetNetworkInfoReply
import io.particle.mesh.R
import io.particle.mesh.common.Result
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.connection.ResultCode
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.DialogSpec.ResDialogSpec
import io.particle.mesh.setup.flow.ExceptionType.ERROR_FATAL
import io.particle.mesh.setup.flow.context.SetupContexts
import mu.KotlinLogging


class StepEnsureTargetDeviceIsNotOnMeshNetwork(
    private val cloud: ParticleCloud,
    private val dialogTool: DialogTool
) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.mesh.checkedForExistingNetwork) {
            return
        }

        val target = ctxs.requireTargetXceiver()
        val reply: Result<GetNetworkInfoReply, ResultCode> = target.sendGetNetworkInfo()
        val removeLocally = when (reply) {
            is Result.Absent -> throw MeshSetupFlowException("No result received when getting existing network")
            is Result.Present -> true
            is Result.Error -> {
                log.info { " Error when getting network info: ${reply.error}" }
                if (reply.error == ResultCode.NOT_FOUND) {
                    false
                } else {
                    throw MeshSetupFlowException("Error when getting existing network")
                }
            }
        }

        if (removeLocally) {
            val dialogResult = dialogTool.dialogResultLD
                .nonNull(scopes)
                .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                    dialogTool.newDialogRequest(
                        ResDialogSpec(
                            R.string.p_mesh_leavenetworkconfirmation_text,
                            R.string.p_mesh_leavenetworkconfirmation_action_leave_network,
                            R.string.p_mesh_action_exit_setup,
                            R.string.p_mesh_leavenetworkconfirmation_header
                        )
                    )
                }

            log.info { "Result for leave network confirmation dialog: $dialogResult" }
            dialogTool.clearDialogResult()

            when (dialogResult) {
                DialogResult.POSITIVE -> { /* no-op, continue flow */ }
                DialogResult.NEGATIVE -> throw MeshSetupFlowException(
                    "User does not want device to leave network; exiting setup",
                    severity = ERROR_FATAL
                )
                null -> throw MeshSetupFlowException(
                    "Unknown error when confirming leaving network"
                )
            }

            target.sendLeaveNetwork().throwOnErrorOrAbsent()
        }

        ctxs.mesh.checkedForExistingNetwork = true

        cloud.removeDeviceFromAnyMeshNetwork(ctxs.targetDevice.deviceId!!)
    }

}