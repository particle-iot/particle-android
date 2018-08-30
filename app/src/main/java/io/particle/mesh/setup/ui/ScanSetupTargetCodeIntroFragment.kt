package io.particle.mesh.setup.ui


import io.particle.mesh.common.QATool
import io.particle.sdk.app.R


class ScanSetupTargetCodeIntroFragment : ScanIntroBaseFragment() {

    override val layoutId = R.layout.fragment_scan_code_intro

    override fun onBarcodeUpdated(barcodeData: BarcodeData?) {
        if (barcodeData == null) {
            QATool.illegalState("Received null barcode?!")
            return
        }

        val flowManager = FlowManagerAccessModel.getViewModel(this).flowManager
        flowManager?.updateTargetDeviceBarcode(barcodeData)
    }

}
