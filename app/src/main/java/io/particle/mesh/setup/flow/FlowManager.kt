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
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.common.QATool
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
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
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import mu.KotlinLogging


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

    var targetDeviceType: MeshDeviceType = MeshDeviceType.XENON  // arbitrary default
    val bleConnectionModule = BLEConnectionModule(this, btConnectionManager, transceiverFactory)
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
        GlobalScope.launch(Dispatchers.Default, CoroutineStart.DEFAULT, null, {
            val error: Exception? = null
            for (i in 0..10) {
                try {
                    flow.runFlow()
                    return@launch
                } catch (ex: Exception) {
                    if (ex is FlowException && ex.exceptionType == ERROR_FATAL) {
                        log.info(ex) { "Hit fatal error, exiting setup: " }
                        endSetup()
                        return@launch
                    }

                    delay(1000)
                    QATool.report(ex)
                }
            }
            if (error != null) {
                withContext(UI) {
                    newDialogRequest(
                        StringDialogSpec(
                            "Setup has encountered an error and cannot " +
                                    "continue. Please exit setup and try again."
                        )
                    )
                    clearDialogResult()
                }
            }
        })
    }

    // FIXME: API/naming here is weak
    fun startMeshFlowForGateway() {

        // FIXME: set/clear some states?  WHICH?

        GlobalScope.launch(Dispatchers.Default, CoroutineStart.DEFAULT, null, {
            val error: Exception? = null
            for (i in 0..10) {
                try {
                    flow.runMeshFlowForGatewayDevice()
                    return@launch
                } catch (ex: Exception) {
                    if (ex is FlowException && ex.exceptionType == ERROR_FATAL) {
                        log.info(ex) { "Hit fatal error, exiting setup: " }
                        endSetup()
                        return@launch
                    }

                    delay(1000)
                    QATool.report(ex)
                }
            }

            if (error != null) {
                withContext(UI) {
                    newDialogRequest(
                        StringDialogSpec(
                            "Setup has encountered an error and cannot " +
                                    "continue. Please exit setup and try again."
                        )
                    )
                    clearDialogResult()
                }
            }
        })
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

        GlobalScope.launch(UI, CoroutineStart.DEFAULT, null, {
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
        })
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
            HashtagWinningFragmentArgs.Builder()
                .setCongratsMessage(message)
                .build()
                .toBundle()
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
        val resource = when (targetDeviceType) {
            MeshDeviceType.ARGON -> R.string.product_name_argon
            MeshDeviceType.BORON -> R.string.product_name_boron
            MeshDeviceType.XENON -> R.string.product_name_xenon
        }
        return everythingNeedsAContext.getString(resource)
    }
}
