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
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.meshsetup.MeshNetworkToJoin.SelectedNetwork
import kotlinx.coroutines.delay
import mu.KotlinLogging


class StepEnsureCommissionerNetworkMatches(
    private val flowUi: FlowUiDelegate,
    private val cloud: ParticleCloud
) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        val commissioner = ctxs.requireCommissionerXceiver()
        val reply: Result<GetNetworkInfoReply, ResultCode> = commissioner.sendGetNetworkInfo()

        when (reply) {
            is Result.Error,
            is Result.Absent -> {
                onNoMatch(ctxs, scopes)
            }
            is Result.Present -> { /* no-op */ }
        }

        val commissionerNetwork = reply.value!!.network
        val toJoinLD = ctxs.mesh.meshNetworkToJoinLD
        val toJoin = (toJoinLD.value!! as SelectedNetwork)

        if (commissionerNetwork?.extPanId == toJoin.networkToJoin.extPanId) {
            toJoinLD.nonNull(scopes).runBlockOnUiThreadAndAwaitUpdate(scopes) {
                // update the network to one which has the network ID
                ctxs.mesh.updateSelectedMeshNetworkToJoin(commissionerNetwork)
            }
        } else {
            onNoMatch(ctxs, scopes)
        }

    }

    private suspend fun onNoMatch(ctxs: SetupContexts, scopes: Scopes) {
        val commissioner = ctxs.requireCommissionerXceiver()

        commissioner.disconnect()
        ctxs.commissioner.updateDeviceTransceiver(null)
        ctxs.commissioner.updateBarcode(null, cloud)

        val result = flowUi.dialogTool.dialogResultLD
            .nonNull(scopes)
            .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                flowUi.dialogTool.newDialogRequest(
                    ResDialogSpec(
                        R.string.p_manualcommissioning_commissioner_candidate_not_on_target_network,
                        android.R.string.ok
                    )
                )
            }

        log.info { "result from awaiting on 'commissioner not on network to be joined' dialog: $result" }
        flowUi.dialogTool.clearDialogResult()
        flowUi.showGlobalProgressSpinner(true)
        delay(500)

        throw CommissionerNetworkDoesNotMatchException()
    }
}
