package io.particle.mesh.setup.flow

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.IdRes
import androidx.navigation.NavController
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
import io.particle.mesh.setup.flow.modules.bleconnection.BLEConnectionModule
import io.particle.mesh.setup.flow.modules.cloudconnection.CloudConnectionModule
import io.particle.mesh.setup.flow.modules.meshsetup.MeshSetupModule
import io.particle.mesh.setup.flow.modules.meshsetup.TargetDeviceMeshNetworksScanner
import io.particle.mesh.setup.ui.BarcodeData
import io.particle.mesh.setup.utils.runOnMainThread
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import mu.KotlinLogging


class FlowManager(
        val targetDeviceType: ParticleDeviceType,
        cloud: ParticleCloud,
        private val navControllerRef: LiveData<NavController?>,
        btConnectionManager: BluetoothConnectionManager,
        transceiverFactory: ProtocolTransceiverFactory
) {

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
        flow.runFlow()
    }

    fun clearState() {

        flow.clearState()
        bleConnectionModule.clearState()
        meshSetupModule.clearState()
        cloudConnectionModule.clearState()
//        targetDeviceTransceiverLD.value?.disconnect()
//        (targetDeviceBarcodeLD as MutableLiveData).postValue(null)
//        (targetDeviceTransceiverLD as MutableLiveData).postValue(null)
//        commissionerTransceiverLD.value?.disconnect()
        // FIXME: finish implementing!
    }

    fun navigate(@IdRes idRes: Int) {
        runOnMainThread { navController?.navigate(idRes) }
    }

    fun startNewFlow() {
        TODO("IMPLEMENT THIS")
    }
}
