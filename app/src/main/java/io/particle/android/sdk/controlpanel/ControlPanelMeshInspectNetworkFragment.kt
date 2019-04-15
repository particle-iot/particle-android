package io.particle.android.sdk.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_controlpanel_mesh_network_info.*


class ControlPanelMeshInspectNetworkFragment : BaseControlPanelFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_controlpanel_mesh_network_info, container, false)
    }

    private fun onNetworkInfoUpdated(networkInfo: Mesh.NetworkInfo) {
        p_controlpanel_mesh_inspect_network_name.text = networkInfo.name
        p_controlpanel_mesh_inspect_network_pan_id.text = networkInfo.panId.toString()
        p_controlpanel_mesh_inspect_network_xpan_id.text = networkInfo.extPanId.toString()
    }

}