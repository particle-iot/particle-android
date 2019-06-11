package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.R.string
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.DialogSpec.ResDialogSpec
import io.particle.mesh.setup.flow.DialogSpec.StringDialogSpec
import io.particle.mesh.setup.flow.context.SetupContexts


class StepCollectCommissionerDeviceInfo(
    private val flowUi: FlowUiDelegate,
    private val cloud: ParticleCloud
) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        val barcodeLD = ctxs.commissioner.barcode
        if (barcodeLD.value != null) {
            return
        }

        val barcodeData = barcodeLD.nonNull(scopes).runBlockOnUiThreadAndAwaitUpdate(scopes) {
            flowUi.getCommissionerBarcode()
        }

        if (barcodeData == ctxs.targetDevice.barcode.value) {
            barcodeLD.runBlockOnUiThreadAndAwaitUpdate(scopes) {
                ctxs.commissioner.updateBarcode(null, cloud)
            }

            val error = SameDeviceScannedTwiceException()
            flowUi.dialogTool.dialogResultLD
                .nonNull(scopes)
                .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                    flowUi.dialogTool.newDialogRequest(
                        StringDialogSpec(error.userFacingMessage!!)
                    )
                }

            throw error
        }

        if (barcodeData == null) {
            throw NoBarcodeScannedException()
        }
    }

}