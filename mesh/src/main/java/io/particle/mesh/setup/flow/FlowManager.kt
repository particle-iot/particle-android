package io.particle.mesh.setup.flow

import android.content.Context
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import com.squareup.okhttp.OkHttpClient
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SERIES
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BORON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B_SERIES
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.XENON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.X_SERIES
import io.particle.mesh.R
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.common.QATool
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.ota.FirmwareUpdateManager
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
import io.particle.mesh.setup.flow.ExceptionType.ERROR_FATAL
import io.particle.mesh.setup.flow.modules.bleconnection.BLEConnectionModule
import io.particle.mesh.setup.flow.modules.cloudconnection.CloudConnectionModule
import io.particle.mesh.setup.flow.modules.cloudconnection.WifiNetworksScannerLD
import io.particle.mesh.setup.flow.modules.device.DeviceModule
import io.particle.mesh.setup.flow.modules.meshsetup.MeshSetupModule
import io.particle.mesh.setup.flow.modules.meshsetup.TargetDeviceMeshNetworksScanner
import io.particle.mesh.setup.ui.*
import io.particle.mesh.setup.ui.DialogSpec.StringDialogSpec
import io.particle.mesh.setup.utils.runOnMainThread
import kotlinx.coroutines.*
import mu.KotlinLogging


private const val FLOW_RETRIES = 10


class FlowManager(
    cloud: ParticleCloud,
    private val navControllerRef: LiveData<NavController?>,
    private val dialogRequestLD: LiveData<DialogSpec?>,
    val dialogResultLD: LiveData<DialogResult?>,
    btConnectionManager: BluetoothConnectionManager,
    transceiverFactory: ProtocolTransceiverFactory,
    private val progressHackLD: MutableLiveData<ProgressHack?>,
    private val terminatorLD: MutableLiveData<MeshFlowTerminator?>,
    private val everythingNeedsAContext: Context
) : Clearable, ProgressHack {

    var targetDeviceType: Gen3ConnectivityType = Gen3ConnectivityType.MESH_ONLY  // arbitrary default
    var targetPlatformDeviceType: ParticleDeviceType = ParticleDeviceType.XENON  // arbitrary default

    val bleConnectionModule =
        BLEConnectionModule(this, btConnectionManager, transceiverFactory, cloud)
    val meshSetupModule: MeshSetupModule
    val cloudConnectionModule: CloudConnectionModule
    val deviceModule: DeviceModule

    private var flow: Flow

    private val navController: NavController?
        get() = navControllerRef.value

    private val log = KotlinLogging.logger {}

    init {
        meshSetupModule = MeshSetupModule(
            this,
            cloud,
            TargetDeviceMeshNetworksScanner(bleConnectionModule.targetDeviceTransceiverLD)
        )

        cloudConnectionModule = CloudConnectionModule(
            WifiNetworksScannerLD(bleConnectionModule.targetDeviceTransceiverLD),
            this,
            cloud
        )

        deviceModule = DeviceModule(this, FirmwareUpdateManager(cloud, OkHttpClient()))

        flow = Flow(
            this,
            bleConnectionModule,
            meshSetupModule,
            cloudConnectionModule,
            deviceModule,
            cloud
        )
    }

    fun startNewFlow() {
        // FIXME: call "clearState()" here?  Probably?
        executeFlow { flow.runFlow() }
    }

    // FIXME: API/naming here is weak
    fun startMeshFlowForGateway() {
        executeFlow { flow.runMeshFlowForGatewayDevice() }
    }

    private fun executeFlow(flowFunction: suspend () -> Unit) {
        GlobalScope.launch(Dispatchers.Default) {
            var error: Exception? = null
            for (i in 0..FLOW_RETRIES) {
                try {
                    flowFunction()
                    return@launch
                } catch (ex: Exception) {
                    if (ex is FlowException && ex.exceptionType == ERROR_FATAL) {
                        log.info(ex) { "Hit fatal error, exiting setup: " }
                        QATool.log(ex.message ?: "")
                        endSetup()
                        return@launch
                    }
                    delay(1000)
                    QATool.report(ex)
                    error = ex
                }
            }
            quitSetupfromError()
        }
    }

    private suspend fun quitSetupfromError() {
        withContext(Dispatchers.Main) {
            newDialogRequest(
                StringDialogSpec(
                    "Setup has encountered an error and cannot " +
                            "continue. Please exit setup and try again."
                )
            )
            clearDialogResult()
            withContext(Dispatchers.Main) {
                liveDataSuspender({ dialogResultLD.nonNull() }).awaitResult()
            }
            endSetup()
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

        var commissionerPwd =
            meshSetupModule.targetDeviceMeshNetworkToJoinCommissionerPassword.value
        if (commissionerPwd == null) {  // and if it's still null...
            commissionerPwd = meshSetupModule.newNetworkPasswordLD.value
        }

        clearState()

        GlobalScope.launch(Dispatchers.Main) {
            delay(100)
            bleConnectionModule.commissionerBarcodeLD.castAndSetOnMainThread(
                commissionerBarcodeToUse
            )
            bleConnectionModule.commissionerTransceiverLD.castAndSetOnMainThread(
                commissionerTransceiverToUse
            )
            meshSetupModule.targetDeviceMeshNetworkToJoinCommissionerPassword.castAndSetOnMainThread(
                commissionerPwd
            )
        }
    }

    override fun clearState() {
        log.info { "clearState()" }
        for (clearable in listOf(
            bleConnectionModule,
            meshSetupModule,
            cloudConnectionModule,
            deviceModule
        )) {
            clearable.clearState()
        }
    }

    override fun showGlobalProgressSpinner(show: Boolean) {
        progressHackLD.value?.showGlobalProgressSpinner(show)
    }

    fun navigate(@IdRes idRes: Int, args: Bundle? = null) {
        showGlobalProgressSpinner(false)
        runOnMainThread {
            navController?.popBackStack()
            if (args == null) {
                navController?.navigate(idRes)
            } else {
                navController?.navigate(idRes, args)
            }
        }
    }

    fun showCongratsScreen(message: String) {
        navigate(
            R.id.action_global_hashtagWinningFragment,
            HashtagWinningFragmentArgs(message).toBundle()
        )
    }

    fun endSetup() {
        terminatorLD.value?.terminateSetup()
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

    fun getString(@StringRes stringRes: Int): String {
        return everythingNeedsAContext.getString(stringRes)
    }

    fun getString(@StringRes stringRes: Int, vararg formatArgs: String): String {
        return everythingNeedsAContext.getString(stringRes, formatArgs)
    }

    fun getTypeName(): String {
        val resource = when (targetPlatformDeviceType) {
            ARGON -> R.string.product_name_argon
            BORON -> R.string.product_name_boron
            XENON -> R.string.product_name_xenon
            A_SERIES -> R.string.product_name_a_series
            B_SERIES -> R.string.product_name_b_series
            X_SERIES -> R.string.product_name_x_series
            else -> throw IllegalArgumentException("Not a mesh device: $targetPlatformDeviceType")
        }
        return everythingNeedsAContext.getString(resource)
    }
}
