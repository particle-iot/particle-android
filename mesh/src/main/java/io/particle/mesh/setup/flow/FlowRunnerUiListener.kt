package io.particle.mesh.setup.flow

import androidx.lifecycle.LiveData
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.ScanNetworksReply.Network
import io.particle.mesh.setup.flow.context.NetworkSetupType
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.context.SetupDevice
import io.particle.mesh.setup.flow.modules.cloudconnection.WifiNetworksScannerLD
import io.particle.mesh.setup.flow.modules.cloudconnection.WifiScanData
import io.particle.mesh.setup.flow.modules.meshsetup.TargetDeviceMeshNetworksScanner


class FlowRunnerUiListener(
    private val ctxs: SetupContexts
) {

    val wifi = WifiData(ctxs)
    val deviceData = DeviceData(ctxs)
    val mesh = MeshData(ctxs)
    val cloud = CloudData(ctxs)

    val targetDevice: SetupDevice
        get() = ctxs.targetDevice
    val commissioner: SetupDevice
        get() = ctxs.commissioner
    val singleTaskCongratsMessage: String
        get() = ctxs.singleStepCongratsMessage


    fun setNetworkSetupType(setupType: NetworkSetupType) {
        ctxs.device.updateNetworkSetupType(setupType)
    }

    fun onGetReadyNextButtonClicked() {
        ctxs.updateGetReadyNextButtonClicked(true)
    }

    fun updateShouldConnectToDeviceCloudConfirmed(confirmed: Boolean) {
       ctxs.cloud.updateShouldConnectToDeviceCloudConfirmed(confirmed)
    }

}


class MeshData(private val ctxs: SetupContexts) {

    val newNetworkIdLD: LiveData<String?> = ctxs.mesh.newNetworkIdLD
    val networkCreatedOnLocalDeviceLD: LiveData<Boolean?> = ctxs.mesh.networkCreatedOnLocalDeviceLD
    val commissionerStartedLD: LiveData<Boolean?> = ctxs.mesh.commissionerStartedLD
    val targetJoinedMeshNetworkLD: LiveData<Boolean?> = ctxs.mesh.targetJoinedMeshNetworkLD
    val showNewNetworkOptionInScanner = ctxs.mesh.showNewNetworkOptionInScanner

    fun getTargetDeviceVisibleMeshNetworksLD(): LiveData<List<Mesh.NetworkInfo>?> {
        return TargetDeviceMeshNetworksScanner(ctxs.targetDevice.transceiverLD, ctxs.scopes)
    }

    fun updateMeshNetworkToJoinCommissionerPassword(password: String) {
        ctxs.mesh.updateTargetDeviceMeshNetworkToJoinCommissionerPassword(password)
    }

    fun updateNewNetworkName(name: String) {
        ctxs.mesh.updateNewNetworkName(name)
    }

    fun updateNewNetworkPassword(password: String) {
        ctxs.mesh.updateNewNetworkPassword(password)
    }

    fun updateNetworkSetupType(networkSetupType: NetworkSetupType) {
        ctxs.device.updateNetworkSetupType(networkSetupType)
    }

    fun onUserSelectedCreateNewNetwork() {
        ctxs.mesh.onUserSelectedCreateNewNetwork()
    }

    fun updateSelectedMeshNetworkToJoin(networkInfo: Mesh.NetworkInfo) {
        ctxs.mesh.updateSelectedMeshNetworkToJoin(networkInfo)
    }

}


class DeviceData(private val ctxs: SetupContexts) {

    val networkSetupTypeLD: LiveData<NetworkSetupType?> = ctxs.device.networkSetupTypeLD
    val bleUpdateProgress: LiveData<Int?> = ctxs.device.bleOtaProgress
    var shouldDetectEthernet = ctxs.device.shouldDetectEthernet
    var firmwareUpdateCount = ctxs.device.firmwareUpdateCount

    fun updateUserConsentedToFirmwareUpdate(consented: Boolean) {
        ctxs.device.updateUserConsentedToFirmwareUpdate(consented)
    }

}


class CloudData(private val ctxs: SetupContexts) {

    val targetDeviceNameToAssignLD: LiveData<String?> = ctxs.cloud.targetDeviceNameToAssignLD
    val pricingImpact = ctxs.cloud.pricingImpact

    fun updateTargetDeviceNameToAssign(name: String) {
        ctxs.cloud.updateTargetDeviceNameToAssign(name)
    }

    fun updatePricingImpactConfirmed(confirmed: Boolean) {
        ctxs.cloud.updatePricingImpactConfirmed(confirmed)
    }

}


class WifiData(private val ctxs: SetupContexts) {

    val targetWifiNetworkJoinedLD: LiveData<Boolean?> = ctxs.wifi.targetWifiNetworkJoinedLD

    val wifiNetworkToConfigure: Network?
        get() = ctxs.wifi.targetWifiNetworkLD.value


    fun getWifiScannerForTargetDevice(): LiveData<List<WifiScanData>?> {
        return WifiNetworksScannerLD(
            ctxs.targetDevice.transceiverLD,
            ctxs.scopes
        )
    }

    fun setWifiNetworkToConfigure(network: Network) {
        ctxs.wifi.updateTargetWifiNetwork(network)
    }

    fun setPasswordForWifiNetworkToConfigure(password: String) {
        ctxs.wifi.updateTargetWifiNetworkPassword(password)
    }

}