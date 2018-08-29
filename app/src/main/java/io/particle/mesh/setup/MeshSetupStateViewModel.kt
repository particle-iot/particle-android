package io.particle.mesh.setup

import android.app.Application
import android.arch.lifecycle.*
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.mesh.bluetooth.connecting.BTDeviceAddress
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.common.Result
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.setup.connection.ProtocolTranceiver
import io.particle.mesh.setup.connection.ProtocolTranceiverFactory
import io.particle.mesh.setup.connection.security.CryptoDelegateFactory
import io.particle.mesh.setup.ui.BarcodeData
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging


data class DeviceToBeSetUpParams(
        val deviceType: ParticleDeviceType? = null,
        val claimCode: String? = null,
        val barcodeData: BarcodeData? = null,
        val deviceId: String? = null,
        val bluetoothDeviceName: String? = null
)


data class OtherParams(  // best. naming. evar.
        val networkName: String? = null,
        val commissionerCredential: String? = null,
        val commissionerBarcode: BarcodeData? = null,
        val networkInfo: Mesh.NetworkInfo? = null,
        val joinerEui64: String? = null,
        val joinerPassword: String? = null
)


class MeshSetupStateViewModel(app: Application) : AndroidViewModel(app) {

    val meshSetupController = MeshSetupController(
            ParticleCloudSDK.getCloud(),
            ProtocolTranceiverFactory(
                    BluetoothConnectionManager(app),
                    CryptoDelegateFactory()
            )
    )

    override fun onCleared() {
        super.onCleared()
        meshSetupController.clearData()
    }

}


// FIXME: generic, poor naming
// FIXME: attach setup state to *this*
class MeshSetupController(
        private val cloud: ParticleCloud,
        private val protocolTranceiverFactory: ProtocolTranceiverFactory
) {

    var targetDevice: ProtocolTranceiver? = null
        private set
    var commissioner: ProtocolTranceiver? = null
        private set

    val deviceToBeSetUpParams: LiveData<DeviceToBeSetUpParams?>
        get() = mutableDeviceParams

    val otherParams: LiveData<OtherParams?>
        get() = mutableOtherParams

    private val mutableDeviceParams = MutableLiveData<DeviceToBeSetUpParams?>()
    private val mutableOtherParams = MutableLiveData<OtherParams?>()

    private val log = KotlinLogging.logger {}

    init {
        mutableDeviceParams.value = DeviceToBeSetUpParams()
        mutableOtherParams.value = OtherParams()
    }

    fun clearData() {
        commissioner?.disconnect()
        targetDevice?.disconnect()
        mutableDeviceParams.value = null
        mutableOtherParams.value = null
    }

    fun updateDeviceParams(params: DeviceToBeSetUpParams) {
        mutableDeviceParams.setOnMainThread(params)
    }

    fun updateOtherParams(params: OtherParams) {
        mutableOtherParams.setOnMainThread(params)
    }

    fun setJoinerBarcode(barcodeData: BarcodeData) {
        updateDeviceParams(mutableDeviceParams.value!!.copy(barcodeData = barcodeData))
    }

    fun setCommissionerBarcode(barcodeData: BarcodeData) {
        log.info { "Setting commissioner barcode: $barcodeData" }
        updateOtherParams(mutableOtherParams.value!!.copy(
                commissionerBarcode = barcodeData
        ))
    }

    fun setBTDeviceName(name: String) {
        updateDeviceParams(mutableDeviceParams.value!!.copy(
                bluetoothDeviceName = name
        ))
    }

    fun fetchClaimCode() {
        launch {
            if (deviceToBeSetUpParams.value!!.claimCode != null) {
                log.debug { "Claim code already fetched; skipping" }
                return@launch
            }
            log.info { "Fetching new claim code" }
            val claimCode = cloud.generateClaimCode().claimCode
            updateDeviceParams(deviceToBeSetUpParams.value!!.copy(claimCode = claimCode))
        }
    }

    suspend fun connectToTargetDevice(
            address: BTDeviceAddress,
            mobileSecret: String
    ): ProtocolTranceiver? {
        val device = protocolTranceiverFactory.buildProtocolTranceiver(address, "joiner", mobileSecret)
        if (device == null) {
            log.error { "Unable to connect to device!" }
            return null
        } else {
            targetDevice = device
        }

        val deviceIdReply = device.sendGetDeviceId()

        val deviceParams = mutableDeviceParams.value!!
        when (deviceIdReply) {
            is Result.Error,
            is Result.Absent -> {
                log.error { "Unable to retrieve device ID!" }
                return null
            }
            is Result.Present -> {
                updateDeviceParams(deviceParams.copy(
                        deviceId = deviceIdReply.value.id
                ))
            }
        }

        val networkInfoReply = device.sendGetNetworkInfo()
        when (networkInfoReply) {
            is Result.Present -> {
                device.sendLeaveNetwork()
            }
        }

        if (deviceParams.claimCode == null) {
            return device
        }

        val ccResult = device.sendSetClaimCode(deviceParams.claimCode)
        return when (ccResult) {
            is Result.Present -> device
            is Result.Error,
            is Result.Absent -> {
                log.error { "Unable to set claim code!" }
                null
            }
        }
    }





    suspend fun connectToCommissioner(
            address: BTDeviceAddress,
            mobileSecret: String
    ): ProtocolTranceiver? {
        val device = protocolTranceiverFactory.buildProtocolTranceiver(address, "commissioner", mobileSecret)
        if (device != null) {
            commissioner = device
        } else {
            log.error { "Unable to connect to device!" }
        }
        return device
    }
}