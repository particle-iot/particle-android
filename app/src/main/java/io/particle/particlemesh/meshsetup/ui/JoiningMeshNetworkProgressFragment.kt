package io.particle.particlemesh.meshsetup.ui


import android.os.Bundle
import android.support.annotation.IdRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.toast
import androidx.navigation.fragment.findNavController
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.firmwareprotos.ctrl.Common
import io.particle.particlemesh.common.QATool
import io.particle.particlemesh.common.Result
import io.particle.particlemesh.meshsetup.utils.safeToast
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging


class JoiningMeshNetworkProgressFragment : BaseMeshSetupFragment() {

    private lateinit var cloud: ParticleCloud
    private val log = KotlinLogging.logger {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        cloud = ParticleCloudSDK.getCloud()
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_joining_mesh_network_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        launch { start() }
    }

    private fun markProgress(@IdRes progressStage: Int) {
        launch(UI) {
            val tv: TextView = view!!.findViewById(progressStage)
            tv.text = "✓ " + tv.text
        }
    }

    private suspend fun start() {
        val ctx = requireActivity().applicationContext
        val commish = setupController.commissioner!!
        val target = setupController.targetDevice!!
        try {

            val commissionerCred = setupController.otherParams.value!!.commissionerCredential!!
            handleResult(commish.sendAuth(commissionerCred))
            handleResult(commish.sendStartCommissioner())
            markProgress(R.id.status_stage_1)

            val netInfo = setupController.otherParams.value!!.networkInfo
            val prepJoinerReply = handleResult(target.sendPrepareJoiner(netInfo!!))
            handleResult(commish.sendAddJoiner(prepJoinerReply.eui64, prepJoinerReply.password))
            markProgress(R.id.status_stage_2)

            // FIXME: some delay here appeared to be necessary or joining failed. Refine the number, see if it's even needed
            delay(1000)

            handleResult(target.sendJoinNetwork())
            val deviceId = setupController.deviceToBeSetUpParams.value!!.deviceId!!
            val isInList = pollDevicesForNewDevice(deviceId)
            if (!isInList) {
                ctx.safeToast("Device with ID $deviceId not found in users' list of devices",
                        duration = Toast.LENGTH_LONG)
                return
            }
            markProgress(R.id.status_stage_3)

            delay(2000)
            launch(UI) {
                findNavController().navigate(
                        R.id.action_joiningMeshNetworkProgressFragment_to_nameYourDeviceFragment
                )
            }

        } catch (ex: Exception) {
            QATool.report(ex)
            ctx.safeToast("Error during setup: ${ex.message}")
            return
        }
    }

    private inline fun <reified T> handleResult(result: Result<T, Common.ResultCode>): T {
        val ctx = requireActivity().applicationContext
        return when(result) {
            is Result.Present -> result.value
            is Result.Error,
            is Result.Absent -> {
                val code = result.error
                val msg = "Error response for ${T::class.java.simpleName}, code: $code"
                ctx.safeToast(msg)
                throw IllegalStateException("Cannot continue flow: $msg")
            }
        }
    }

    private suspend fun pollDevicesForNewDevice(deviceId: String): Boolean {
        // FIXME: what should the timing be here?
        val idLower = deviceId.toLowerCase()
        for (i in 0..14) {
            delay(500)
            val userOwnsDevice = try {
                cloud.userOwnsDevice(idLower)
            } catch (ex: Exception) {
                false
            }
            if (userOwnsDevice) {
                log.info { "Found device assigned to user with ID $deviceId" }
                return true
            }
            log.info { "No device found yet assigned to user with ID $deviceId" }
        }
        log.warn { "Timed out waiting for device to be assigned to user with ID $deviceId" }
        return false
    }

}
