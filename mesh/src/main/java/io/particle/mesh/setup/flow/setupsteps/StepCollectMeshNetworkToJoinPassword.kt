package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.R
import io.particle.mesh.common.Result
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.flow.FlowException
import io.particle.mesh.setup.flow.MeshSetupFlowException
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.modules.FlowUiDelegate
import io.particle.mesh.setup.ui.DialogSpec.ResDialogSpec
import mu.KotlinLogging


class StepCollectMeshNetworkToJoinPassword(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.mesh.targetDeviceMeshNetworkToJoinCommissionerPassword.value.truthy()) {
            return
        }

        val passwordLd = ctxs.mesh.targetDeviceMeshNetworkToJoinCommissionerPassword
        val password = passwordLd.nonNull(scopes).runBlockOnUiThreadAndAwaitUpdate(scopes) {
            if (!ctxs.mesh.shownNetworkPasswordUi) {
                flowUi.collectPasswordForMeshToJoin()
                ctxs.mesh.shownNetworkPasswordUi = true
            }
        }

        if (password == null) {
            throw MeshSetupFlowException("Error while collecting mesh network password")
        }

        flowUi.showGlobalProgressSpinner(true)
        val commissioner = ctxs.requireCommissionerXceiver()
        val sendAuthResult = commissioner.sendAuth(password)
        when (sendAuthResult) {
            is Result.Present -> return
            is Result.Error,
            is Result.Absent -> {
                ctxs.mesh.updateTargetDeviceMeshNetworkToJoinCommissionerPassword(null)

                val result = flowUi.dialogTool.dialogResultLD
                    .nonNull(scopes)
                    .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                        flowUi.dialogTool.newDialogRequest(
                            ResDialogSpec(
                                R.string.p_mesh_network_password_is_incorrect,
                                android.R.string.ok
                            )
                        )
                    }

                log.info { "result from awaiting on 'commissioner not on network to be joined' dialog: $result" }
                flowUi.dialogTool.clearDialogResult()

                throw MeshSetupFlowException("Bad commissioner password")
            }
        }
    }

}
