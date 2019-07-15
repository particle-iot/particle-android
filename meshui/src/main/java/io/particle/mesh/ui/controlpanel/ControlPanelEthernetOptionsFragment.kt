package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.setup.utils.runOnMainThread
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.inflateFragment
import kotlinx.android.synthetic.main.fragment_control_panel_ethernet_options.*
import kotlinx.coroutines.GlobalScope


class ControlPanelEthernetOptionsFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_cp_ethernet,
        showBackButton = true
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return container?.inflateFragment(R.layout.fragment_control_panel_ethernet_options)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)
        p_controlpanel_ethernet_options_toggle_pins_switch.setOnClickListener {

        }
    }

    override fun onResume() {
        super.onResume()
        flowScopes.onWorker { checkEthernetPinStatus() }
    }

    private suspend fun checkEthernetPinStatus() {

        flowScopes.onMain { onEthernetPinStatusUpdated(true) }
        TODO("not implemented")
    }


    private fun onEthernetPinStatusUpdated(pinsEnabled: Boolean) {
        p_controlpanel_ethernet_options_toggle_pins_switch.isChecked = pinsEnabled
    }

}
