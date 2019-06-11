package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.DialogSpec.StringDialogSpec
import io.particle.mesh.setup.flow.context.SetupContexts


class StepGetNewMeshNetworkName(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.mesh.newNetworkNameLD.value != null) {
            return
        }

        val requestedName = ctxs.mesh.newNetworkNameLD
            .nonNull(scopes)
            .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                flowUi.getNewMeshNetworkName()
            }

        val networks = ctxs.cloud.apiNetworks!!
        val error = when {
            requestedName.isNullOrBlank() -> NetworkNameTooShortException()
            networks.firstOrNull { it.name == requestedName } != null -> NameInUseException()
            else -> null
        }

        error?.let {
            flowUi.dialogTool.dialogResultLD
                .nonNull(scopes)
                .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                    flowUi.dialogTool.newDialogRequest(StringDialogSpec(error.userFacingMessage!!))
                }
            throw error
        }
    }
}