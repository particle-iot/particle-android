package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.utils.runOnMainThread
import io.particle.mesh.setup.utils.safeToast
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.inflateFragment
import kotlinx.android.synthetic.main.fragment_control_panel_ethernet_options.*
import kotlinx.coroutines.GlobalScope
import mu.KotlinLogging


class ControlPanelEthernetOptionsFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_cp_ethernet,
        showBackButton = true
    )

    private val log = KotlinLogging.logger {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return container?.inflateFragment(R.layout.fragment_control_panel_ethernet_options)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)
        val checked = flowUiListener.deviceData.isEthernetEnabled
        p_controlpanel_ethernet_options_toggle_pins_switch.isChecked = checked
        p_controlpanel_ethernet_options_toggle_pins_switch.setOnClickListener {
            toggleEthernetPins(p_controlpanel_ethernet_options_toggle_pins_switch.isChecked)
        }

        val statusText = if (flowUiListener.deviceData.isEthernetEnabled) "Active" else "Inactive"
        p_controlpanel_ethernet_options_current_pins_status.text = statusText
    }

    private fun toggleEthernetPins(shouldEnable: Boolean) {
        Scopes().onMain {
            startFlowWithBarcode { device, barcode ->
                flowRunner.startControlPanelToggleEthernetPinsFlow(device, barcode, shouldEnable)
            }
        }
    }
}
