package io.particle.mesh.ui.controlpanel

import android.app.Application
import androidx.lifecycle.LiveData
import io.particle.mesh.setup.flow.DialogTool
import io.particle.mesh.setup.flow.NavigationTool
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.ui.ProgressHack
import io.particle.mesh.ui.BaseFlowUiDelegate
import io.particle.mesh.ui.R
import mu.KotlinLogging


class ControlPanelFlowUiDelegate(
    navControllerLD: LiveData<NavigationTool?>,
    app: Application,
    dialogTool: DialogTool,
    progressHack: ProgressHack,
    scopes: Scopes = Scopes()
) : BaseFlowUiDelegate(navControllerLD, app, dialogTool,  progressHack, scopes) {

    override fun showGetReadyForSetupScreen() {
        navigate(R.id.action_global_controlPanelPrepareForPairingFragment)
    }

    override fun showTargetPairingProgressUi() {
        navigate(R.id.action_global_controlPanelPrepareForPairingFragment)
    }

    override fun showConnectingToDeviceCloudWiFiUi() {
        navigate(R.id.action_global_argonConnectingStatusFragment)
//        navigate(R.id.action_global_connectingToDeviceCloudFragment)??
    }

    override fun showScanForWifiNetworksUi() {
//        navigate(R.id.action_global_controlPanelScanForWifiNetworksFragment)
        navigate(R.id.action_global_scanForWiFiNetworksFragment)
    }

    override fun showSetWifiPasswordUi() {
        navigate(R.id.action_global_controlPanelEnterWifiNetworkPasswordFragment)
    }

    override fun showSingleTaskCongratsScreen(singleTaskCongratsMessage: String) {
        navigate(R.id.action_global_controlPanelCongratsFragment)
    }

}
