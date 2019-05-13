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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.squareup.phrase.Phrase
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.controlpanel.SimStatusMode.DEACTIVATE
import io.particle.mesh.ui.controlpanel.SimStatusMode.REACTIVATE
import io.particle.mesh.ui.controlpanel.SimStatusMode.UNPAUSE
import io.particle.mesh.ui.inflateFragment
import io.particle.mesh.ui.setBackgroundTint
import kotlinx.android.synthetic.main.fragment_control_panel_sim_status_change.*


enum class SimStatusMode {
    DEACTIVATE,
    UNPAUSE,
    REACTIVATE
}


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

        simstatus_body.text = Phrase.from(simstatus_body, cfg.bodyText)
            // FIXME: insert actual value
            .putOptional(TEMPLATE_KEY_LAST_4_DIGITS, "1234")
            .format()

        simstatus_data_limit_control.isVisible = cfg.showDataLimitRow
        if (cfg.showDataLimitRow) {
            // FIXME: FILL IN CURRENT LIMIT
            simstatus_data_limit_control.setOnClickListener {
                findNavController().navigate(
                    R.id.action_global_controlPanelCellularDataLimitFragment
                )
            }
        }

        simstatus_data_rates.isVisible = cfg.showDataRates
        if (cfg.showDataRates) {
            // FIXME: FILL IN MONTHLY DATA RATES
        }

        action_change_sim_status.setOnClickListener {
            when (args.simStatusChangeMode) {
                DEACTIVATE -> onSimDeactivateClicked()
                UNPAUSE -> onSimUnpauseClicked()
                REACTIVATE -> onSimReactivateClicked()
            }
        }
    }

    private fun onSimReactivateClicked() {
        TODO("not implemented")
    }

    private fun onSimUnpauseClicked() {
        TODO("not implemented")
    }

    private fun onSimDeactivateClicked() {
        TODO("not implemented")
    }

}


// FIXME: use correct icons here!

private enum class SimStatusConfig(
    @DrawableRes val bigIcon: Int,
    @StringRes val headerText: Int,
    @StringRes val bodyText: Int,
    @StringRes val finePrintText: Int,
    @StringRes val actionButtonText: Int,
    @ColorRes val actionButtonColor: Int? = null,
    val showDataRates: Boolean = false,
    val showDataLimitRow: Boolean = false
) {
    DEACTIVATE(
        R.drawable.p_particle_logo,
        R.string.p_controlpanel_deactivate_sim_header,
        R.string.p_controlpanel_sim_deactivate_body_text,
        R.string.p_controlpanel_sim_deactivation_fine_print,
        R.string.p_action_deactivate_sim,
        actionButtonColor = R.color.p_action_button_red
    ),

    UNPAUSE(
        R.drawable.p_particle_logo,
        R.string.p_controlpanel_unpause_sim_header,
        R.string.p_controlpanel_sim_unpause_body_text,
        R.string.p_controlpanel_sim_unpause_fine_print,
        R.string.p_action_unpause_sim,
        showDataLimitRow = true
    ),

    REACTIVATE(
        R.drawable.p_particle_logo,
        R.string.p_controlpanel_sim_reactivate_header,
        R.string.p_controlpanel_sim_reactivate_body_text,
        R.string.p_controlpanel_sim_reactivate_fine_print,
        R.string.p_action_reactivate_sim,
        showDataRates = true
    )

}

private const val TEMPLATE_KEY_LAST_4_DIGITS = "sim_last_four_digits"