package io.particle.android.sdk.controlpanel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import io.particle.mesh.setup.flow.DialogTool
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.modules.BaseFlowUiDelegate
import io.particle.mesh.setup.ui.ProgressHack
import io.particle.sdk.app.R
import mu.KotlinLogging


class ControlPanelFlowUiDelegate(
    navControllerLD: LiveData<NavController?>,
    app: Application,
    dialogTool: DialogTool,
    progressHack: ProgressHack,
    scopes: Scopes = Scopes()
) : BaseFlowUiDelegate(navControllerLD, app, dialogTool,  progressHack, scopes) {

    private val log = KotlinLogging.logger {}

    override fun getDeviceBarcode() {
        navigate(R.id.action_global_scanJoinerCodeIntroFragment)
    }

    override fun getNetworkSetupType() {
        navigate(R.id.action_global_useStandaloneOrInMeshFragment)
    }

    override fun showGetReadyForSetupScreen() {
        navigate(R.id.action_global_controlPanelPrepareForParingFragment)
    }

    override fun showTargetPairingProgressUi() {
        navigate(R.id.action_global_controlPanelPrepareForParingFragment)
//        navigate(R.id.action_global_controlPanelBLEPairingProgressFragment)
    }

    override fun showPricingImpactScreen() {
        navigate(R.id.action_global_pricingImpactFragment)
    }

    override fun showConnectingToDeviceCloudUi() {
        navigate(R.id.action_global_connectingToDeviceCloudFragment)
    }

    override fun showScanForWifiNetworksUi() {
        navigate(R.id.action_global_controlPanelScanForWifiNetworksFragment)
    }

    override fun showNameDeviceUi() {
        navigate(R.id.action_global_nameYourDeviceFragment)
    }

    override fun showBleOtaIntroUi() {
        navigate(R.id.action_global_bleOtaIntroFragment)
    }

    override fun showBleOtaUi() {
        navigate(R.id.action_global_bleOtaFragment)
    }

    override fun showSetWifiPasswordUi() {
        navigate(R.id.action_global_controlPanelEnterWifiNetworkPasswordFragment)
    }

    override fun showSingleTaskCongratsScreen(singleTaskCongratsMessage: String) {
        navigate(R.id.action_global_controlPanelCongratsFragment)
    }

    override fun getNewMeshNetworkName() {
        navigate(R.id.action_global_newMeshNetworkNameFragment)
    }

    override fun getNewMeshNetworkPassword() {
        navigate(R.id.action_global_newMeshNetworkPasswordFragment)
    }

    override fun showCreatingMeshNetworkUi() {
        navigate(R.id.action_global_creatingMeshNetworkFragment)
    }

    override fun showInternetConnectedConnectToDeviceCloudIntroUi() {
        navigate(R.id.action_global_argonConnectToDeviceCloudIntroFragment)
    }

    override fun showConnectingToDeviceCloudWiFiUi() {
        navigate(R.id.action_global_argonConnectingStatusFragment)
    }

    override fun showConnectingToDeviceCloudCellularUi() {
        navigate(R.id.action_global_boronConnectingStatusFragment)
    }

    override fun getMeshNetworkToJoin() {
        navigate(R.id.action_global_scanForMeshNetworksFragment)
    }

    override fun getCommissionerBarcode() {
        navigate(R.id.action_global_manualCommissioningAddToNetworkFragment)
    }

    override fun showComissionerPairingProgressUi() {
        navigate(R.id.action_global_assistingDevicePairingProgressFragment)
    }

    override fun collectPasswordForMeshToJoin() {
        navigate(R.id.action_global_enterNetworkPasswordFragment)
    }

    override fun showJoiningMeshNetworkUi() {
        navigate(R.id.action_global_joiningMeshNetworkProgressFragment)
    }

    override fun showJoinerSetupFinishedUi() {
        navigate(R.id.action_global_setupFinishedFragment)
    }

    override fun showCreateNetworkFinishedUi() {
        navigate(R.id.action_global_newMeshNetworkFinishedFragment)
    }

    override fun showGatewaySetupFinishedUi() {
        navigate(R.id.action_global_gatewaySetupFinishedFragment)
    }

}
