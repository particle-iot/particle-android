package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.ui.navigateOnClick
import io.particle.mesh.ui.R
import kotlinx.android.synthetic.main.fragment_controlpanel_mesh_network_options.*


class ControlPanelMeshOptionsFragment : BaseControlPanelFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_controlpanel_mesh_network_options, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        p_controlpanel_mesh_inspect_current_network.navigateOnClick(
            R.id.action_global_controlPanelMeshInspectNetworkFragment
        )
    }

}