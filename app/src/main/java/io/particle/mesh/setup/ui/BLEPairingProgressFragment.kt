package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.squareup.phrase.Phrase
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_ble_pairing_progress.*
import kotlinx.android.synthetic.main.fragment_get_ready_for_setup.view.*
import mu.KotlinLogging


class BLEPairingProgressFragment : BaseMeshSetupFragment() {

    private val log = KotlinLogging.logger {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        flowManagerVM.flowManager!!.bleConnectionModule.targetDeviceTransceiverLD.observe(
                this,
                Observer {
                    onTargetDeviceConnected()
                })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ble_pairing_progress, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        status_text.text = Phrase.from(view, R.string.pairing_with_your_device)
                .put("product_type", flowManagerVM.flowManager!!.getTypeName(view.context))
                .format()
    }

    private fun onTargetDeviceConnected() {
        log.info { "onDeviceConnected()" }

        progressBar.visibility = View.GONE
        state_success.visibility = View.VISIBLE
        p_pairingprogress_congrats_text.visibility = View.VISIBLE

        val xceiver = flowManagerVM.flowManager!!.bleConnectionModule.targetDeviceTransceiverLD.value
        val msg = "Successfully paired with device ${xceiver?.deviceName ?: '?'}"
        status_text.text = msg
    }

}
