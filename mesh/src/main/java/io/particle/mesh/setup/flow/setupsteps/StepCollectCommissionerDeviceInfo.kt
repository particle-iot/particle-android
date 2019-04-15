package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.NoBarcodeScannedException
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.modules.FlowUiDelegate


class StepCollectCommissionerDeviceInfo(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        val barcodeLD = ctxs.ble.commissioner.barcode
        if (barcodeLD.value != null) {
            return
        }

        val barcodeData = barcodeLD.nonNull(scopes).runBlockOnUiThreadAndAwaitUpdate(scopes) {
            flowUi.getCommissionerBarcode()
        }

        if (barcodeData == null) {
            throw NoBarcodeScannedException()
        }
    }

}