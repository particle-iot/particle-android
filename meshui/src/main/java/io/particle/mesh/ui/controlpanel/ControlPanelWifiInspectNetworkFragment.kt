package io.particle.mesh.ui.controlpanel


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.navArgs
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.GetCurrentNetworkReply
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.inflateFragment
import kotlinx.android.synthetic.main.fragment_control_panel_wifi_inspect_network.*


class ControlPanelWifiInspectNetworkFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_common_wifi,
        showBackButton = true
    )

    private val args: ControlPanelWifiInspectNetworkFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return container?.inflateFragment(R.layout.fragment_control_panel_wifi_inspect_network)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)
        val currentNetwork = args.currentNetwork
        if (currentNetwork == null) {
            onNoWifiNetwork()
        } else {
            onWifiNetworkPresent(currentNetwork)
        }

        p_controlpanel_wifi_join_new_network_frame.setOnClickListener {
            flowScopes.onMain { startFlowWithBarcode(flowRunner::startControlPanelWifiConfigFlow) }
        }

        p_controlpanel_wifi_manage_wifi_frame.setOnClickListener {
            flowScopes.onMain {
                startFlowWithBarcode(flowRunner::startControlPanelManageWifiNetworksFlow)
            }
        }
    }

    private fun onWifiNetworkPresent(currentNetwork: GetCurrentNetworkReply) {
        p_controlpanel_wifi_inspect_ssid_value.text = currentNetwork.ssid
        p_controlpanel_wifi_inspect_rssi_value.text = currentNetwork.rssi.toString()
        p_controlpanel_wifi_inspect_channel.text = currentNetwork.channel.toString()
    }

    private fun onNoWifiNetwork() {
        p_controlpanel_wifi_inspect_ssid_value.text = "(No network)"
        p_controlpanel_wifi_inspect_rssi.isVisible = false
        p_controlpanel_wifi_inspect_channel_frame.isVisible = false
    }

}
