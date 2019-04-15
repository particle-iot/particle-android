package io.particle.mesh.setup.flow.modules

import android.app.Application
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.ScanNetworksReply
import io.particle.mesh.R
import io.particle.mesh.common.logged
import io.particle.mesh.setup.flow.DialogTool
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.NetworkSetupType
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.context.SetupDevice
import io.particle.mesh.setup.flow.modules.cloudconnection.WifiNetworksScannerLD
import io.particle.mesh.setup.flow.modules.cloudconnection.WifiScanData
import io.particle.mesh.setup.ui.BarcodeData.CompleteBarcodeData
import io.particle.mesh.setup.ui.HashtagWinningFragmentArgs
import io.particle.mesh.setup.ui.ProgressHack
import kotlinx.coroutines.launch
import mu.KotlinLogging


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


abstract class BaseFlowUiDelegate(
    private val navControllerLD: LiveData<NavController?>,
    private val everythingNeedsAContext: Application,
    override val dialogTool: DialogTool,
    protected val progressHack: ProgressHack,
    protected val scopes: Scopes
) : FlowUiDelegate {

    private val log = KotlinLogging.logger {}

    protected var shownTargetInitialIsConnectedScreen by log.logged(false)

    override fun onTargetPairingSuccessful(deviceName: String): Boolean {
        if (shownTargetInitialIsConnectedScreen) {
            return false // already shown, no need to show again
        }
        shownTargetInitialIsConnectedScreen = true
        showSingleTaskCongratsScreen("Successfully paired with $deviceName")
//        showCongratsScreen("Successfully paired with $deviceName")
        return true
    }

    override fun showGlobalProgressSpinner(shouldShow: Boolean) {
        log.info { "showGlobalProgressSpinner(): $shouldShow" }
        progressHack.showGlobalProgressSpinner(shouldShow)
    }

    override fun getString(stringId: Int): String {
        return everythingNeedsAContext.getString(stringId)
    }

    private fun showCongratsScreen(message: String) {
        navigate(
            R.id.action_global_hashtagWinningFragment,
            HashtagWinningFragmentArgs(message).toBundle()
        )

    }

    protected fun navigate(@IdRes navTargetId: Int, args: Bundle? = null) {
        val nav = navControllerLD.value
        if (nav == null) {
            log.warn { "Attempted to use nav controller but it was null!" }
            return
        }

        showGlobalProgressSpinner(false)
        scopes.mainThreadScope.launch {
            nav.popBackStack()
            if (args == null) {
                nav.navigate(navTargetId)
            } else {
                nav.navigate(navTargetId, args)
            }
        }
    }
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