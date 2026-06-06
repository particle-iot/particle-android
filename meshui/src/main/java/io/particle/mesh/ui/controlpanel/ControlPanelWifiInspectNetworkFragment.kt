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
import io.particle.mesh.ui.databinding.FragmentControlPanelWifiInspectNetworkBinding
import io.particle.mesh.ui.inflateFragment


class ControlPanelWifiInspectNetworkFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_common_wifi,
        showBackButton = true
    )

    private val args: ControlPanelWifiInspectNetworkFragmentArgs by navArgs()

    private var _binding: FragmentControlPanelWifiInspectNetworkBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = container?.inflateFragment(R.layout.fragment_control_panel_wifi_inspect_network)
        _binding = root?.let { FragmentControlPanelWifiInspectNetworkBinding.bind(it) }
        return root
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)
        val currentNetwork = args.currentNetwork
        if (currentNetwork == null) {
            onNoWifiNetwork()
        } else {
            onWifiNetworkPresent(currentNetwork)
        }

        binding.pControlpanelWifiJoinNewNetworkFrame.setOnClickListener {
            flowScopes.onMain { startFlowWithBarcode(flowRunner::startControlPanelWifiConfigFlow) }
        }

        binding.pControlpanelWifiManageWifiFrame.setOnClickListener {
            flowScopes.onMain {
                startFlowWithBarcode(flowRunner::startControlPanelManageWifiNetworksFlow)
            }
        }
    }

    private fun onWifiNetworkPresent(currentNetwork: GetCurrentNetworkReply) {
        binding.pControlpanelWifiInspectSsidValue.text = currentNetwork.ssid
        binding.pControlpanelWifiInspectRssiValue.text = currentNetwork.rssi.toString()
        binding.pControlpanelWifiInspectChannel.text = currentNetwork.channel.toString()
    }

    private fun onNoWifiNetwork() {
        binding.pControlpanelWifiInspectSsidValue.text = "(No network)"
        binding.pControlpanelWifiInspectRssi.isVisible = false
        binding.pControlpanelWifiInspectChannelFrame.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
