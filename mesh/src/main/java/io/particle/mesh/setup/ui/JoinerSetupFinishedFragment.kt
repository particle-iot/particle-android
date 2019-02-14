package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.particle.mesh.R
import kotlinx.android.synthetic.main.fragment_joiner_setup_finished.view.*


class JoinerSetupFinishedFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_joiner_setup_finished, container, false)

        root.action_start_building.setOnClickListener{ endSetup() }
        root.action_add_next_mesh_device.setOnClickListener{ startNewFlow() }

        return root
    }

    private fun endSetup() {
        findNavController().navigate(R.id.action_global_letsGetBuildingFragment)
    }

    private fun startNewFlow() {
        flowManagerVM.flowManager?.startNewFlowWithCommissioner()
    }
}
