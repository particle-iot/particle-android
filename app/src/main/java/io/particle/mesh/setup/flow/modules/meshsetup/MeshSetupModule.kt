package io.particle.mesh.setup.flow.modules.meshsetup

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import io.particle.firmwareprotos.ctrl.Common.ResultCode
import io.particle.firmwareprotos.ctrl.Common.ResultCode.NOT_FOUND
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.firmwareprotos.ctrl.mesh.Mesh.GetNetworkInfoReply
import io.particle.firmwareprotos.ctrl.mesh.Mesh.NetworkInfo
import io.particle.mesh.common.Result
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.flow.Clearable
import io.particle.mesh.setup.flow.FlowException
import io.particle.mesh.setup.flow.FlowManager
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.withContext


class MeshSetupModule(
        private val flowManager: FlowManager,
        val targetDeviceVisibleMeshNetworksLD: TargetDeviceMeshNetworksScanner
) : Clearable {

    val targetDeviceMeshNetworkToJoinLD: LiveData<NetworkInfo?> = MutableLiveData()
    val targetDeviceMeshNetworkToJoinCommissionerPassword: LiveData<String?> = MutableLiveData()
    val targetJoinedMeshNetworkLD: LiveData<Boolean?> = MutableLiveData()
    val commissionerStartedLD: LiveData<Boolean?> = MutableLiveData()

    private var targetJoinedSuccesfully = false

    private val targetXceiver
        get() = flowManager.bleConnectionModule.targetDeviceTransceiverLD.value


    override fun clearState() {
        (targetDeviceMeshNetworkToJoinLD as MutableLiveData).postValue(null)
        (targetDeviceMeshNetworkToJoinCommissionerPassword as MutableLiveData).postValue(null)
        (targetJoinedMeshNetworkLD as MutableLiveData).postValue(null)
        (commissionerStartedLD as MutableLiveData).postValue(null)
        targetJoinedSuccesfully = false
    }

    fun updateSelectedMeshNetworkToJoin(meshNetworkToJoin: Mesh.NetworkInfo) {
        (targetDeviceMeshNetworkToJoinLD as MutableLiveData).postValue(meshNetworkToJoin)
    }

    fun updateTargetMeshNetworkCommissionerPassword(password: String) {
        (targetDeviceMeshNetworkToJoinCommissionerPassword as MutableLiveData).postValue(password)
    }

    fun updateCommissionerStarted(started: Boolean) {
        (commissionerStartedLD as MutableLiveData).postValue(started)
    }

    fun updateTargetJoinedMeshNetwork(joined: Boolean) {
        (targetJoinedMeshNetworkLD as MutableLiveData).postValue(joined)
    }

    suspend fun ensureRemovedFromExistingNetwork() {
        val reply: Result<GetNetworkInfoReply, ResultCode> = targetXceiver!!.sendGetNetworkInfo()
        when(reply) {
            is Result.Present -> {


                // FIXME: PROMPT USER


                targetXceiver!!.sendLeaveNetwork().throwOnErrorOrAbsent()
            }
            is Result.Absent -> throw FlowException("No result received when getting existing network")
            is Result.Error -> {
                if (reply.error == NOT_FOUND) {
                    return
                } else {
                    throw FlowException("Error when getting existing network")
                }
            }
        }
    }

    suspend fun ensureResetNetworkCredentials() {
        targetXceiver!!.sendResetNetworkCredentials().throwOnErrorOrAbsent()
    }

    suspend fun ensureJoinerVisibleMeshNetworksListPopulated() {
        targetDeviceVisibleMeshNetworksLD.forceSingleScan()
    }

    suspend fun ensureMeshNetworkSelected() {
        if (targetDeviceMeshNetworkToJoinLD.value != null) {
            return
        }

        flowManager.navigate(R.id.action_global_scanForMeshNetworksFragment)
        val ldSuspender = liveDataSuspender({ targetDeviceMeshNetworkToJoinLD })
        val meshNetworkToJoin = withContext(UI) {
            ldSuspender.awaitResult()
        }
    }

    suspend fun ensureTargetMeshNetworkPasswordCollected() {
        if (targetDeviceMeshNetworkToJoinCommissionerPassword.value.truthy()) {
            return
        }

        val ld = targetDeviceMeshNetworkToJoinCommissionerPassword
        val ldSuspender = liveDataSuspender({ ld })
        val password = withContext(UI) {
            flowManager.navigate(R.id.action_global_enterNetworkPasswordFragment)
            ldSuspender.awaitResult()
        }

        if (password == null) {
            throw FlowException("Error collecting mesh network password")
        }
    }

    suspend fun ensureMeshNetworkJoinedShown() {
        flowManager.navigate(R.id.action_global_joiningMeshNetworkProgressFragment)
    }

    suspend fun ensureMeshNetworkJoined() {
        // FIXME: do a real check here -- just ASK the device if it has joined the network yet!
        if (targetJoinedSuccesfully) {
            return
        }

        val joiner = targetXceiver!!
        val commish = flowManager.bleConnectionModule.commissionerTransceiverLD.value!!

        val password = targetDeviceMeshNetworkToJoinCommissionerPassword.value!!
        commish.sendAuth(password).throwOnErrorOrAbsent()
        commish.sendStartCommissioner().throwOnErrorOrAbsent()
        updateCommissionerStarted(true)

        val networkToJoin = targetDeviceMeshNetworkToJoinLD.value!!
        val prepJoinerData = joiner.sendPrepareJoiner(networkToJoin).throwOnErrorOrAbsent()
        commish.sendAddJoiner(prepJoinerData.eui64, prepJoinerData.password).throwOnErrorOrAbsent()
        updateTargetJoinedMeshNetwork(true)

        // FIXME: joining (sometimes?) fails here without a delay.  Revisit this value/try removing later?
        delay(2000)
        joiner.sendJoinNetwork().throwOnErrorOrAbsent()

        targetJoinedSuccesfully = true
    }

    suspend fun ensureCommissionerStopped() {
        val commish = flowManager.bleConnectionModule.commissionerTransceiverLD.value!!
        commish.sendStopCommissioner()
    }

    suspend fun ensureListeningStoppedForBothDevices() {
        val joiner = targetXceiver!!
        val commish = flowManager.bleConnectionModule.commissionerTransceiverLD.value!!

        commish.sendStopListeningMode()
        joiner.sendStopListeningMode()
    }

}