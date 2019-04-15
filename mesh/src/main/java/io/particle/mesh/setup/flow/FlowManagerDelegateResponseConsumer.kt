package io.particle.mesh.setup.flow

import io.particle.firmwareprotos.ctrl.wifi.WifiNew.ScanNetworksReply
import io.particle.mesh.setup.ui.BarcodeData


interface FlowRunnerUiResponseReceiverrrr {
    fun setTargetDeviceInfo(dataMatrix: BarcodeData, useEthernet: Boolean): MeshSetupFlowException?

    fun setTargetPerformFirmwareUpdate(update: Boolean): MeshSetupFlowException?
    fun setTargetDeviceLeaveNetwork(leave: Boolean): MeshSetupFlowException?

    fun setSelectStandAloneOrMeshSetup(meshSetup: Boolean): MeshSetupFlowException?
    fun setOptionalSelectedNetwork(selectedNetworkExtPanID: String?): MeshSetupFlowException?

    fun setPricingImpactDone(): MeshSetupFlowException?
    fun setInfoDone(): MeshSetupFlowException?

    fun setDeviceName(name: String, onComplete: (MeshSetupFlowException?) -> Unit)
    fun setAddOneMoreDevice(addOneMoreDevice: Boolean): MeshSetupFlowException?

    fun setNewNetworkName(name: String): MeshSetupFlowException?
    fun setNewNetworkPassword(password: String): MeshSetupFlowException?
    fun setSelectedWifiNetwork(selectedNetwork: ScanNetworksReply.Network): MeshSetupFlowException?
    fun setSelectedWifiNetworkPassword(password: String, onComplete: (MeshSetupFlowException?) -> Unit)

    fun setSelectedNetwork(selectedNetworkExtPanID: String): MeshSetupFlowException?
    fun setCommissionerDeviceInfo(dataMatrix: BarcodeData): MeshSetupFlowException?
    fun setSelectedNetworkPassword(password: String, onComplete: (MeshSetupFlowException?) -> Unit)

    fun rescanNetworks(): MeshSetupFlowException?
}


enum class MeshSetupFlowState {
    TargetDeviceConnecting,
    TargetDeviceConnected,
    TargetDeviceReady,

    TargetDeviceScanningForNetworks,
    TargetInternetConnectedDeviceScanningForNetworks,
    TargetDeviceScanningForWifiNetworks,

    TargetDeviceConnectingToInternetStarted,
    TargetDeviceConnectingToInternetStep0Done, // used for activating sim card only
    TargetDeviceConnectingToInternetStep1Done,
    TargetDeviceConnectingToInternetCompleted,

    CommissionerDeviceConnecting,
    CommissionerDeviceConnected,
    CommissionerDeviceReady,

    JoiningNetworkStarted,
    JoiningNetworkStep1Done,
    JoiningNetworkStep2Done,
    JoiningNetworkCompleted,

    FirmwareUpdateProgress,
    FirmwareUpdateFileComplete,
    FirmwareUpdateComplete,

    CreateNetworkStarted,
    CreateNetworkStep1Done,
    CreateNetworkCompleted,

    SetupCanceled
}
