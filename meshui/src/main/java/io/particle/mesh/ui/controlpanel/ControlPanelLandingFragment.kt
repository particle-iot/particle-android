package io.particle.mesh.ui.controlpanel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BLUZ
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BORON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.CORE
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.DIGISTUMP_OAK
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ELECTRON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.OTHER
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.P1
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.PHOTON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.RASPBERRY_PI
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.RED_BEAR_DUO
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.XENON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.X_SOM
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.inflateFragment
import io.particle.mesh.ui.navigateOnClick
import kotlinx.android.synthetic.main.fragment_control_panel_landing.*
import kotlinx.coroutines.delay


class ControlPanelLandingFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(R.string.p_controlpanel_control_panel)

    private lateinit var cloud: ParticleCloud


    private val flowManagementScope = Scopes()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cloud = ParticleCloudSDK.getCloud()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return container?.inflateFragment(R.layout.fragment_control_panel_landing)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deviceType = device.deviceType!!

        p_controlpanel_landing_ethernet_item_frame.isVisible = false // this is just off for now
        p_controlpanel_landing_wifi_item_frame.isVisible = deviceType in listOf(ARGON, A_SOM)
        p_controlpanel_landing_cellular_item_frame.isVisible = deviceType in listOf(BORON, B_SOM)

        p_controlpanel_landing_wifi_item.navigateOnClick(
            R.id.action_controlPanelLandingFragment_to_controlPanelWifiOptionsFragment
        )

        p_controlpanel_landing_cellular_item.setOnClickListener {
            flowRunner.startShowControlPanelCellularOptionsFlow(device)
        }

        p_controlpanel_landing_mesh_item.navigateOnClick(
            R.id.action_global_controlPanelMeshOptionsFragment
        )

        p_controlpanel_landing_docs_item.setOnClickListener {
            showDocumentation(activity!!, device.deviceType!!)
        }

        p_controlpanel_landing_unclaim_item.setOnClickListener {
            navigateToUnclaim()
        }

    }

    override fun onResume() {
        super.onResume()
        flowManagementScope.onMain {
            // FIXME: hackish, try to remove
            delay(500)
            if (isResumed) {
                flowRunner.endCurrentFlow()  // end any current flows
            }
        }
    }

    private fun navigateToUnclaim() {
        flowSystemInterface.showGlobalProgressSpinner(true)
        findNavController().navigate(
            R.id.action_global_controlPanelUnclaimDeviceFragment,
            ControlPanelUnclaimDeviceFragmentArgs(device.name).toBundle()
        )
        flowSystemInterface.showGlobalProgressSpinner(false)
    }
}


private fun showDocumentation(context: Context, deviceType: ParticleDeviceType) {
    val finalPathSegment = when (deviceType) {
        CORE -> "core"
        PHOTON -> "photon"
        P1 -> "datasheets/wi-fi/p1-datasheet"
        ELECTRON -> "electron"
        ARGON, A_SOM -> "argon"
        BORON, B_SOM -> "boron"
        XENON, X_SOM -> "xenon"
        RASPBERRY_PI,
        RED_BEAR_DUO,
        BLUZ,
        DIGISTUMP_OAK,
        OTHER -> null
    }

    finalPathSegment?.let {
        val uri = Uri.parse("https://docs.particle.io/$finalPathSegment")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
