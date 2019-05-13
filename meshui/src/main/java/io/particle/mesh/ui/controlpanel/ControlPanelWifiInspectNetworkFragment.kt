package io.particle.mesh.ui.controlpanel


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.navArgs
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.inflateFragment
import kotlinx.android.synthetic.main.fragment_control_panel_wifi_inspect_network.*


class ControlPanelWifiInspectNetworkFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_controlpanel_wifi_inspect_title,
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

        p_controlpanel_wifi_inspect_ssid_value.text = args.currentNetwork.ssid
        p_controlpanel_wifi_inspect_channel.text = args.currentNetwork.channel.toString()
    }

}
