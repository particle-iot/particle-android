package io.particle.mesh.ui.setup


import androidx.annotation.MainThread
import androidx.fragment.app.FragmentActivity
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.mesh.common.QATool
import io.particle.mesh.setup.BarcodeData.CompleteBarcodeData
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.R
import kotlinx.android.synthetic.main.fragment_scan_code_intro.*


class ScanSetupTargetCodeIntroFragment : ScanIntroBaseFragment() {

    override val layoutId = R.layout.fragment_scan_code_intro

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        setup_header_text.setText( R.string.pair_xenon_with_your_phone)
        textView.setText(R.string.p_scancodeintro_text_1)
    }

    @MainThread
    override fun onBarcodeUpdated(barcodeData: CompleteBarcodeData?) {
        if (barcodeData == null) {
            QATool.illegalState("Received null barcode?!")
            return
        }

        flowScopes.onWorker {
            flowUiListener?.targetDevice?.updateBarcode(barcodeData, ParticleCloudSDK.getCloud())
        }
    }

}
