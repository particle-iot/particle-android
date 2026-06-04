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
import io.particle.mesh.ui.databinding.FragmentControlPanelNetworkIdBinding
import io.particle.mesh.ui.inflateFragment


class ControlPanelNetworkIdFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.controlpanel_network_id_title,
        showBackButton = true,
        showCloseButton = false
    )

    private val args: ControlPanelNetworkIdFragmentArgs by navArgs()

    private var _binding: FragmentControlPanelNetworkIdBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = container?.inflateFragment(R.layout.fragment_control_panel_network_id)
        _binding = root?.let { FragmentControlPanelNetworkIdBinding.bind(it) }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        binding.networkIdValue.text = args.networkId
    }

}
