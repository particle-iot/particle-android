package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.inflateFragment
import kotlinx.android.synthetic.main.fragment_control_panel_wifi_options.*


class ControlPanelWifiOptionsFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_common_wifi,
        showBackButton = true,
        showCloseButton = false
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return container?.inflateFragment(R.layout.fragment_control_panel_wifi_options)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        p_controlpanel_wifi_join_new_network_frame.setOnClickListener { joinNewWifiClicked() }
        p_controlpanel_wifi_inspect_current_network_frame.setOnClickListener {
            inspectCurrentNetworkClicked()
        }
        p_controlpanel_wifi_manage_wifi_frame.setOnClickListener { manageWifiClicked() }
    }

    private fun joinNewWifiClicked() {
        flowScopes.onMain { startFlowWithBarcode(flowRunner::startControlPanelWifiConfigFlow) }
    }

    private fun inspectCurrentNetworkClicked() {
        flowScopes.onMain {
            startFlowWithBarcode(flowRunner::startControlPanelInspectCurrentWifiNetworkFlow)
        }
    }

    private fun manageWifiClicked() {
        flowScopes.onMain {
            startFlowWithBarcode(flowRunner::startControlPanelManageWifiNetworksFlow)
        }
    }
}
