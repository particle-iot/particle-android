package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import io.particle.android.sdk.cloud.models.ParticleApiSimStatus
import io.particle.android.sdk.cloud.models.ParticleApiSimStatus.ACTIVE
import io.particle.android.sdk.cloud.models.ParticleApiSimStatus.INACTIVE_DATA_LIMIT_REACHED
import io.particle.android.sdk.cloud.models.ParticleApiSimStatus.INACTIVE_INVALID_PAYMENT_METHOD
import io.particle.android.sdk.cloud.models.ParticleApiSimStatus.INACTIVE_NEVER_ACTIVATED
import io.particle.android.sdk.cloud.models.ParticleApiSimStatus.INACTIVE_USER_DEACTIVATED
import io.particle.android.sdk.utils.pass
import io.particle.mesh.common.QATool
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.inflateFragment
import kotlinx.android.synthetic.main.fragment_controlpanel_cellular_options.*
import mu.KotlinLogging


class ControlPanelCellularOptionsFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_cp_cellular,
        showBackButton = true
    )

    private val log = KotlinLogging.logger {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return container?.inflateFragment(R.layout.fragment_controlpanel_cellular_options)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        p_controlpanel_cellular_options_change_data_limit.setOnClickListener {
            flowRunner.startSetNewDataLimitFlow(device)
        }

        p_controlpanel_cellular_options_change_sim_status.setOnClickListener {
            onChangeSimStatusClicked()
        }
    }

    override fun onResume() {
        super.onResume()
        val sim = flowUiListener?.targetDevice?.sim
        log.info { "onResume() SIM=$sim" }
        if (sim == null) {
            val msg = "device=${flowUiListener?.targetDevice}, sim=$sim"
            QATool.report(IllegalStateException("Missing information from cellular device: $msg"))
        }
        val limit = sim?.monthlyDataRateLimitInMBs
        p_controlpanel_cellular_options_change_data_limit_value.text = "${limit}MB"
        sim?.let { onSimStatusUpdated(it.simStatus) }
    }


    private fun onSimStatusUpdated(status: ParticleApiSimStatus) {
        val config = when (status) {
            ACTIVE -> SimStatusSwitchConfig.ACTIVE
            INACTIVE_USER_DEACTIVATED -> SimStatusSwitchConfig.INACTIVE
            INACTIVE_DATA_LIMIT_REACHED -> SimStatusSwitchConfig.PAUSED
            INACTIVE_INVALID_PAYMENT_METHOD -> SimStatusSwitchConfig.INACTIVE
            INACTIVE_NEVER_ACTIVATED -> throw IllegalArgumentException(
                "SIMs shown in this screen should always have been activated"
            )
        }

        p_controlpanel_cellular_options_current_sim_status.setText(config.statusString)
        p_controlpanel_cellular_options_fine_print.setText(config.finePrint)
        p_controlpanel_cellular_options_change_sim_status.isChecked = config.isSwitchChecked
    }
    
    private fun onChangeSimStatusClicked() {
        when (flowUiListener!!.targetDevice.sim!!.simStatus) {
            ACTIVE -> flowRunner.startSimDeactivateFlow(device)
            INACTIVE_USER_DEACTIVATED -> flowRunner.startSimReactivateFlow(device)
            INACTIVE_DATA_LIMIT_REACHED -> flowRunner.startSimUnpauseFlow(device)
            // FIXME: find out what we want here
            INACTIVE_INVALID_PAYMENT_METHOD -> showInvalidPaymentDialog()
            INACTIVE_NEVER_ACTIVATED -> throw IllegalArgumentException(
                "SIMs shown in this screen should always have been activated"
            )
        }
    }

    private fun showInvalidPaymentDialog() {
        val aktivity = activity ?: return

        p_controlpanel_cellular_options_change_sim_status.isChecked = false

        AlertDialog.Builder(aktivity, R.style.Theme_MaterialComponents_Light_Dialog_Alert)
            .setTitle("Invalid payment method")
            .setMessage("To continue, please update your payment method at https://console.particle.io/billing/edit-card")
            .setPositiveButton("OK") { _, _ -> pass } /* just close. */
            .show()
    }
}


private enum class SimStatusSwitchConfig(
    @StringRes val statusString: Int,
    @StringRes val finePrint: Int,
    val isSwitchChecked: Boolean
) {
    
    ACTIVE(
        R.string.p_cp_cellular_options_sim_status_active,
        R.string.p_cp_cellular_options_sim_status_fine_print_active,
        true
    ),

    INACTIVE(
        R.string.p_cp_cellular_options_sim_status_deactivated,
        R.string.p_cp_cellular_options_sim_status_fine_print_deactivated,
        false
    ),
    
    PAUSED(
        R.string.p_cp_cellular_options_sim_status_paused,
        R.string.p_cp_cellular_options_sim_status_fine_print_paused,
        false
    )
}