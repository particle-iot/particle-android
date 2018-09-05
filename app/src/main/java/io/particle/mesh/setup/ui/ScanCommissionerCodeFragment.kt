package io.particle.mesh.setup.ui

import io.particle.sdk.app.R
import mu.KotlinLogging


class ScanCommissionerCodeFragment :  ScanIntroBaseFragment() {

    override val layoutId: Int = R.layout.fragment_scan_commissioner_code

    private val log = KotlinLogging.logger {}

    override fun onBarcodeUpdated(barcodeData: BarcodeData?) {
        log.info { "onBarcodeUpdated(COMMISH): $barcodeData" }
        flowManagerVM.flowManager!!.bleConnectionModule.updateCommissionerBarcode(barcodeData!!)
    }

}