package io.particle.mesh.setup.ui

import android.os.Bundle
import android.view.View
import com.squareup.phrase.Phrase
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_scan_commissioner_code.*
import mu.KotlinLogging


class ScanCommissionerCodeFragment :  ScanIntroBaseFragment() {

    override val layoutId: Int = R.layout.fragment_scan_commissioner_code

    private val log = KotlinLogging.logger {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val productName = flowManagerVM.flowManager!!.getTypeName(view.context)

        textView.text = Phrase.from(view, R.string.p_scancommissionercode_tip_content)
                .put("product_type", productName)
                .format()

        setup_header_text.text = Phrase.from(view, R.string.pair_assisting_device_with_your_phone)
                .put("product_type", productName)
                .format()

        assistantText.text = Phrase.from(view, R.string.p_pairassistingdevice_subheader_1)
                .put("product_type", productName)
                .format()
    }

    override fun onBarcodeUpdated(barcodeData: BarcodeData?) {
        log.info { "onBarcodeUpdated(COMMISH): $barcodeData" }
        flowManagerVM.flowManager!!.bleConnectionModule.updateCommissionerBarcode(barcodeData!!)
    }

}