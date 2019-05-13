package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.DialogSpec.StringDialogSpec
import io.particle.mesh.setup.flow.ExceptionType.ERROR_FATAL
import io.particle.mesh.setup.flow.context.SetupContexts
import mu.KotlinLogging


class StepShowMeshInspectNetworkUi(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}


    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.mesh.currentlyJoinedNetwork == null) {
            showNoMeshNetworkDialog(scopes)
        } else {
            flowUi.showMeshInspectNetworkUi()
        }
    }

    private suspend fun showNoMeshNetworkDialog(scopes: Scopes) {
        val dialogResult = flowUi.dialogTool.dialogResultLD
            .nonNull(scopes)
            .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                flowUi.dialogTool.newDialogRequest(
                    StringDialogSpec("This device is not part of any mesh network.")
                )
            }

        log.info { "Result for leave network confirmation dialog: $dialogResult" }

        flowUi.dialogTool.clearDialogResult()

        flowUi.popBackStack()
    }

}
