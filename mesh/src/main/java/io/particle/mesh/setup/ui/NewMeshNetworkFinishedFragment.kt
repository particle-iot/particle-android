package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.squareup.phrase.Phrase
import io.particle.mesh.common.QATool
import io.particle.mesh.R
import kotlinx.android.synthetic.main.fragment_new_mesh_network_finished.view.*
import java.lang.NullPointerException


class NewMeshNetworkFinishedFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_new_mesh_network_finished, container, false)

        root.action_add_next_mesh_device.setOnClickListener {
            flowManagerVM.flowManager?.startNewFlowWithCommissioner()
        }
        root.action_start_building.setOnClickListener { endSetup() }

        return root
    }

    private fun endSetup() {
        findNavController().navigate(R.id.action_global_letsGetBuildingFragment)
    }

}
