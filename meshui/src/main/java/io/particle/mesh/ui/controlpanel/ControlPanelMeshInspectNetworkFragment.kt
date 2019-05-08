package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import kotlinx.android.synthetic.main.fragment_controlpanel_mesh_network_info.*


class ControlPanelMeshInspectNetworkFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_controlpanel_mesh_inspect_title,
        showBackButton = true
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_controlpanel_mesh_network_info, container, false)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)
        onNetworkInfoUpdated(flowUiListener.mesh.currentlyJoinedNetwork!!)
    }

    private fun onNetworkInfoUpdated(networkInfo: Mesh.NetworkInfo) {
        p_controlpanel_mesh_inspect_network_name.text = networkInfo.name
        p_controlpanel_mesh_inspect_network_pan_id.text = networkInfo.panId.toString()
        p_controlpanel_mesh_inspect_network_xpan_id.text = networkInfo.extPanId.toString()
        p_controlpanel_mesh_inspect_network_channel.text = networkInfo.channel.toString()

        p_controlpanel_mesh_inspect_network_network_id_frame.setOnClickListener {
            flowSystemInterface.navControllerLD.value?.navigate(
                R.id.action_global_controlPanelNetworkIdFragment,
                ControlPanelNetworkIdFragmentArgs(networkInfo.networkId).toBundle()
            )
        }
    }

}