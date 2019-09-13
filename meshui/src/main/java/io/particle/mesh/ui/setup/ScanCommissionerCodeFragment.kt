package io.particle.mesh.ui.setup

import androidx.fragment.app.FragmentActivity
import com.squareup.phrase.Phrase
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.mesh.setup.BarcodeData.CompleteBarcodeData
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.R
import kotlinx.android.synthetic.main.fragment_scan_commissioner_code.*
import mu.KotlinLogging


class ScanCommissionerCodeFragment :  ScanIntroBaseFragment() {

    override val layoutId: Int = R.layout.fragment_scan_commissioner_code

    private val log = KotlinLogging.logger {}


    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)
        val productName = getUserFacingTypeName()
        p_commissionerscan_hint_text.text = Phrase.from(view, R.string.p_scancommissionercode_tip_content)
            .put("product_type", productName)
            .format()
    }

    override fun onBarcodeUpdated(barcodeData: CompleteBarcodeData?) {
        log.info { "onBarcodeUpdated(COMMISH): $barcodeData" }
        flowScopes.onWorker {
            flowUiListener?.commissioner?.updateBarcode(barcodeData!!, ParticleCloudSDK.getCloud())
        }
    }

}