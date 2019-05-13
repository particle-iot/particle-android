package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.navigateOnClick
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.inflateFragment
import kotlinx.android.synthetic.main.fragment_controlpanel_mesh_network_options.*


class ControlPanelMeshOptionsFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_common_mesh,
        showBackButton = true
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return container?.inflateFragment(R.layout.fragment_controlpanel_mesh_network_options)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        p_controlpanel_mesh_inspect_current_network_frame.setOnClickListener {
            inspectNetworkClicked()
        }
    }

    private fun inspectNetworkClicked() {
        flowScopes.onMain {
            startFlowWithBarcode(flowRunner::startControlPanelMeshInspectCurrentNetworkFlow)
        }
    }

}