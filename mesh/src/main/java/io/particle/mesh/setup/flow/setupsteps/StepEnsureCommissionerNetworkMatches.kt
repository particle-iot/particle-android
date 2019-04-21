package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.R
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.DialogSpec.ResDialogSpec
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.modules.meshsetup.MeshNetworkToJoin.SelectedNetwork
import kotlinx.coroutines.delay
import mu.KotlinLogging


class StepEnsureCommissionerNetworkMatches(
    private val flowUi: FlowUiDelegate,
    private val cloud: ParticleCloud
) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        val commissioner = ctxs.requireCommissionerXceiver()
        val reply = commissioner.sendGetNetworkInfo().throwOnErrorOrAbsent()

        val commissionerNetwork = reply.network
        val toJoinLD = ctxs.mesh.meshNetworkToJoinLD
        val toJoin = (ctxs.mesh.meshNetworkToJoinLD.value!! as SelectedNetwork)

        if (commissionerNetwork?.extPanId == toJoin.networkToJoin.extPanId) {
            toJoinLD.nonNull(scopes).runBlockOnUiThreadAndAwaitUpdate(scopes) {
                // update the network to one which has the network ID
                ctxs.mesh.updateSelectedMeshNetworkToJoin(commissionerNetwork)
            }
            // update the network to be joined
            return  // it's a match; we're done.
        }

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
        delay(500)

        throw CommissionerNetworkDoesNotMatchException()
    }

}
