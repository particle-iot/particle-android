package io.particle.mesh.setup.flow.modules.cloudconnection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.ScanNetworksReply
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.flow.Clearable
import io.particle.mesh.setup.flow.FlowManager
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.withContext
import mu.KotlinLogging


class ArgonSteps(
    private val flowManager: FlowManager
) : Clearable {

    private val log = KotlinLogging.logger {}

    val targetWifiNetwork: LiveData<ScanNetworksReply.Network?> = MutableLiveData()
    val targetWifiNetworkPassword: LiveData<String?> = MutableLiveData()

    private var connectingToCloudUiShown = false
    private var targetWifiNetworkJoined = false

    override fun clearState() {
        connectingToCloudUiShown = false
        targetWifiNetworkJoined = false

        val setToNulls = listOf(
            targetWifiNetwork,
            targetWifiNetworkPassword
        )
        for (ld in setToNulls) {
            ld.castAndPost(null)
        }
    }

    fun updateTargetWifiNetwork(network: ScanNetworksReply.Network) {
        log.info { "updateTargetWifiNetwork(): $network" }
        targetWifiNetwork.castAndPost(network)
    }

    fun updateTargetWifiNetworkPassword(password: String) {
        log.info { "updateTargetWifiNetworkPassword()" }
        targetWifiNetworkPassword.castAndPost(password)
    }

    suspend fun ensureShowConnectToDeviceCloudConfirmation() {
        log.info { "ensureShowConnectToDeviceCloudConfirmation()" }

        val confirmationLD = flowManager.cloudConnectionModule.shouldConnectToDeviceCloudConfirmed
        if (confirmationLD.value.truthy()) {
            return
        }

        val suspender = liveDataSuspender({ confirmationLD.nonNull() })
        withContext(UI) {
            flowManager.navigate(R.id.action_global_argonConnectToDeviceCloudIntroFragment)
            suspender.awaitResult()
        }
    }

    suspend fun ensureTargetWifiNetworkCaptured() {
        log.info { "ensureTargetWifiNetworkCaptured()" }
        if (targetWifiNetwork.value != null) {
            return
        }

        val wifiNetworkSelectionSuspender = liveDataSuspender({ targetWifiNetwork.nonNull() })
        withContext(UI) {
            flowManager.navigate(R.id.action_global_scanForWiFiNetworksFragment)
            wifiNetworkSelectionSuspender.awaitResult()
        }
    }

    suspend fun ensureTargetWifiNetworkPasswordCaptured() {
        if (targetWifiNetworkPassword.value.truthy()) {
            return
        }


        // FIXME: VALIDATE PASSWORD HERE BY CONNECTING; SHOW DIALOG IF PASSWORD IS BAD!

        val passwordSuspender = liveDataSuspender({ targetWifiNetworkPassword.nonNull() })
        withContext(UI) {
            flowManager.navigate(R.id.action_global_enterWifiNetworkPasswordFragment)
            passwordSuspender.awaitResult()
        }
        flowManager.showGlobalProgressSpinner(true)
    }

    suspend fun ensureTargetWifiNetworkJoined() {
        if (targetWifiNetworkJoined) {
            return
        }

        val xceiver = flowManager.bleConnectionModule.targetDeviceTransceiverLD.value!!

        try {
            flowManager.showGlobalProgressSpinner(true)
            xceiver.sendJoinNewNetwork(
                targetWifiNetwork.value!!,
                targetWifiNetworkPassword.value!!
            ).throwOnErrorOrAbsent()
        } finally {
            flowManager.showGlobalProgressSpinner(false)
        }

        targetWifiNetworkJoined = true
    }

    suspend fun ensureConnectingToCloudUiShown() {
        if (connectingToCloudUiShown) {
            return
        }
        connectingToCloudUiShown = true
        flowManager.navigate(R.id.action_global_argonConnectingStatusFragment)
    }

}