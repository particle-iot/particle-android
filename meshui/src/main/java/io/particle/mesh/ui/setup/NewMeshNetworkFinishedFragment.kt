package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import kotlinx.android.synthetic.main.fragment_new_mesh_network_finished.*


class NewMeshNetworkFinishedFragment : BaseFlowFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_mesh_network_finished, container, false)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        action_add_next_mesh_device.setOnClickListener {
            flowRunner.startNewFlowWithCommissioner()
        }
        action_start_building.setOnClickListener { endSetup() }

    }

    private fun endSetup() {
        // FIXME: this shouldn't live in the UI
        findNavController().navigate(R.id.action_global_letsGetBuildingFragment)
    }

}
