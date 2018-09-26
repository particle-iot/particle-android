package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_assisting_device_pairing_progress.*
import mu.KotlinLogging


class AssistingDevicePairingProgressFragment : BaseMeshSetupFragment() {

    private val log = KotlinLogging.logger {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        flowManagerVM.flowManager!!.bleConnectionModule.commissionerTransceiverLD.observe(
                this,
                Observer { onAssistingDeviceConnected() }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_assisting_device_pairing_progress, container, false)
    }

    private fun onAssistingDeviceConnected() {
        log.info { "onAssistingDeviceConnected()" }

        progressBar.visibility = View.GONE
        state_success.visibility = View.VISIBLE
        p_pairingprogress_congrats_text.visibility = View.VISIBLE

        val xceiver = flowManagerVM.flowManager!!.bleConnectionModule.commissionerTransceiverLD.value
        val msg = "Successfully paired with device ${xceiver?.deviceName ?: '?'}"
        status_text.text = msg
    }



}
