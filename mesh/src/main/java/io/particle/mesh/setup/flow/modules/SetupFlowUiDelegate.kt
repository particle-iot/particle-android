package io.particle.mesh.setup.flow.modules

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import io.particle.mesh.R
import io.particle.mesh.setup.flow.DialogTool
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.ui.ProgressHack
import mu.KotlinLogging


class SetupFlowUiDelegate(
    navControllerLD: LiveData<NavController?>,
    everythingNeedsAContext: Application,
    dialogTool: DialogTool,
    progressHack: ProgressHack,
    scopes: Scopes = Scopes()
) : BaseFlowUiDelegate(navControllerLD, everythingNeedsAContext, dialogTool, progressHack, scopes) {

    private val log = KotlinLogging.logger {}

    override fun getDeviceBarcode() {
        navigate(R.id.action_global_scanJoinerCodeIntroFragment)
    }

    override fun getNetworkSetupType() {
        navigate(R.id.action_global_useStandaloneOrInMeshFragment)
    }

    override fun showGetReadyForSetupScreen() {
        navigate(R.id.action_global_getReadyForSetupFragment)
    }

    override fun showTargetPairingProgressUi() {
        navigate(R.id.action_global_BLEPairingProgressFragment)
    }

    override fun showPricingImpactScreen() {
        navigate(R.id.action_global_pricingImpactFragment)
    }

    override fun showConnectingToDeviceCloudUi() {
        navigate(R.id.action_global_connectingToDeviceCloudFragment)
    }

    override fun showConnectingToDeviceCloudWiFiUi() {
        navigate(R.id.action_global_connectingToDeviceCloudFragment)
    }

    override fun showScanForWifiNetworksUi() {
        navigate(R.id.action_global_scanForWiFiNetworksFragment)
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

    override fun getNewMeshNetworkName() {
        TODO("not implemented")
    }

    override fun getNewMeshNetworkPassword() {
        TODO("not implemented")
    }

    override fun showCreatingMeshNetworkUi() {
        TODO("not implemented")
    }

    override fun showInternetConnectedConnectToDeviceCloudIntroUi() {
        TODO("not implemented")
    }

    override fun showConnectingToDeviceCloudCellularUi() {
        TODO("not implemented")
    }

    override fun getMeshNetworkToJoin() {
        TODO("not implemented")
    }

    override fun getCommissionerBarcode() {
        TODO("not implemented")
    }

    override fun showComissionerPairingProgressUi() {
        TODO("not implemented")
    }

    override fun collectPasswordForMeshToJoin() {
        TODO("not implemented")
    }

    override fun showJoiningMeshNetworkUi() {
        TODO("not implemented")
    }

    override fun showJoinerSetupFinishedUi() {
        TODO("not implemented")
    }

    override fun showCreateNetworkFinishedUi() {
        TODO("not implemented")
    }

    override fun showGatewaySetupFinishedUi() {
        TODO("not implemented")
    }

    override fun showSingleTaskCongratsScreen(singleTaskCongratsMessage: String) {
        TODO("not implemented")
    }

    override fun showSetWifiPasswordUi() {
        TODO("not implemented")
    }

}
