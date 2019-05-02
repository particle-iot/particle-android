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
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.inflateFragment
import kotlinx.android.synthetic.main.fragment_controlpanel_cellular_options.*


class ControlPanelCellularOptionsFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_cp_cellular,
        showBackButton = true
    )

    private val cloud = ParticleCloudSDK.getCloud()

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
            findNavController().navigate(
                R.id.action_global_controlPanelSimStatusChangeFragment,
                ControlPanelSimStatusChangeFragmentArgs(SimStatusMode.UNPAUSE).toBundle()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        flowSystemInterface.showGlobalProgressSpinner(true)
        flowScopes.onWorker {
            val sim = try {
                cloud.getSim(device.iccid!!)
            } catch (ex: Exception) {
                return@onWorker
            }
            flowScopes.onMain {
                val limit = sim.monthlyDataRateLimitInMBs
                p_controlpanel_cellular_options_change_data_limit_value.text = "${limit}MB"
            }
            flowSystemInterface.showGlobalProgressSpinner(false)
        }
    }

}