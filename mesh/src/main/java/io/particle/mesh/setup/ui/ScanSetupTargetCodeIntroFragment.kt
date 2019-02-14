package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.View
import io.particle.mesh.common.QATool
import io.particle.mesh.setup.ui.BarcodeData.CompleteBarcodeData
import io.particle.mesh.R
import kotlinx.android.synthetic.main.fragment_scan_code_intro.*


class ScanSetupTargetCodeIntroFragment : ScanIntroBaseFragment() {

    override val layoutId = R.layout.fragment_scan_code_intro

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setup_header_text.setText( R.string.pair_xenon_with_your_phone)
        textView.setText(R.string.p_scancodeintro_text_1)
    }

    override fun onBarcodeUpdated(barcodeData: CompleteBarcodeData?) {
        if (barcodeData == null) {
            QATool.illegalState("Received null barcode?!")
            return
        }

        val flowManager = FlowManagerAccessModel.getViewModel(this).flowManager!!
        flowManager.bleConnectionModule.updateTargetDeviceBarcode(barcodeData)
    }

}
