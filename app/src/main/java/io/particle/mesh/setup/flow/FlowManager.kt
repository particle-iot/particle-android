package io.particle.mesh.setup.flow

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.annotation.IdRes
import androidx.navigation.NavController
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.common.QATool
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
import io.particle.mesh.setup.flow.modules.bleconnection.BLEConnectionModule
import io.particle.mesh.setup.flow.modules.cloudconnection.CloudConnectionModule
import io.particle.mesh.setup.flow.modules.meshsetup.MeshSetupModule
import io.particle.mesh.setup.flow.modules.meshsetup.TargetDeviceMeshNetworksScanner
import io.particle.mesh.setup.ui.BarcodeData
import io.particle.mesh.setup.ui.DialogResult
import io.particle.mesh.setup.ui.DialogSpec
import io.particle.mesh.setup.ui.DialogSpec.StringDialogSpec
import io.particle.mesh.setup.ui.ProgressHack
import io.particle.mesh.setup.utils.runOnMainThread
import io.particle.mesh.setup.utils.safeToast
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import mu.KotlinLogging


class FlowManager(
        var targetDeviceType: ParticleDeviceType,
        cloud: ParticleCloud,
        private val navControllerRef: LiveData<NavController?>,
        private val dialogRequestLD: LiveData<DialogSpec?>,
        val dialogResultLD: LiveData<DialogResult?>,
        btConnectionManager: BluetoothConnectionManager,
        transceiverFactory: ProtocolTransceiverFactory,
        val everythingNeedsAContext: Application,
        private val progressHackLD: MutableLiveData<ProgressHack?>
) : Clearable, ProgressHack {

    val bleConnectionModule = BLEConnectionModule(this, btConnectionManager, transceiverFactory)
    val meshSetupModule: MeshSetupModule
    val cloudConnectionModule: CloudConnectionModule

    private var flow: Flow

    private val navController: NavController?
        get() = navControllerRef.value

    private val log = KotlinLogging.logger {}

    init {
        val scanner = TargetDeviceMeshNetworksScanner(bleConnectionModule.targetDeviceTransceiverLD)
        meshSetupModule = MeshSetupModule(this, scanner)
        cloudConnectionModule = CloudConnectionModule(this, cloud)
        flow = Flow(this, bleConnectionModule, meshSetupModule, cloudConnectionModule)
    }

    fun startNewFlow() {
        // FIXME: call "clearState()" here?  Probably?
        launch {
            val error: Exception? = null
            for (i in 0..10) {
                try {
                    flow.runFlow()
                    return@launch
                } catch (ex: Exception) {
                    everythingNeedsAContext.safeToast("Error: " + ex.message.toString(), Toast.LENGTH_LONG)
                    delay(1000)
                    QATool.report(ex)
                }
            }
            if (error != null) {
                withContext(UI) {
                    newDialogRequest(StringDialogSpec("Setup has encountered an error and cannot " +
                            "continue. Please exit setup and try again."))
                    clearDialogResult()
                }
            }
        }
    }

    // FIXME: API/naming here is weak
    fun startMeshFlowForGateway() {

        // FIXME: set/clear some states?  WHICH?

        launch {
            val error: Exception? = null
            for (i in 0..10) {
                try {
                    flow.runMeshFlowForGatewayDevice()
                    return@launch
                } catch (ex: Exception) {
                    everythingNeedsAContext.safeToast("Error: " + ex.message.toString(), Toast.LENGTH_LONG)
                    delay(1000)
                    QATool.report(ex)
                }
            }
            if (error != null) {
                withContext(UI) {
                    newDialogRequest(StringDialogSpec("Setup has encountered an error and cannot " +
                            "continue. Please exit setup and try again."))
                    clearDialogResult()
                }
            }
        }
    }

    fun startNewFlowWithCommissioner() {
        clearAndRetainCommissioner()
        navController?.navigate(R.id.action_global_selectDeviceFragment)
    }

    private fun clearAndRetainCommissioner() {
        log.info { "clearAndRetainCommissioner()" }


        bleConnectionModule.commissionerBarcodeLD.observeForever {
            log.info { "New commissioner barcode value set: $it" }
        }

        var commissionerBarcodeToUse: BarcodeData?
        var commissionerTransceiverToUse: ProtocolTransceiver?
        if (bleConnectionModule.commissionerTransceiverLD.value != null) {
            log.info { "Retaining original commissioner" }
            commissionerBarcodeToUse = bleConnectionModule.commissionerBarcodeLD.value
            commissionerTransceiverToUse = bleConnectionModule.commissionerTransceiverLD.value
            // set this to null to avoid disconnect being called on the device which
            // will become our commissioner
            bleConnectionModule.commissionerTransceiverLD.castAndSetOnMainThread(null)
        } else {
            log.info { "Using previous target device as new commissioner" }
            commissionerBarcodeToUse = bleConnectionModule.targetDeviceBarcodeLD.value
            commissionerTransceiverToUse = bleConnectionModule.targetDeviceTransceiverLD.value
            // set this to null to avoid disconnect being called on the device which
            // will become our commissioner
            bleConnectionModule.targetDeviceTransceiverLD.castAndSetOnMainThread(null)
        }

        var commissionerPwd = meshSetupModule.targetDeviceMeshNetworkToJoinCommissionerPassword.value
        if (commissionerPwd == null) {  // and if it's still null...
            commissionerPwd = meshSetupModule.newNetworkPasswordLD.value
        }

        clearState()

        launch(UI) {
            delay(100)
            bleConnectionModule.commissionerBarcodeLD.castAndSetOnMainThread(commissionerBarcodeToUse)
            bleConnectionModule.commissionerTransceiverLD.castAndSetOnMainThread(commissionerTransceiverToUse)
            meshSetupModule.targetDeviceMeshNetworkToJoinCommissionerPassword.castAndSetOnMainThread(commissionerPwd)
        }
    }

    override fun clearState() {
        log.info { "clearState()" }
        for (clearable in listOf(bleConnectionModule, meshSetupModule, cloudConnectionModule)) {
            clearable.clearState()
        }
    }

    override fun showGlobalProgressSpinner(show: Boolean) {
        progressHackLD.value?.showGlobalProgressSpinner(show)
    }

    fun navigate(@IdRes idRes: Int) {
        runOnMainThread {
            navController?.popBackStack()
            navController?.navigate(idRes)
        }
    }

    fun newDialogRequest(spec: DialogSpec) {
        log.debug { "newDialogRequest(): $spec" }
        (dialogRequestLD as MutableLiveData).postValue(spec)
    }

    fun updateDialogResult(dialogResult: DialogResult) {
        log.debug { "updateDialogResult(): $dialogResult" }
        (dialogResultLD as MutableLiveData).postValue(dialogResult)
    }

    fun clearDialogResult() {
        (dialogResultLD as MutableLiveData).postValue(null)
    }

    fun clearDialogRequest() {
        (dialogRequestLD as MutableLiveData).postValue(null)
    }
}
