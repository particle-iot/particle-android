package io.particle.mesh.setup.flow

import androidx.annotation.StringRes


interface FlowUiDelegate {

    val dialogTool: DialogTool

    fun getString(@StringRes stringId: Int): String

    fun showSingleTaskCongratsScreen(singleTaskCongratsMessage: String)

    fun showGlobalProgressSpinner(shouldShow: Boolean)

    fun getDeviceBarcode()

    fun getNetworkSetupType()

    fun showGetReadyForSetupScreen()

    fun showTargetPairingProgressUi()

    fun showPricingImpactScreen()

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

    fun showComissionerPairingProgressUi()

    fun collectPasswordForMeshToJoin()

    fun showJoiningMeshNetworkUi()

    fun showJoinerSetupFinishedUi()

    fun showCreateNetworkFinishedUi()

}


