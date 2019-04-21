package io.particle.mesh.setup.flow.modules.bleconnection

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.setup.BarcodeData.CompleteBarcodeData
import io.particle.mesh.setup.SerialNumber
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.toConnectivityType
import io.particle.mesh.setup.toDeviceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KotlinLogging


class BLEConnectionModule(
    private val flowManager: FlowManager,
    private val btConnectionManager: BluetoothConnectionManager,
    private val transceiverFactory: ProtocolTransceiverFactory,
    private val particleCloud: ParticleCloud
) : Clearable {

    private val log = KotlinLogging.logger {}

    val targetDeviceBarcodeLD: LiveData<CompleteBarcodeData?> = MutableLiveData()
    val targetDeviceTransceiverLD: LiveData<ProtocolTransceiver?> = MutableLiveData()
    // FIXME: having 2 LDs, representing the transceiver & the uninitialized transceiver isn't great
    val targetDeviceConnectedLD: LiveData<Boolean?> = MutableLiveData()

    var targetDeviceId: String? = null
    val commissionerBarcodeLD: LiveData<CompleteBarcodeData?> = MutableLiveData()
    val commissionerTransceiverLD: LiveData<ProtocolTransceiver?> = MutableLiveData()
    val commissionerDeviceConnectedLD: LiveData<Boolean?> = MutableLiveData()
    val getReadyNextButtonClickedLD: LiveData<Boolean?> = MutableLiveData()

    private var connectingToTargetUiShown = false
    private var connectingToAssistingDeviceUiShown = false
    private var shownTargetInitialIsConnectedScreen = false
    private var shownAssistingDeviceInitialIsConnectedScreen = false

    private val targetXceiver
        get() = targetDeviceTransceiverLD.value


    override fun clearState() {
        targetDeviceId = null
        connectingToTargetUiShown = false
        connectingToAssistingDeviceUiShown = false
        shownTargetInitialIsConnectedScreen = false
        shownAssistingDeviceInitialIsConnectedScreen = false

        targetDeviceTransceiverLD.value?.disconnect()
        commissionerTransceiverLD.value?.disconnect()

        val setToNulls = listOf(
            targetDeviceBarcodeLD,
            targetDeviceTransceiverLD,
            targetDeviceConnectedLD,
            commissionerBarcodeLD,
            commissionerTransceiverLD,
            getReadyNextButtonClickedLD
        )
        for (ld in setToNulls) {
            ld.castAndPost(null)
        }
    }

    fun updateCommissionerBarcode(barcodeData: CompleteBarcodeData?) {
        log.info { "updateCommissionerBarcode()" }
        (commissionerBarcodeLD as MutableLiveData).postValue(barcodeData)
    }

    fun updateTargetDeviceBarcode(barcodeData: CompleteBarcodeData?) {
        log.info { "updateTargetDeviceBarcode(): barcodeData=$barcodeData" }
        (targetDeviceBarcodeLD as MutableLiveData).postValue(barcodeData)
    }

    fun updateTargetDeviceConnectionInitialized(initialized: Boolean) {
        log.info { "updateTargetDeviceConnectionInitialized(): $initialized" }
        targetDeviceConnectedLD.castAndPost(initialized)
    }

    fun updateAssistingDeviceConnectionInitialized(initialized: Boolean) {
        log.info { "updateAssistingDeviceConnectionInitialized(): $initialized" }
        commissionerDeviceConnectedLD.castAndPost(initialized)
    }

    fun updateGetReadyNextButtonClicked(clicked: Boolean) {
        log.info { "updateGetReadyNextButtonClicked(): $clicked" }
        getReadyNextButtonClickedLD.castAndPost(clicked)
    }


    suspend fun ensureBarcodeDataForTargetDevice() {
        log.info { "ensureBarcodeDataForTargetDevice()" }
        if (targetDeviceBarcodeLD.value != null) {
            return
        }

//        val userSpecifiedDeviceType = flowManager.targetDeviceType
        val liveDataSuspender = liveDataSuspender({ targetDeviceBarcodeLD.nonNull() })
        val barcodeData = withContext(Dispatchers.Main) {
//            flowManager.navigate(R.id.action_global_scanJoinerCodeIntroFragment)
            liveDataSuspender.awaitResult()
        }

        if (barcodeData == null) {
            throw FlowException("Error getting barcode data for target device")
        }

        flowManager.targetPlatformDeviceType = barcodeData.toDeviceType(particleCloud)
        flowManager.targetDeviceType =  barcodeData.toConnectivityType(particleCloud)

        if (getReadyNextButtonClickedLD.value != true) {
            val liveDataSuspender2 = liveDataSuspender({ getReadyNextButtonClickedLD.nonNull() })
            withContext(Dispatchers.Main) {
//                flowManager.navigate(R.id.action_global_getReadyForSetupFragment)
                liveDataSuspender2.awaitResult()
            }
        }
    }

    suspend fun ensureTargetDeviceConnected() {
        log.info { "ensureTargetDeviceConnected()" }
        if (targetXceiver != null && targetXceiver!!.isConnected) {
            return
        }

        if (!connectingToTargetUiShown) {
//            flowManager.navigate(R.id.action_global_BLEPairingProgressFragment)
            connectingToTargetUiShown = true
        }


        // FIXME: consider what states we should be resetting here


        val ldSuspender = liveDataSuspender({ targetDeviceTransceiverLD })
        val transceiver = withContext(Dispatchers.Main) {
            connectTargetDevice()
            ldSuspender.awaitResult()
        }

        if (transceiver == null) {
            throw FlowException("Error ensuring target connected")
        }
    }

    suspend fun ensureTargetDeviceId(): String {
        log.info { "ensureTargetDeviceId()" }
        if (targetDeviceId != null) {
            return targetDeviceId!!
        }

        // get device ID
        val deviceIdReply = targetXceiver!!.sendGetDeviceId().throwOnErrorOrAbsent()
        targetDeviceId = deviceIdReply.id
        return deviceIdReply.id
    }

    suspend fun ensureShowTargetPairingSuccessful() {
        log.info { "ensureShowTargetPairingSuccessful()" }
        if (shownTargetInitialIsConnectedScreen) {
            return
        }
        shownTargetInitialIsConnectedScreen = true

        // FIXME: i18n this!
        flowManager.showCongratsScreen(
            "Successfully paired with " +
                    targetDeviceTransceiverLD.value?.bleBroadcastName
        )

        delay(2000)
    }

    suspend fun ensureShowAssistingDevicePairingSuccessful() {

        // FIXME: look into why this isn't being used...

        log.info { "ensureShowAssistingDevicePairingSuccessful()" }
        if (shownAssistingDeviceInitialIsConnectedScreen) {
            return
        }
        shownAssistingDeviceInitialIsConnectedScreen = true

        flowManager.showCongratsScreen(
            "Successfully paired with " +
                    commissionerTransceiverLD.value?.bleBroadcastName
        )

        delay(2000)
    }

    suspend fun ensureBarcodeDataForComissioner() {
        log.info { "ensureBarcodeDataForComissioner()" }
        if (commissionerBarcodeLD.value != null) {
            return
        }

        log.debug { "No commissioner barcode found; showing UI" }
        val liveDataSuspender = liveDataSuspender({ commissionerBarcodeLD.nonNull() })
        val barcodeData = withContext(Dispatchers.Main) {
//            flowManager.navigate(R.id.action_global_manualCommissioningAddToNetworkFragment)
            liveDataSuspender.awaitResult()
        }

        if (barcodeData == null) {
            throw FlowException("Error getting barcode data for commissioner")
        }
    }

    suspend fun ensureCommissionerConnected() {
        log.info { "ensureCommissionerConnected()" }
        var commissioner = commissionerTransceiverLD.value
        if (commissioner != null && commissioner.isConnected) {
            return
        }


        if (!connectingToAssistingDeviceUiShown) {
//            flowManager.navigate(R.id.action_global_assistingDevicePairingProgressFragment)
            connectingToAssistingDeviceUiShown = true
        }


        // FIXME: consider what states we should be resetting here


        val xceiverSuspender = liveDataSuspender({ commissionerTransceiverLD })
        commissioner = withContext(Dispatchers.Main) {
            connectCommissioner()
            xceiverSuspender.awaitResult()
        }

        flowManager.showCongratsScreen(
            "Successfully paired with " +
                    commissionerTransceiverLD.value?.bleBroadcastName
        )

        delay(2000)

        if (commissioner == null) {
            throw FlowException("Error ensuring commissioner connected")
        }
    }

    suspend fun ensureListeningStoppedForBothDevices() {
        log.info { "ensureListeningStoppedForBothDevices()" }
        targetXceiver?.sendStopListeningMode()
        flowManager.bleConnectionModule.commissionerTransceiverLD.value?.sendStopListeningMode()
    }


    @MainThread
    private suspend fun connectTargetDevice() {
        log.info { "connectTargetDevice()" }
        val barcode = targetDeviceBarcodeLD.value!!
        val targetTransceiver = connect(barcode, "target")

        if (targetTransceiver == null) {
            throw FlowException("Error connecting target")
        }

        log.debug { "Target device connected!" }
        targetDeviceTransceiverLD.castAndSetOnMainThread(targetTransceiver)
    }

    @MainThread
    private suspend fun connectCommissioner() {
        log.info { "connectCommissioner()" }
        val barcode = commissionerBarcodeLD.value!!
        val commissioner = connect(barcode, "commissioner")

        if (commissioner == null) {
            throw FlowException("Error connecting commissioner")
        }

        log.debug { "Commissioner connected!" }
        commissionerTransceiverLD.castAndSetOnMainThread(commissioner)
    }

    @MainThread
    private suspend fun connect(
        barcode: CompleteBarcodeData,
        connName: String
    ): ProtocolTransceiver? {
        val broadcastName = getDeviceBroadcastName(
            barcode.serialNumber.toDeviceType(particleCloud),
            barcode.serialNumber
        )
        val device = btConnectionManager.connectToDevice(broadcastName, Scopes())
            ?: return null
        return transceiverFactory.buildProtocolTransceiver(device, connName, Scopes(), barcode.mobileSecret)
    }

    private fun getDeviceBroadcastName(deviceType: ParticleDeviceType, serialNum: SerialNumber): String {
        val deviceTypeName = when(deviceType) {
            ARGON,
            A_SERIES -> "Argon"
            BORON,
            B_SERIES -> "Boron"
            XENON,
            X_SERIES -> "Xenon"
            else -> throw IllegalArgumentException("Not a mesh device: $this")
        }
        val serial = serialNum.value
        val lastSix = serial.substring(serial.length - BT_NAME_ID_LENGTH).toUpperCase()

        return "$deviceTypeName-$lastSix"
    }

}

private const val BT_NAME_ID_LENGTH = 6
