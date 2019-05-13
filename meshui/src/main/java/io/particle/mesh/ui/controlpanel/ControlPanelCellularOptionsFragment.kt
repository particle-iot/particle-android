package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.utils.safeToast
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
            activity.safeToast("SIM actions are temporarily out of order: these will be available in the next release")
//            findNavController().navigate(
//                R.id.action_global_controlPanelSimStatusChangeFragment,
//                ControlPanelSimStatusChangeFragmentArgs(SimStatusMode.DEACTIVATE).toBundle()
//            )
        }
    }

    override fun onResume() {
        super.onResume()
        val sim = flowUiListener?.targetDevice?.sim
        val limit = sim?.monthlyDataRateLimitInMBs
        p_controlpanel_cellular_options_change_data_limit_value.text = "${limit}MB"
    }

}