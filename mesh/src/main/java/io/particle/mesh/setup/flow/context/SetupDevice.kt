package io.particle.mesh.setup.flow.context

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.firmwareprotos.ctrl.Network.InterfaceEntry
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.common.logged
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.flow.Gen3ConnectivityType
import io.particle.mesh.setup.toConnectivityType
import io.particle.mesh.setup.toDeviceType
import io.particle.mesh.setup.ui.BarcodeData.CompleteBarcodeData
import mu.KotlinLogging


// TOOD: replace this with sealed classes, one for each role
enum class DeviceRole {
    SETUP_TARGET,
    COMMISSIONER
}


class SetupDevice(
    val deviceRole: DeviceRole,
    val barcode: LiveData<CompleteBarcodeData?> = MutableLiveData(),
    val transceiverLD: LiveData<ProtocolTransceiver?> = MutableLiveData(),
    // FIXME: having 2 LDs, representing the transceiver & the uninitialized transceiver isn't great
    val deviceConnectedLD: LiveData<Boolean?> = MutableLiveData(),
    val isClaimedLD: LiveData<Boolean?> = MutableLiveData()

) {

    private val log = KotlinLogging.logger {}

    var deviceId: String? by log.logged()
    var connectivityType: Gen3ConnectivityType? by log.logged()
    var deviceType: ParticleDeviceType? by log.logged()
    var hasLatestFirmware by log.logged(false)
    var shouldBeClaimed: Boolean? by log.logged()
    var currentDeviceName: String? by log.logged()
    var iccid: String? by log.logged()

    @WorkerThread
    fun updateBarcode(barcodeData: CompleteBarcodeData?, cloud: ParticleCloud) {
        log.info { "updateBarcode(): $barcodeData" }
        barcode.castAndPost(barcodeData)
        barcodeData?.let {
            deviceType = it.toDeviceType(cloud)
            connectivityType = it.toConnectivityType(cloud)
        }
    }

    fun updateDeviceTransceiver(transceiver: ProtocolTransceiver?) {
        log.info { "updateDeviceTransceiver(): $transceiver" }
        transceiverLD.castAndSetOnMainThread(transceiver)
    }

    fun updateDeviceConnectionInitialized(initialized: Boolean) {
        log.info { "updateDeviceConnectionInitialized(): $initialized" }
        deviceConnectedLD.castAndPost(initialized)
    }

    fun updateIsClaimed(isClaimed: Boolean) {
        log.info { "updateIsClaimed(): $isClaimed" }
        isClaimedLD.castAndPost(isClaimed)
    }

}
