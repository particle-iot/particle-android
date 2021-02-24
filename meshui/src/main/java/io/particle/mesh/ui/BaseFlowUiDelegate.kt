package io.particle.mesh.ui

import android.app.Application
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import io.particle.firmwareprotos.ctrl.wifi.WifiNew
import io.particle.mesh.common.logged
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.PostCongratsAction.EXIT
import io.particle.mesh.setup.flow.PostCongratsAction.NOTHING
import io.particle.mesh.setup.flow.PostCongratsAction.RESET_TO_START
import io.particle.mesh.setup.ui.ProgressHack
import io.particle.mesh.ui.controlpanel.ControlPanelCongratsFragmentArgs
import io.particle.mesh.ui.controlpanel.ControlPanelSimStatusChangeFragmentArgs
import io.particle.mesh.ui.controlpanel.ControlPanelWifiInspectNetworkFragmentArgs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging


abstract class BaseFlowUiDelegate(
    private val navControllerLD: LiveData<NavigationTool?>,
    private val everythingNeedsAContext: Application,
    override val dialogTool: DialogTool,
    protected val progressHack: ProgressHack,
    protected val scopes: Scopes,
    private val terminator: MeshFlowTerminator
) : FlowUiDelegate {

    private val log = KotlinLogging.logger {}

    protected var shownTargetInitialIsConnectedScreen by log.logged(false)

    override fun getDeviceBarcode() {
        navigate(R.id.action_global_scanJoinerCodeIntroFragment)
    }

    override fun showGetReadyForSetupScreen() {
        navigate(R.id.action_global_getReadyForSetupFragment)
    }

    override fun getNetworkSetupType() {
        navigate(R.id.action_global_useStandaloneOrInMeshFragment)
    }

    override fun showConnectingToDeviceCloudUi() {
        navigate(R.id.action_global_connectingToDeviceCloudFragment)
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

    override fun showConnectingToDeviceCloudCellularUi() {
        navigate(R.id.action_global_boronConnectingStatusFragment)
    }

    override fun getMeshNetworkToJoin() {
        navigate(R.id.action_global_scanForMeshNetworksFragment)
    }

    override fun getCommissionerBarcode() {
        navigate(R.id.action_global_manualCommissioningAddToNetworkFragment)
    }

    override fun showCommissionerPairingProgressUi() {
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

    override fun showConnectingToDeviceCloudWiFiUi() {
        navigate(R.id.action_global_argonConnectingStatusFragment)
    }

    override fun onTargetPairingSuccessful(deviceName: String): Boolean {
        if (shownTargetInitialIsConnectedScreen) {
            return false // already shown, no need to show again
        }
        shownTargetInitialIsConnectedScreen = true
        showCongratsScreen("Successfully paired with $deviceName")
        return true
    }

    override fun showCongratsScreen(
        congratsMessage: String,
        postCongratsAction: PostCongratsAction
    ) {

        navigate(
            R.id.action_global_controlPanelCongratsFragment,
            ControlPanelCongratsFragmentArgs(congratsMessage).toBundle(),
            shouldPopBackstack = false
        )

        when (postCongratsAction) {
            EXIT -> terminator.terminateFlow(FlowTerminationAction.NoFurtherAction)
            RESET_TO_START -> {
                log.info { "Resetting to top of stack" }
                val navTool = navControllerLD.value
                scopes.onMain {
                    delay(2000)
                    navTool?.let {
                        navTool.popBackStack(R.id.controlPanelLandingFragment)
                    }
                }
            }
            NOTHING -> { /* no-op */ }
        }
    }

    override fun showSnackbarWithMessage(messageToShow: String) {
        dialogTool.newSnackbarRequest(messageToShow)
        scopes.onMain {
            delay(50)
            dialogTool.clearSnackbarRequest()
        }
    }

    override fun showInspectCurrentWifiNetworkUi(
        currentNetwork: WifiNew.GetCurrentNetworkReply?,
        connectingToTargetUiShown: Boolean
    ) {
        navigate(
            R.id.action_global_controlPanelWifiInspectNetworkFragment,
            ControlPanelWifiInspectNetworkFragmentArgs(currentNetwork).toBundle(),
            shouldPopBackstack = connectingToTargetUiShown
        )
    }

    override fun showControlPanelCellularOptionsUi() {
        navigate(R.id.action_global_controlPanelCellularOptionsFragment, shouldPopBackstack = false)
    }

    override fun showControlPanelSimUnpauseUi() {
        navigate(
            R.id.action_global_controlPanelSimStatusChangeFragment,
            ControlPanelSimStatusChangeFragmentArgs(SimStatusChangeMode.UNPAUSE).toBundle(),
            shouldPopBackstack = false
        )
    }

    override fun showControlPanelSimDeactivateUi() {
        navigate(
            R.id.action_global_controlPanelSimStatusChangeFragment,
            ControlPanelSimStatusChangeFragmentArgs(SimStatusChangeMode.DEACTIVATE).toBundle(),
            shouldPopBackstack = false
        )
    }

    override fun showControlPanelSimReactivateUi() {
        navigate(
            R.id.action_global_controlPanelSimStatusChangeFragment,
            ControlPanelSimStatusChangeFragmentArgs(SimStatusChangeMode.REACTIVATE).toBundle(),
            shouldPopBackstack = false
        )
    }

    override fun showSetCellularDataLimitUi() {
        navigate(
            R.id.action_global_controlPanelCellularDataLimitFragment,
            shouldPopBackstack = false
        )
    }

    override fun showMeshInspectNetworkUi(connectingToTargetUiShown: Boolean) {
        navigate(
            R.id.action_global_controlPanelMeshInspectNetworkFragment,
            shouldPopBackstack = connectingToTargetUiShown
        )
    }

    override fun showEthernetOptionsUi(connectingToTargetUiShown: Boolean) {
        navigate(
            R.id.action_global_controlPanelEthernetOptionsFragment,
            shouldPopBackstack = connectingToTargetUiShown
        )
    }

    override fun showSetupFinishedUi() {
        navigate(R.id.action_global_letsGetBuildingFragment)
    }

    override fun showMeshOptionsUi(connectingToTargetUiShown: Boolean) {
        navigate(
            R.id.action_global_controlPanelMeshOptionsFragment,
            shouldPopBackstack = connectingToTargetUiShown
        )
    }

    override fun showControlPanelWifiManageList() {
        navigate(
            R.id.action_global_controlPanelWifiManageNetworksFragment,
            shouldPopBackstack = false
        )
    }

    override fun popBackStack(): Boolean {
        log.info { "popBackStack()" }
        return navControllerLD.value?.popBackStack() ?: false
    }

    override fun showGlobalProgressSpinner(shouldShow: Boolean) {
        log.info { "showGlobalProgressSpinner(): $shouldShow" }
        progressHack.showGlobalProgressSpinner(shouldShow)
    }

    override fun getString(stringId: Int): String {
        return everythingNeedsAContext.getString(stringId)
    }

    protected fun navigate(
        @IdRes navTargetId: Int,
        args: Bundle? = null,
        shouldPopBackstack: Boolean = true
    ) {
        val nav = navControllerLD.value
        if (nav == null) {
            log.warn { "Attempted to use nav controller but it was null!" }
            return
        }

        val name = everythingNeedsAContext.resources.getResourceName(navTargetId)
        log.info { "Navigating to new target: $name, shouldPopBackstack=$shouldPopBackstack" }

        showGlobalProgressSpinner(false)
        scopes.mainThreadScope.launch {
            if (shouldPopBackstack) {
                log.info { "Popping back stack" }
                nav.popBackStack()
            }
            if (args == null) {
                nav.navigate(navTargetId)
            } else {
                nav.navigate(navTargetId, args)
            }
        }
    }
}
