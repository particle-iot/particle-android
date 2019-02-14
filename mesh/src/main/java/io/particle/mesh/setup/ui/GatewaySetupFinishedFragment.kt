package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.squareup.phrase.Phrase
import io.particle.mesh.common.QATool
import io.particle.mesh.R
import kotlinx.android.synthetic.main.fragment_gateway_setup_finished.*
import kotlinx.android.synthetic.main.fragment_gateway_setup_finished.view.*


class GatewaySetupFinishedFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_gateway_setup_finished, container, false)

        root.action_start_building.setOnClickListener { endSetup() }
        root.action_add_next_mesh_device.setOnClickListener { startNewFlow() }

        val cloud = flowManagerVM.flowManager!!.cloudConnectionModule

        val hdr = Phrase.from(root, R.string.p_joinersetupfinished_continue_to_mesh_header)
            .put("device_name", cloud.targetDeviceNameToAssignLD.value!!)
            .format()
        root.p_mesh_header.text = hdr

        return root
    }

    private fun endSetup() {
        findNavController().navigate(R.id.action_global_letsGetBuildingFragment)
    }

    private fun startNewFlow() {
        flowManagerVM.flowManager?.startMeshFlowForGateway()
    }

}
