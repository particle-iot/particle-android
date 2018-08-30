package io.particle.mesh.setup.ui


import android.arch.lifecycle.Observer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.utils.safeToast
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_ble_pairing_progress.*
import mu.KotlinLogging


class BLEPairingProgressFragment : BaseMeshSetupFragment() {

    private val log = KotlinLogging.logger {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        flowManagerVM.flowManager!!.targetDeviceTransceiverLD.observe(this, Observer {
            onDeviceConnected(it)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ble_pairing_progress, container, false)
    }

    private fun onDeviceConnected(transceiver: ProtocolTransceiver?) {
        progressBar.visibility = View.GONE
        state_success.visibility = View.VISIBLE

        status_text.text = "Successfully paired with device ${transceiver?.deviceName ?: '?'}"

        requireActivity().safeToast("Connected device!")

//        findNavController().navigate(
//                R.id.action_BLEPairingProgressFragment_to_scanForMeshNetworksFragment
//        )
    }

}
