package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.View
import com.squareup.phrase.Phrase
import io.particle.mesh.common.QATool
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_scan_code_intro.*


class ScanSetupTargetCodeIntroFragment : ScanIntroBaseFragment() {

    override val layoutId = R.layout.fragment_scan_code_intro

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val productName = flowManagerVM.flowManager!!.getTypeName()

        setup_header_text.text = Phrase.from(view, R.string.pair_xenon_with_your_phone)
                .put("product_type", productName)
                .format()

        textView.text = Phrase.from(view, R.string.p_scancodeintro_text_1)
                .put("product_type", productName)
                .format()
    }

    override fun onBarcodeUpdated(barcodeData: BarcodeData?) {
        if (barcodeData == null) {
            QATool.illegalState("Received null barcode?!")
            return
        }

        val flowManager = FlowManagerAccessModel.getViewModel(this).flowManager!!
        flowManager.bleConnectionModule.updateTargetDeviceBarcode(barcodeData)
    }

}
