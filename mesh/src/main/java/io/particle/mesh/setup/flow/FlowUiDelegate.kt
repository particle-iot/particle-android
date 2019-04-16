package io.particle.mesh.setup.flow

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.ScanNetworksReply
import io.particle.mesh.setup.BarcodeData.CompleteBarcodeData
import io.particle.mesh.setup.flow.DialogTool
import io.particle.mesh.setup.flow.context.NetworkSetupType
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.context.SetupDevice
import io.particle.mesh.setup.flow.modules.cloudconnection.WifiNetworksScannerLD
import io.particle.mesh.setup.flow.modules.cloudconnection.WifiScanData


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

    fun showGatewaySetupFinishedUi()

}


class FlowRunnerUiResponseReceiver(
    private val ctxs: SetupContexts,
    private val cloud: ParticleCloud
) {

    val wifiNetworkToConfigure: ScanNetworksReply.Network?
        get() = ctxs.wifi.targetWifiNetworkLD.value

    val targetDevice: SetupDevice
        get() = ctxs.ble.targetDevice

    val singleTaskCongratsMessage: String
        get() = ctxs.singleStepCongratsMessage


    fun setTargetDeviceBarcodeData(barcodeData: CompleteBarcodeData) {
        ctxs.ble.targetDevice.updateBarcode(barcodeData, cloud)
    }

    fun setNetworkSetupType(setupType: NetworkSetupType) {
        ctxs.device.updateNetworkSetupType(setupType)
    }

    fun onGetReadyNextButtonClicked() {
        ctxs.updateGetReadyNextButtonClicked(true)
    }

    fun getWifiScannerForTargetDevice(): LiveData<List<WifiScanData>?> {
        return WifiNetworksScannerLD(ctxs.ble.targetDevice.transceiverLD, ctxs.scopes)
    }

    fun setWifiNetworkToConfigure(network: ScanNetworksReply.Network) {
        ctxs.wifi.updateTargetWifiNetwork(network)
    }

    fun setPasswordForWifiNetworkToConfigure(password: String) {
        ctxs.wifi.updateTargetWifiNetworkPassword(password)
    }

}