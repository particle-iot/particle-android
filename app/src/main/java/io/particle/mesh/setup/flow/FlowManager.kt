package io.particle.mesh.setup.flow

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.IdRes
import androidx.navigation.NavController
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.common.QATool
import io.particle.mesh.common.android.livedata.ClearValueOnInactiveLiveData
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
import io.particle.mesh.setup.flow.modules.bleconnection.BLEConnectionModule
import io.particle.mesh.setup.flow.modules.cloudconnection.CloudConnectionModule
import io.particle.mesh.setup.flow.modules.meshsetup.MeshSetupModule
import io.particle.mesh.setup.flow.modules.meshsetup.TargetDeviceMeshNetworksScanner
import io.particle.mesh.setup.ui.BarcodeData
import io.particle.mesh.setup.ui.DialogResult
import io.particle.mesh.setup.ui.DialogSpec
import io.particle.mesh.setup.utils.runOnMainThread
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import mu.KotlinLogging


class FlowManager(
        val targetDeviceType: ParticleDeviceType,
        cloud: ParticleCloud,
        private val navControllerRef: LiveData<NavController?>,
        val dialogRequestLD: LiveData<DialogSpec?>,
        val dialogResultLD: LiveData<DialogResult?>,
        btConnectionManager: BluetoothConnectionManager,
        transceiverFactory: ProtocolTransceiverFactory
) : Clearable {

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

    fun startFlow() {
        launch {
            for (i in 0..4) {
                try {
                    flow.runFlow()
                    return@launch
                } catch (ex: Exception) {
                    QATool.report(ex)
                }
            }
        }
    }

    fun startNewFlow() {
        TODO("IMPLEMENT THIS")
    }

    override fun clearState() {
        for (clearable in listOf(bleConnectionModule, meshSetupModule, cloudConnectionModule)) {
            clearable.clearState()
        }
    }

    fun navigate(@IdRes idRes: Int) {
        runOnMainThread { navController?.navigate(idRes) }
    }

    fun newDialogRequest(spec: DialogSpec) {
        log.debug { "newDialogRequest()" }
        (dialogRequestLD as MutableLiveData).postValue(spec)
    }

    fun updateDialogResult(dialogResult: DialogResult) {
        log.debug { "updateDialogResult(): $dialogResult" }
        (dialogResultLD as MutableLiveData).postValue(dialogResult)
    }

}
