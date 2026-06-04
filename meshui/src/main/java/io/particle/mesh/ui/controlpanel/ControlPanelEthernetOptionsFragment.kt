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
import io.particle.mesh.ui.databinding.FragmentControlPanelEthernetOptionsBinding
import kotlinx.coroutines.GlobalScope
import mu.KotlinLogging


class ControlPanelEthernetOptionsFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_cp_ethernet,
        showBackButton = true
    )

    private val log = KotlinLogging.logger {}

    private var _binding: FragmentControlPanelEthernetOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentControlPanelEthernetOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)
        val checked = flowUiListener.deviceData.isEthernetEnabled
        binding.pControlpanelEthernetOptionsTogglePinsSwitch.isChecked = checked
        binding.pControlpanelEthernetOptionsTogglePinsSwitch.setOnClickListener {
            toggleEthernetPins(binding.pControlpanelEthernetOptionsTogglePinsSwitch.isChecked)
        }

        val statusText = if (flowUiListener.deviceData.isEthernetEnabled) "Active" else "Inactive"
        binding.pControlpanelEthernetOptionsCurrentPinsStatus.text = statusText
    }

    private fun toggleEthernetPins(shouldEnable: Boolean) {
        Scopes().onMain {
            startFlowWithBarcode { device, barcode ->
                flowRunner.startControlPanelToggleEthernetPinsFlow(device, barcode, shouldEnable)
            }
        }
    }
}
