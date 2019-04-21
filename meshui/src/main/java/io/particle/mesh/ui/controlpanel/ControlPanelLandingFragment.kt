package io.particle.mesh.ui.controlpanel

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.WorkerThread
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.mesh.setup.utils.safeToast
import io.particle.mesh.ui.navigateOnClick
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import kotlinx.android.synthetic.main.fragment_control_panel_landing.*
import java.lang.Exception


class ControlPanelLandingFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(R.string.p_controlpanel_control_panel)

    private lateinit var cloud: ParticleCloud


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cloud = ParticleCloudSDK.getCloud()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_control_panel_landing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        p_controlpanel_landing_wifi_item.navigateOnClick(
            R.id.action_controlPanelLandingFragment_to_controlPanelWifiOptionsFragment
        )

        p_controlpanel_landing_cellular_item.navigateOnClick(
            R.id.action_global_controlPanelCellularOptionsFragment
        )

        p_controlpanel_landing_mesh_item.navigateOnClick(
            R.id.action_global_controlPanelMeshOptionsFragment
        )

        p_controlpanel_landing_docs_item.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://html5zombo.com")))
        }

        p_controlpanel_landing_unclaim_item.setOnClickListener {
            flowScopes.onWorker { unclaimDevice() }
        }

    }

    @WorkerThread
    private fun unclaimDevice() {
        flowSystemInterface.showGlobalProgressSpinner(true)

        val error = try {
            val device = cloud.getDevice(deviceId)
            device.unclaim()
            null

        } catch (ex: Exception) {
            ex

        } finally {
            flowScopes.onMain {
                flowSystemInterface.showGlobalProgressSpinner(false)
            }
        }

        flowScopes.onMain {
            if (!isAdded) {
                return@onMain
            }

            if (error == null) {
                activity?.safeToast("Device unclaimed!")
                activity?.finish()
            } else {
                activity?.safeToast("Error unclaiming device")
            }
        }
    }

}
