package io.particle.mesh.ui.setup

import android.app.Application
import androidx.lifecycle.LiveData
import io.particle.mesh.setup.flow.DialogTool
import io.particle.mesh.setup.flow.NavigationTool
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.ui.ProgressHack
import io.particle.mesh.ui.BaseFlowUiDelegate
import io.particle.mesh.ui.R
import mu.KotlinLogging


class SetupFlowUiDelegate(
    navControllerLD: LiveData<NavigationTool?>,
    everythingNeedsAContext: Application,
    dialogTool: DialogTool,
    progressHack: ProgressHack,
    scopes: Scopes = Scopes()
) : BaseFlowUiDelegate(navControllerLD, everythingNeedsAContext, dialogTool, progressHack, scopes) {

    override fun showGetReadyForSetupScreen() {
        navigate(R.id.action_global_getReadyForSetupFragment)
    }

    override fun showTargetPairingProgressUi() {
        navigate(R.id.action_global_BLEPairingProgressFragment)
    }

    override fun showConnectingToDeviceCloudWiFiUi() {
        navigate(R.id.action_global_connectingToDeviceCloudFragment)
//        navigate(R.id.action_global_argonConnectingStatusFragment)
    }

    override fun showScanForWifiNetworksUi() {
        navigate(R.id.action_global_scanForWiFiNetworksFragment)
    }

    override fun showSingleTaskCongratsScreen(singleTaskCongratsMessage: String) {
        navigate(
            R.id.action_global_hashtagWinningFragment,
            HashtagWinningFragmentArgs(singleTaskCongratsMessage).toBundle()
        )
    }

    override fun showSetWifiPasswordUi() {
        navigate(R.id.action_global_enterWifiNetworkPasswordFragment)
//        navigate(R.id.action_global_controlPanelEnterWifiNetworkPasswordFragment)
    }

}
