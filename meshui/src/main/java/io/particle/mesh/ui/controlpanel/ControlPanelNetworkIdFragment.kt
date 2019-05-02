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
import kotlinx.android.synthetic.main.fragment_control_panel_network_id.*


class ControlPanelNetworkIdFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.controlpanel_network_id_title,
        showBackButton = true,
        showCloseButton = false
    )

    private val args: ControlPanelNetworkIdFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return container?.inflateFragment(R.layout.fragment_control_panel_network_id)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        network_id_value.text = args.networkId
    }

}
