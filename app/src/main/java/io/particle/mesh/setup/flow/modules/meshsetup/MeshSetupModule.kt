package io.particle.mesh.setup.flow.modules.meshsetup

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.widget.Toast
import io.particle.android.sdk.tinker.TinkerApplication
import io.particle.firmwareprotos.ctrl.Common.ResultCode
import io.particle.firmwareprotos.ctrl.Common.ResultCode.NOT_FOUND
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.firmwareprotos.ctrl.mesh.Mesh.GetNetworkInfoReply
import io.particle.firmwareprotos.ctrl.mesh.Mesh.NetworkInfo
import io.particle.mesh.common.Result
import io.particle.mesh.common.android.livedata.*
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.flow.Clearable
import io.particle.mesh.setup.flow.FlowException
import io.particle.mesh.setup.flow.FlowManager
import io.particle.mesh.setup.flow.modules.meshsetup.MeshNetworkToJoin.CreateNewNetwork
import io.particle.mesh.setup.flow.modules.meshsetup.MeshNetworkToJoin.SelectedNetwork
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent
import io.particle.mesh.setup.ui.DialogResult
import io.particle.mesh.setup.ui.DialogSpec
import io.particle.mesh.setup.utils.safeToast
import io.particle.sdk.app.R
import io.particle.sdk.app.R.string
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.withContext
import kotlinx.coroutines.experimental.withTimeoutOrNull
import mu.KotlinLogging
import java.lang.IllegalStateException


class MeshSetupModule(
        private val flowManager: FlowManager,
        val targetDeviceVisibleMeshNetworksLD: TargetDeviceMeshNetworksScanner

) : Clearable {

    val targetDeviceMeshNetworkToJoinLD: LiveData<MeshNetworkToJoin?> = MutableLiveData()
    val targetDeviceMeshNetworkToJoinCommissionerPassword: LiveData<String?> = MutableLiveData()
    val targetJoinedMeshNetworkLD: LiveData<Boolean?> = MutableLiveData()
    val commissionerStartedLD: LiveData<Boolean?> = MutableLiveData()
    val newNetworkNameLD: LiveData<String?> = MutableLiveData()
    val newNetworkPasswordLD: LiveData<String?> = MutableLiveData()
    val createNetworkSent: LiveData<Boolean?> = MutableLiveData()

    var showNewNetworkOptionInScanner: Boolean = false

    private var targetJoinedSuccessfully = false
    private var newNetworkCreatedSuccessfully = false

    private val targetXceiver
        get() = flowManager.bleConnectionModule.targetDeviceTransceiverLD.value

    private val log = KotlinLogging.logger {}

    override fun clearState() {
        val setToNulls = listOf(
                targetDeviceMeshNetworkToJoinLD,
                targetDeviceMeshNetworkToJoinCommissionerPassword,
                targetJoinedMeshNetworkLD,
                commissionerStartedLD,
                newNetworkNameLD,
                newNetworkPasswordLD,
                createNetworkSent
        )
        for (ld in setToNulls) {
            ld.castAndPost(null)
        }

        targetJoinedSuccessfully = false
        newNetworkCreatedSuccessfully = false
    }

    fun updateSelectedMeshNetworkToJoin(meshNetworkToJoin: Mesh.NetworkInfo) {
        log.info { "updateSelectedMeshNetworkToJoin(): $meshNetworkToJoin" }
        (targetDeviceMeshNetworkToJoinLD as MutableLiveData).postValue(
                SelectedNetwork(meshNetworkToJoin)
        )
    }

    fun updateTargetMeshNetworkCommissionerPassword(password: String) {
        log.info { "updateTargetMeshNetworkCommissionerPassword()" }
        (targetDeviceMeshNetworkToJoinCommissionerPassword as MutableLiveData).postValue(password)
    }

    fun updateCommissionerStarted(started: Boolean) {
        log.info { "updateCommissionerStarted(): $started" }
        (commissionerStartedLD as MutableLiveData).postValue(started)
    }

    fun updateTargetJoinedMeshNetwork(joined: Boolean) {
        log.info { "updateTargetJoinedMeshNetwork(): $joined" }
        (targetJoinedMeshNetworkLD as MutableLiveData).postValue(joined)
    }

    fun onUserSelectedCreateNewNetwork() {
        log.info { "onUserSelectedCreateNewNetwork()" }
        (targetDeviceMeshNetworkToJoinLD as MutableLiveData).postValue(CreateNewNetwork())
    }

    fun updateNewNetworkName(newName: String) {
        log.info { "updateNewNetworkName(): $newName" }
        (newNetworkNameLD as MutableLiveData).postValue(newName)
    }

    fun updateNewNetworkPassword(password: String) {
        log.info { "updateNewNetworkPassword()" }
        (newNetworkPasswordLD as MutableLiveData).postValue(password)
    }

    suspend fun ensureRemovedFromExistingNetwork() {
        log.info { "ensureRemovedFromExistingNetwork()" }
        val reply: Result<GetNetworkInfoReply, ResultCode> = targetXceiver!!.sendGetNetworkInfo()
        when(reply) {
            is Result.Present -> {


                // FIXME: PROMPT USER.  Bail early if user declines to leave network

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

        targetXceiver!!.sendLeaveNetwork().throwOnErrorOrAbsent()
    }

    suspend fun ensureCommissionerIsOnNetworkToBeJoined() {
        log.info { "ensureCommissionerIsOnNetworkToBeJoined()" }
        val commissioner = flowManager.bleConnectionModule.commissionerTransceiverLD.value!!
        val reply = commissioner.sendGetNetworkInfo().throwOnErrorOrAbsent()

        val commissionerNetwork = reply.network
        val toJoin = (targetDeviceMeshNetworkToJoinLD.value!! as SelectedNetwork)

        if (commissionerNetwork?.extPanId == toJoin.networkToJoin.extPanId) {
            return  // it's a match; we're done.
        }

        commissioner.disconnect()
        flowManager.bleConnectionModule.commissionerTransceiverLD.castAndPost(null)
        flowManager.bleConnectionModule.updateCommissionerBarcode(null)

        val ldSuspender = liveDataSuspender({ flowManager.dialogResultLD })
        val result = withContext(UI) {
            flowManager.newDialogRequest(DialogSpec(
                    R.string.p_manualcommissioning_commissioner_candidate_not_on_target_network,
                    android.R.string.ok
            ))
            var result: DialogResult? = null
            while(result == null) {
                result = ldSuspender.awaitResult()
            }
        }
        log.info { "result from awaiting on 'commissioner not on network to be joined' dialog: $result" }
        flowManager.clearDialogResult()
        delay(500)

        throw FlowException("Commissioner is on the wrong network")
    }

    suspend fun ensureJoinerVisibleMeshNetworksListPopulated() {
        log.info { "ensureJoinerVisibleMeshNetworksListPopulated()" }
        targetDeviceVisibleMeshNetworksLD.forceSingleScan()
    }

    suspend fun ensureMeshNetworkSelected() {
        log.info { "ensureMeshNetworkSelected()" }
        if (targetDeviceMeshNetworkToJoinLD.value != null) {
            return
        }

        flowManager.navigate(R.id.action_global_scanForMeshNetworksFragment)
        val ldSuspender = liveDataSuspender({ targetDeviceMeshNetworkToJoinLD.nonNull() })
        val meshNetworkToJoin = withContext(UI) {
            ldSuspender.awaitResult()
        }
    }

    suspend fun ensureTargetMeshNetworkPasswordCollected() {
        log.info { "ensureTargetMeshNetworkPasswordCollected()" }
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
            throw FlowException("Error while collecting mesh network password")
        }

        // FIXME: re-introduce this
//        val commissioner = flowManager.bleConnectionModule.commissionerTransceiverLD.value!!
//        val sendAuthResult = commissioner.sendAuth(password)
//        when(sendAuthResult) {
//            is Result.Present -> return
//            is Result.Error,
//            is Result.Absent -> {
//                targetDeviceMeshNetworkToJoinCommissionerPassword.castAndSetOnMainThread(null)
//                throw FlowException("Bad commissioner password")
//            }
//        }
    }

    suspend fun ensureMeshNetworkJoinedUiShown() {
        log.info { "ensureMeshNetworkJoinedUiShown()" }
        flowManager.navigate(R.id.action_global_joiningMeshNetworkProgressFragment)
    }

    suspend fun ensureMeshNetworkJoined() {
        log.info { "ensureMeshNetworkJoined()" }
        // FIXME: do a real check here -- just ASK the device if it has joined the network yet!
        if (targetJoinedSuccessfully) {
            return
        }

        val joiner = targetXceiver!!
        val commish = flowManager.bleConnectionModule.commissionerTransceiverLD.value!!

        val password = targetDeviceMeshNetworkToJoinCommissionerPassword.value!!
        commish.sendAuth(password).throwOnErrorOrAbsent()
        commish.sendStartCommissioner().throwOnErrorOrAbsent()
        updateCommissionerStarted(true)

        val toJoinWrapper = targetDeviceMeshNetworkToJoinLD.value!!
        val networkToJoin: NetworkInfo = when(toJoinWrapper) {
            is MeshNetworkToJoin.SelectedNetwork -> toJoinWrapper.networkToJoin
            is MeshNetworkToJoin.CreateNewNetwork -> throw IllegalStateException()
        }
        val prepJoinerData = joiner.sendPrepareJoiner(networkToJoin).throwOnErrorOrAbsent()
        commish.sendAddJoiner(prepJoinerData.eui64, prepJoinerData.password).throwOnErrorOrAbsent()
        updateTargetJoinedMeshNetwork(true)

        // FIXME: joining (sometimes?) fails here without a delay.  Revisit this value/try removing later?
        delay(5000)
        val start = System.currentTimeMillis()
        // time out after *five minutes* (yep.)
        joiner.sendJoinNetwork(300000).throwOnErrorOrAbsent()
        val totalMillis = System.currentTimeMillis() - start
        TinkerApplication.appContext?.safeToast(
                "JoinNetworkReply: device took ${totalMillis/1000}s",
                duration = Toast.LENGTH_LONG
        )

        targetJoinedSuccessfully = true
    }

    suspend fun ensureCommissionerStopped() {
        log.info { "ensureCommissionerStopped()" }
        val commish = flowManager.bleConnectionModule.commissionerTransceiverLD.value!!
        commish.sendStopCommissioner()
    }

    suspend fun ensureNewNetworkName() {
        log.info { "ensureNewNetworkName()" }
        if (newNetworkNameLD.value != null) {
            return
        }

        val ldSuspender = liveDataSuspender({ newNetworkNameLD.nonNull() })
        withContext(UI) {
            flowManager.navigate(R.id.action_global_newMeshNetworkNameFragment)
            ldSuspender.awaitResult()
        }
    }

    suspend fun ensureNewNetworkPassword() {
        log.info { "ensureNewNetworkPassword()" }
        if (newNetworkPasswordLD.value != null) {
            return
        }

        val ldSuspender = liveDataSuspender({ newNetworkPasswordLD.nonNull() })
        withContext(UI) {
            flowManager.navigate(R.id.action_global_newMeshNetworkPasswordFragment)
            ldSuspender.awaitResult()
        }
    }

    suspend fun ensureCreatingNewNetworkUiShown() {
        log.info { "ensureCreatingNewNetworkUiShown()" }
        if (newNetworkCreatedSuccessfully) {
            return
        }

        flowManager.navigate(R.id.action_global_creatingMeshNetworkFragment)
    }

    suspend fun ensureCreateNewNetworkMessageSent() {
        log.info { "ensureCreateNewNetworkMessageSent()" }
        if (createNetworkSent.value.truthy()) {
            return
        }

        val name = newNetworkNameLD.value!!
        val password = newNetworkPasswordLD.value!!

        targetXceiver!!.sendCreateNetwork(name, password).throwOnErrorOrAbsent()

        (createNetworkSent as MutableLiveData).setOnMainThread(true)
    }

}