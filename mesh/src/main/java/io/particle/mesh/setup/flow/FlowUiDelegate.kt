package io.particle.mesh.setup.flow

import androidx.annotation.StringRes
import io.particle.firmwareprotos.ctrl.wifi.WifiNew


enum class PostCongratsAction {
    NOTHING,
    EXIT,
    RESET_TO_START
}


interface FlowUiDelegate {

    val rootDestinationId: Int

    val dialogTool: DialogTool

    fun getString(@StringRes stringId: Int): String

    fun showCongratsScreen(
        congratsMessage: String,
        postCongratsAction: PostCongratsAction = PostCongratsAction.NOTHING
    )

    fun showSnackbarWithMessage(messageToShow: String)

    fun showGlobalProgressSpinner(shouldShow: Boolean)

    fun getDeviceBarcode()

    fun getNetworkSetupType()

    fun showGetReadyForSetupScreen()

    fun showTargetPairingProgressUi()

    fun showConnectingToDeviceCloudUi()

    fun showScanForWifiNetworksUi()

    fun showNameDeviceUi()

    fun showBleOtaIntroUi()

    fun showBleOtaUi()

    fun showSetWifiPasswordUi()

    fun onTargetPairingSuccessful(deviceName: String): Boolean

    fun getNewMeshNetworkName()

    fun getNewMeshNetworkPassword()

    fun showCreatingMeshNetworkUi()

    fun showInternetConnectedConnectToDeviceCloudIntroUi()

    fun showConnectingToDeviceCloudWiFiUi()

    fun showConnectingToDeviceCloudCellularUi()

    fun getMeshNetworkToJoin()

    fun getCommissionerBarcode()

    fun showCommissionerPairingProgressUi()

    fun collectPasswordForMeshToJoin()

    fun showJoiningMeshNetworkUi()

    fun showJoinerSetupFinishedUi()

    fun showSetupFinishedUi()

    fun showCreateNetworkFinishedUi()

    fun showInspectCurrentWifiNetworkUi(
        currentNetwork: WifiNew.GetCurrentNetworkReply?,
        connectingToTargetUiShown: Boolean
    )

    fun showControlPanelCellularOptionsUi()

    fun showControlPanelSimUnpauseUi()

    fun showControlPanelSimDeactivateUi()

    fun showControlPanelSimReactivateUi()

    fun showSetCellularDataLimitUi()

    fun showMeshInspectNetworkUi(connectingToTargetUiShown: Boolean)

    fun showMeshOptionsUi(connectingToTargetUiShown: Boolean)

    fun showEthernetOptionsUi(connectingToTargetUiShown: Boolean)

    fun showControlPanelWifiManageList()

    fun popBackStack(): Boolean
}


