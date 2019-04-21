package io.particle.mesh.setup.flow.modules.cloudconnection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.models.ParticleSimStatus
import io.particle.mesh.common.Result
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.connection.ResultCode
import io.particle.mesh.setup.flow.Clearable
import io.particle.mesh.setup.flow.ExceptionType
import io.particle.mesh.setup.flow.FlowException
import io.particle.mesh.setup.flow.FlowManager
import io.particle.mesh.R
import kotlinx.coroutines.delay
import mu.KotlinLogging


class BoronSteps(
    private val flowManager: FlowManager,
    private val cloud: ParticleCloud
) : Clearable {

    private val log = KotlinLogging.logger {}

    val targetDeviceIccid: LiveData<String?> = MutableLiveData()
    val isSimActivatedLD: LiveData<Boolean?> = MutableLiveData()

    private var connectingToCloudUiShown = false

    override fun clearState() {
        log.info { "clearState()" }
        targetDeviceIccid.castAndSetOnMainThread(null)
        isSimActivatedLD.castAndSetOnMainThread(null)
        connectingToCloudUiShown = false
    }

    suspend fun ensureIccidRetrieved() {
        log.info { "ensureIccidRetrieved()" }
        if (targetDeviceIccid.value.truthy()) {
            return
        }

        val targetXceiver = flowManager.bleConnectionModule.targetDeviceTransceiverLD.value!!

        val iccidReply = targetXceiver.sendGetIccId()
        when (iccidReply) {
            is Result.Present -> {
                targetDeviceIccid.castAndPost(iccidReply.value.iccid)
            }

            is Result.Error -> {
                if (iccidReply.error == ResultCode.INVALID_STATE) {
                    targetXceiver.sendReset()
                    delay(2000)
                    throw FlowException("INVALID_STATE received while getting ICCID; " +
                            "sending reset command and restarting flow"
                    )
                }
                throw FlowException("Error ${iccidReply.error} when retrieving ICCID")
            }

            is Result.Absent -> {
                throw FlowException("Unknown error when retrieving ICCID")
            }
        }
    }

    suspend fun ensureSimActivationStatusUpdated() {
        log.info { "ensureSimActivationStatusUpdated()" }
        if (isSimActivatedLD.value.truthy()) {
            return
        }

        // is SIM activated
        val statusAndMsg = cloud.checkSim(targetDeviceIccid.value!!)

        val isActive = when (statusAndMsg.first) {
            ParticleSimStatus.READY_TO_ACTIVATE -> false

            ParticleSimStatus.ACTIVATED_FREE,
            ParticleSimStatus.ACTIVATED -> true

            ParticleSimStatus.NOT_FOUND,
            ParticleSimStatus.NOT_OWNED_BY_USER,
            ParticleSimStatus.ERROR -> {
                throw FlowException(
                    statusAndMsg.second,
                    ExceptionType.ERROR_RECOVERABLE,
                    showErrorAsDialog = true
                )
            }
        }

        isSimActivatedLD.castAndPost(isActive)
    }

    suspend fun ensureSimActivated() {
        log.info { "ensureSimActivated()" }
        if (isSimActivatedLD.value.truthy()) {
            return
        }

        for (i in 0..2) {

            val statusCode = doActivateSim()
            if (statusCode == 200) {
                isSimActivatedLD.castAndPost(true)
                return

            } else if (statusCode == 504) {
                continue

            } else {
                throw FlowException(
                    "There was an error activating your SIM, please retry setup. " +
                            "If you have retried many times, please contact support.",
                    ExceptionType.ERROR_RECOVERABLE,
                    showErrorAsDialog = true
                )
            }
        }

        throw FlowException(
            "SIM activation is taking longer than expected. Please retry your SIM activation. " +
                    "If you have retried many times, please contact support.",
            ExceptionType.ERROR_RECOVERABLE,
            showErrorAsDialog = true
        )
    }

    suspend fun ensureConnectingToCloudUiShown() {
        if (connectingToCloudUiShown) {
            return
        }
        connectingToCloudUiShown = true
//        flowManager.navigate(R.id.action_global_boronConnectingStatusFragment)
    }

    private fun doActivateSim(): Int {
        val response = cloud.activateSim(targetDeviceIccid.value!!)
        return response.status
    }

}
