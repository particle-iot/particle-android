package io.particle.mesh.setup.flow.context

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.common.logged
import io.particle.mesh.setup.flow.Clearable
import io.particle.mesh.setup.flow.modules.meshsetup.MeshNetworkToJoin
import io.particle.mesh.setup.flow.modules.meshsetup.MeshNetworkToJoin.CreateNewNetwork
import io.particle.mesh.setup.flow.modules.meshsetup.MeshNetworkToJoin.SelectedNetwork
import mu.KotlinLogging


class MeshContext : Clearable {

    private val log = KotlinLogging.logger {}

    val targetDeviceMeshNetworkToJoinLD: LiveData<MeshNetworkToJoin?> = MutableLiveData()
    val targetDeviceMeshNetworkToJoinCommissionerPassword: LiveData<String?> = MutableLiveData()
    val targetJoinedMeshNetworkLD: LiveData<Boolean?> = MutableLiveData()
    val commissionerStartedLD: LiveData<Boolean?> = MutableLiveData()
    val newNetworkNameLD: LiveData<String?> = MutableLiveData()
    val newNetworkPasswordLD: LiveData<String?> = MutableLiveData()
    val newNetworkIdLD: LiveData<String?> = MutableLiveData()
    val networkCreatedOnLocalDeviceLD: LiveData<Boolean?> = MutableLiveData()

    var showNewNetworkOptionInScanner by log.logged(false)
    var shownNetworkPasswordUi by log.logged(false)
    var targetJoinedSuccessfully by log.logged(false)
    var newNetworkCreatedSuccessfully by log.logged(false)
    var checkedForExistingNetwork by log.logged(false)

    override fun clearState() {
        val setToNulls = listOf(
            targetDeviceMeshNetworkToJoinLD,
            targetDeviceMeshNetworkToJoinCommissionerPassword,
            targetJoinedMeshNetworkLD,
            commissionerStartedLD,
            newNetworkNameLD,
            newNetworkPasswordLD,
            newNetworkIdLD,
            networkCreatedOnLocalDeviceLD
        )
        for (ld in setToNulls) {
            ld.castAndPost(null)
        }

        targetJoinedSuccessfully = false
        newNetworkCreatedSuccessfully = false
        checkedForExistingNetwork = false
        showNewNetworkOptionInScanner = false
        shownNetworkPasswordUi = false
    }

    fun updateSelectedMeshNetworkToJoin(meshNetworkToJoin: Mesh.NetworkInfo) {
        log.info { "updateSelectedMeshNetworkToJoin(): $meshNetworkToJoin" }
        targetDeviceMeshNetworkToJoinLD.castAndPost(SelectedNetwork(meshNetworkToJoin))
    }

    fun updateTargetDeviceMeshNetworkToJoinCommissionerPassword(password: String?) {
        log.info { "updateTargetDeviceMeshNetworkToJoinCommissionerPassword()" }
        targetDeviceMeshNetworkToJoinCommissionerPassword.castAndSetOnMainThread(password)
    }

    fun updateCommissionerStarted(started: Boolean) {
        log.info { "updateCommissionerStarted(): $started" }
        commissionerStartedLD.castAndPost(started)
    }

    fun updateTargetJoinedMeshNetwork(joined: Boolean) {
        log.info { "updateTargetJoinedMeshNetwork(): $joined" }
        targetJoinedMeshNetworkLD.castAndPost(joined)
    }

    fun onUserSelectedCreateNewNetwork() {
        log.info { "onUserSelectedCreateNewNetwork()" }
        targetDeviceMeshNetworkToJoinLD.castAndPost(CreateNewNetwork())
    }

    fun updateNewNetworkName(newName: String) {
        log.info { "updateNewNetworkName(): $newName" }
        newNetworkNameLD.castAndPost(newName)
    }

    fun updateNewNetworkPassword(password: String) {
        log.info { "updateNewNetworkPassword()" }
        newNetworkPasswordLD.castAndPost(password)
    }

    fun updateNewNetworkIdLD(networkId: String) {
        log.info { "updateNewNetworkIdLD()" }
        newNetworkIdLD.castAndPost(networkId)
    }

    fun updateNetworkCreatedOnLocalDeviceLD(created: Boolean) {
        log.info { "updateNetworkCreatedOnLocalDeviceLD()" }
        networkCreatedOnLocalDeviceLD.castAndSetOnMainThread(created)
    }

}