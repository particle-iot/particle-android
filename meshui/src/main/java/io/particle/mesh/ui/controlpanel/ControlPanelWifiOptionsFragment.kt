package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.databinding.FragmentControlPanelWifiOptionsBinding
import io.particle.mesh.ui.inflateFragment


class ControlPanelWifiOptionsFragment : BaseControlPanelFragment() {

    private var _binding: FragmentControlPanelWifiOptionsBinding? = null
    private val binding get() = _binding!!

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
        val root = container?.inflateFragment(R.layout.fragment_control_panel_wifi_options)
        _binding = root?.let { FragmentControlPanelWifiOptionsBinding.bind(it) }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.pControlpanelWifiJoinNewNetworkFrame.setOnClickListener { joinNewWifiClicked() }
        binding.pControlpanelWifiInspectCurrentNetworkFrame.setOnClickListener {
            inspectCurrentNetworkClicked()
        }
        binding.pControlpanelWifiManageWifiFrame.setOnClickListener { manageWifiClicked() }
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
