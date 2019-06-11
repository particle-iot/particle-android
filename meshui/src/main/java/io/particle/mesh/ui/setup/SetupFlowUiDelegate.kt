package io.particle.mesh.ui.setup

import android.app.Application
import androidx.lifecycle.LiveData
import io.particle.mesh.setup.flow.DialogTool
import io.particle.mesh.setup.flow.MeshFlowTerminator
import io.particle.mesh.setup.flow.NavigationTool
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.ui.ProgressHack
import io.particle.mesh.ui.BaseFlowUiDelegate
import io.particle.mesh.ui.R


class SetupFlowUiDelegate(
    navControllerLD: LiveData<NavigationTool?>,
    everythingNeedsAContext: Application,
    dialogTool: DialogTool,
    progressHack: ProgressHack,
    terminator: MeshFlowTerminator,
    scopes: Scopes = Scopes()
) : BaseFlowUiDelegate(
    navControllerLD,
    everythingNeedsAContext,
    dialogTool,
    progressHack,
    scopes,
    terminator
) {

    override fun showGetReadyForSetupScreen() {
        navigate(R.id.action_global_getReadyForSetupFragment)
    }

    override fun showTargetPairingProgressUi() {
        navigate(R.id.action_global_BLEPairingProgressFragment)
    }

    override fun showScanForWifiNetworksUi() {
        navigate(R.id.action_global_scanForWiFiNetworksFragment)
    }

    override fun showSetWifiPasswordUi() {
        navigate(R.id.action_global_enterWifiNetworkPasswordFragment)
//        navigate(R.id.action_global_controlPanelEnterWifiNetworkPasswordFragment)
    }

}
