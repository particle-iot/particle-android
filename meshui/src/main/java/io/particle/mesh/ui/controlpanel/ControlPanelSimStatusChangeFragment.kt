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
import io.particle.mesh.ui.inflateFragment
import io.particle.mesh.ui.setBackgroundTint
import kotlinx.android.synthetic.main.fragment_control_panel_sim_status_change.*
import mu.KotlinLogging


class ControlPanelSimStatusChangeFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_controlpanel_simstatuschange_title,
        showBackButton = true
    )

    private val args: ControlPanelSimStatusChangeFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return container?.inflateFragment(R.layout.fragment_control_panel_sim_status_change)
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
        simstatus_big_icon.setImageResource(cfg.bigIcon)
        simstatus_header.text = getString(cfg.headerText)
        simstatus_fine_print.text = getString(cfg.finePrintText)

        action_change_sim_status.text = getString(cfg.actionButtonText)
        cfg.actionButtonColor?.let { action_change_sim_status.setBackgroundTint(it) }
        action_change_sim_status.isEnabled = cfg.actionButtonInitiallyEnabled
        if (!cfg.actionButtonInitiallyEnabled) {
            flowUiListener!!.cellular.newSelectedDataLimitLD.observe(this, Observer {
                it?.let { action_change_sim_status.isEnabled = true }
            })
        }

        simstatus_body.text = Phrase.from(simstatus_body, cfg.bodyText)
            .putOptional(TEMPLATE_KEY_LAST_4_DIGITS, device.iccid?.takeLast(4))
            .format()

        simstatus_data_limit_control.isVisible = cfg.showDataLimitRow
        if (cfg.showDataLimitRow) {

            val limitFromApi = flowUiListener?.targetDevice?.sim?.monthlyDataRateLimitInMBs
            val userSelectedLimit = flowUiListener?.cellular?.newSelectedDataLimitLD?.value
            val limit = userSelectedLimit ?: limitFromApi

            p_controlpanel_data_limit_value.text = "$limit MB"

            simstatus_data_limit_control.setOnClickListener {
                findNavController().navigate(
                    R.id.action_global_controlPanelCellularDataLimitFragment
                )
            }
        }

        action_change_sim_status.setOnClickListener {
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