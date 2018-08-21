package io.particle.particlemesh.meshsetup.ui


import androidx.navigation.fragment.findNavController
import io.particle.particlemesh.common.QATool
import io.particle.sdk.app.R


class ScanJoinerCodeIntroFragment : ScanIntroBaseFragment() {

    override val layoutId = R.layout.fragment_scan_code_intro

    override fun onBarcodeUpdated(barcodeData: BarcodeData?) {
        if (barcodeData == null) {
            QATool.illegalState("Received null barcode?!")
            return
        }

        setupController.setJoinerBarcode(barcodeData)
        findNavController().navigate(
                R.id.action_scanCodeIntroFragment_to_BLEPairingProgressFragment
        )
    }

}



