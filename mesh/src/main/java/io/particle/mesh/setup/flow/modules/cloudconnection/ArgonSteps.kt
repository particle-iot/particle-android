package io.particle.mesh.setup.flow.modules.cloudconnection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.ScanNetworksReply
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.Security.NO_SECURITY
import io.particle.mesh.common.Result
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.connection.ResultCode
import io.particle.mesh.setup.flow.Clearable
import io.particle.mesh.setup.flow.FlowException
import io.particle.mesh.setup.flow.FlowManager
import io.particle.mesh.setup.ui.DialogSpec.StringDialogSpec
import io.particle.mesh.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging


class ArgonSteps(
    private val flowManager: FlowManager
) : Clearable {

    private val log = KotlinLogging.logger {}

    val targetWifiNetwork: LiveData<ScanNetworksReply.Network?> = MutableLiveData()
    val targetWifiNetworkPassword: LiveData<String?> = MutableLiveData()
    val targetWifiNetworkJoinedLD: LiveData<Boolean?> = MutableLiveData()

    private var connectingToCloudUiShown = false

    override fun clearState() {
        connectingToCloudUiShown = false

        val setToNulls = listOf(
            targetWifiNetwork,
            targetWifiNetworkPassword,
            targetWifiNetworkJoinedLD
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

    suspend fun ensureTargetWifiNetworkCaptured() {
        log.info { "ensureTargetWifiNetworkCaptured()" }
        if (targetWifiNetwork.value != null) {
            return
        }

        val wifiNetworkSelectionSuspender = liveDataSuspender({ targetWifiNetwork.nonNull() })
        withContext(Dispatchers.Main) {
            flowManager.navigate(R.id.action_global_scanForWiFiNetworksFragment)
            wifiNetworkSelectionSuspender.awaitResult()
        }
    }

    suspend fun ensureTargetWifiNetworkPasswordCaptured() {
        if (targetWifiNetworkPassword.value.truthy()
            || targetWifiNetwork.value?.security == NO_SECURITY) {
            return
        }


        // FIXME: VALIDATE PASSWORD HERE BY CONNECTING; SHOW DIALOG IF PASSWORD IS BAD!

        val passwordSuspender = liveDataSuspender({ targetWifiNetworkPassword.nonNull() })
        withContext(Dispatchers.Main) {
            flowManager.navigate(R.id.action_global_enterWifiNetworkPasswordFragment)
            passwordSuspender.awaitResult()
        }
        flowManager.showGlobalProgressSpinner(true)
    }

    suspend fun ensureTargetWifiNetworkJoined() {
        if (targetWifiNetworkJoinedLD.value.truthy()) {
            return
        }

        val xceiver = flowManager.bleConnectionModule.targetDeviceTransceiverLD.value!!

        try {
            flowManager.showGlobalProgressSpinner(true)

            val joinNewNetworkResponse = xceiver.sendJoinNewNetwork(
                targetWifiNetwork.value!!,
                targetWifiNetworkPassword.value
            )
            val genericError = FlowException("Could not join to Wi-Fi network due to an unknown error")
            when (joinNewNetworkResponse) {
                is Result.Present -> targetWifiNetworkJoinedLD.castAndPost(true)
                is Result.Absent -> throw genericError
                is Result.Error -> {
                    if (joinNewNetworkResponse.error == ResultCode.NOT_FOUND) {

                        flowManager.clearDialogResult()
                        val suspender = liveDataSuspender({ flowManager.dialogResultLD.nonNull() })
                        val result = withContext(Dispatchers.Main) {
                            flowManager.newDialogRequest(
                                // FIXME: i18n!
                                StringDialogSpec(
                                    "Could not connect to Wi-Fi.  Please try entering your password again."
                                )
                            )
                            suspender.awaitResult()
                        }
                        flowManager.clearDialogResult()
                        targetWifiNetworkPassword.castAndPost(null)
                        connectingToCloudUiShown = false
                        throw FlowException("Error connecting to Wi-Fi (bad password?)")

                    } else {
                        throw genericError
                    }
                }
            }

        } finally {
            flowManager.showGlobalProgressSpinner(false)
        }
    }

    suspend fun ensureConnectingToCloudUiShown() {
        if (connectingToCloudUiShown) {
            return
        }
        connectingToCloudUiShown = true
        flowManager.navigate(R.id.action_global_argonConnectingStatusFragment)
    }

}