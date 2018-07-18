package io.particle.particlemesh.meshsetup

import android.app.Application
import android.arch.lifecycle.*
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.particlemesh.bluetooth.connecting.BTDeviceAddress
import io.particle.particlemesh.bluetooth.connecting.MeshSetupConnectionFactory
import io.particle.particlemesh.common.Result
import io.particle.particlemesh.common.android.livedata.setOnMainThread
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging


data class DeviceToBeSetUpParams(
        val deviceType: ParticleDeviceType? = null,
        val claimCode: String? = null,
        val serialNumber: String? = null,
        val deviceId: String? = null,
        val mobileSecret: String? = null,
        val bluetoothDeviceName: String? = null
)


data class OtherParams(  // best. naming. evar.
        val networkName: String? = null,
        val commissionerCredential: String? = null,
        val networkInfo: Mesh.NetworkInfo? = null,
        val joinerEui64: String? = null,
        val joinerPassword: String? = null
)


class MeshSetupStateViewModel(app: Application) : AndroidViewModel(app) {

    val meshSetupController = MeshSetupController(
            ParticleCloudSDK.getCloud(),
            RequestSenderFactory(MeshSetupConnectionFactory(app))
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
        private val requestSenderFactory: RequestSenderFactory
) {

    var targetDevice: RequestSender? = null
        private set
    var commissioner: RequestSender? = null
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

    fun setTargetSerialNumber(serialNumber: String, mobileSecret: String) {
        updateDeviceParams(mutableDeviceParams.value!!.copy(
                serialNumber = serialNumber,
                mobileSecret = mobileSecret
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

    suspend fun connectToTargetDevice(address: BTDeviceAddress): RequestSender? {
        val device = requestSenderFactory.buildRequestSender(address, "joiner")
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

    suspend fun connectToCommissioner(address: BTDeviceAddress): RequestSender? {
        val device = requestSenderFactory.buildRequestSender(address, "commissioner")
        if (device != null) {
            commissioner = device
        } else {
            log.error { "Unable to connect to device!" }
        }
        return device
    }
}