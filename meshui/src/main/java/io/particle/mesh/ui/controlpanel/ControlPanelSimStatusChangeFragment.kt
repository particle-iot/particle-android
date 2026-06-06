package io.particle.mesh.ui.controlpanel


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.squareup.phrase.Phrase
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.setup.flow.SimStatusChangeMode.DEACTIVATE
import io.particle.mesh.setup.flow.SimStatusChangeMode.REACTIVATE
import io.particle.mesh.setup.flow.SimStatusChangeMode.UNPAUSE
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.databinding.FragmentControlPanelSimStatusChangeBinding
import io.particle.mesh.ui.setBackgroundTint
import mu.KotlinLogging


class ControlPanelSimStatusChangeFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_controlpanel_simstatuschange_title,
        showBackButton = true
    )

    private val args: ControlPanelSimStatusChangeFragmentArgs by navArgs()

    private var _binding: FragmentControlPanelSimStatusChangeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentControlPanelSimStatusChangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        initViewFromConfig(
            when (args.simStatusChangeMode) {
                DEACTIVATE -> SimStatusConfig.DEACTIVATE
                UNPAUSE -> SimStatusConfig.UNPAUSE
                REACTIVATE -> SimStatusConfig.REACTIVATE
            }
        )
    }

    private fun initViewFromConfig(cfg: SimStatusConfig) {
        binding.simstatusBigIcon.setImageResource(cfg.bigIcon)
        binding.simstatusHeader.text = getString(cfg.headerText)
        binding.simstatusFinePrint.text = getString(cfg.finePrintText)

        binding.actionChangeSimStatus.text = getString(cfg.actionButtonText)
        cfg.actionButtonColor?.let { binding.actionChangeSimStatus.setBackgroundTint(it) }
        binding.actionChangeSimStatus.isEnabled = cfg.actionButtonInitiallyEnabled
        if (!cfg.actionButtonInitiallyEnabled) {
            flowUiListener!!.cellular.newSelectedDataLimitLD.observe(this, Observer {
                it?.let { binding.actionChangeSimStatus.isEnabled = true }
            })
        }

        binding.simstatusBody.text = Phrase.from(binding.simstatusBody, cfg.bodyText)
            .putOptional(TEMPLATE_KEY_LAST_4_DIGITS, device.iccid?.takeLast(4))
            .format()

        binding.simstatusDataLimitControl.isVisible = cfg.showDataLimitRow
        if (cfg.showDataLimitRow) {

            val limitFromApi = flowUiListener?.targetDevice?.sim?.monthlyDataRateLimitInMBs
            val userSelectedLimit = flowUiListener?.cellular?.newSelectedDataLimitLD?.value
            val limit = userSelectedLimit ?: limitFromApi

            binding.pControlpanelDataLimitValue.text = "$limit MB"

            binding.simstatusDataLimitControl.setOnClickListener {
                findNavController().navigate(
                    R.id.action_global_controlPanelCellularDataLimitFragment
                )
            }
        }

        binding.actionChangeSimStatus.setOnClickListener {
            flowUiListener?.cellular?.updateChangeSimStatusButtonClicked()
        }
    }
}



private enum class SimStatusConfig(
    @DrawableRes val bigIcon: Int,
    @StringRes val headerText: Int,
    @StringRes val bodyText: Int,
    @StringRes val finePrintText: Int,
    @StringRes val actionButtonText: Int,
    @ColorRes val actionButtonColor: Int? = null,
    val showDataLimitRow: Boolean = false,
    val actionButtonInitiallyEnabled: Boolean = true
) {
    DEACTIVATE(
        R.drawable.sim_deactivate_header_image,
        R.string.p_controlpanel_deactivate_sim_header,
        R.string.p_controlpanel_sim_deactivate_body_text,
        R.string.p_controlpanel_sim_deactivation_fine_print,
        R.string.p_action_deactivate_sim,
        actionButtonColor = R.color.p_action_button_red
    ),

    UNPAUSE(
        R.drawable.sim_activate_header_image,
        R.string.p_controlpanel_unpause_sim_header,
        R.string.p_controlpanel_sim_unpause_body_text,
        R.string.p_controlpanel_sim_unpause_fine_print,
        R.string.p_action_unpause_sim,
        showDataLimitRow = true,
        actionButtonInitiallyEnabled = false
    ),

    REACTIVATE(
        R.drawable.sim_activate_header_image,
        R.string.p_controlpanel_sim_reactivate_header,
        R.string.p_controlpanel_sim_reactivate_body_text,
        R.string.p_controlpanel_sim_reactivate_fine_print,
        R.string.p_action_reactivate_sim
    )

}

private const val TEMPLATE_KEY_LAST_4_DIGITS = "sim_last_four_digits"