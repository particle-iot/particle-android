package io.particle.mesh.ui.controlpanel

import android.app.Application
import androidx.lifecycle.LiveData
import io.particle.mesh.setup.flow.DialogTool
import io.particle.mesh.setup.flow.MeshFlowTerminator
import io.particle.mesh.setup.flow.NavigationTool
import io.particle.mesh.setup.flow.PostCongratsAction.RESET_TO_START
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
    terminator: MeshFlowTerminator,
    scopes: Scopes = Scopes()
) : BaseFlowUiDelegate(navControllerLD, app, dialogTool, progressHack, scopes, terminator) {

    override val rootDestinationId: Int = R.id.controlPanelLandingFragment

    override fun getDeviceBarcode() {
        progressHack.showGlobalProgressSpinner(true)
    }

    override fun showTargetPairingProgressUi() {
        navigate(
            R.id.action_global_controlPanelPrepareForPairingFragment,
            shouldPopBackstack = false
        )
    }

    override fun showScanForWifiNetworksUi() {
        navigate(R.id.action_global_scanForWiFiNetworksFragment, shouldPopBackstack = false)
    }

    override fun showSetWifiPasswordUi() {
        navigate(R.id.action_global_controlPanelEnterWifiNetworkPasswordFragment)
    }

    override fun showJoinerSetupFinishedUi() {
        showCongratsScreen("Mesh network joined!", RESET_TO_START)
    }

    override fun getNetworkSetupType() {
        navigate(R.id.action_global_scanForMeshNetworksFragment)
    }

}
