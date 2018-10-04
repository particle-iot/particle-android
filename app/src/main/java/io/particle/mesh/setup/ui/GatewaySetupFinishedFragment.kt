package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.phrase.Phrase
import io.particle.mesh.common.QATool
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_gateway_setup_finished.view.*


class GatewaySetupFinishedFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_gateway_setup_finished, container, false)

        root.action_start_building.setOnClickListener { endSetup() }
        root.action_add_next_mesh_device.setOnClickListener { startNewFlow() }

        val cloudModule = flowManagerVM.flowManager!!.cloudConnectionModule
        var deviceName = cloudModule.targetDeviceNameToAssignLD.value ?: cloudModule.currentDeviceName.value
        if (deviceName == null) {
            QATool.report(NullPointerException("Device name is null."))
        }
        deviceName = deviceName ?: getString(R.string.default_device_name)

        root.setup_header_text.text = Phrase.from(root, R.string.congrats_your_device_is_set_up_and_added_to_the_network)
                .put("device_name", deviceName)
                .format()

        root.textView11.text = Phrase.from(root, R.string.p_joinersetupfinished_continue_to_mesh_header)
                .put("device_name", deviceName)
                .format()


        return root
    }

    private fun endSetup() {
        requireActivity().finish()
    }

    private fun startNewFlow() {
        flowManagerVM.flowManager?.startMeshFlowForGateway()
    }

}
