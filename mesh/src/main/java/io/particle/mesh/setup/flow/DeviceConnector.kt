package io.particle.mesh.setup.flow

import androidx.annotation.MainThread
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SERIES
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BORON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B_SERIES
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.XENON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.X_SERIES
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.setup.SerialNumber
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
import io.particle.mesh.setup.toDeviceType
import io.particle.mesh.setup.ui.BarcodeData.CompleteBarcodeData


class DeviceConnector(
    private val cloud: ParticleCloud,
    private val btConnectionManager: BluetoothConnectionManager,
    private val transceiverFactory: ProtocolTransceiverFactory
) {

    @MainThread
    suspend fun connect(
        barcode: CompleteBarcodeData,
        connName: String,
        scopes: Scopes
    ): ProtocolTransceiver? {
        val broadcastName = getDeviceBroadcastName(
            barcode.serialNumber.toDeviceType(cloud),
            barcode.serialNumber
        )
        val device = btConnectionManager.connectToDevice(broadcastName, scopes)
            ?: return null
        return transceiverFactory.buildProtocolTransceiver(
            device,
            connName,
            scopes,
            barcode.mobileSecret
        )
    }


    private fun getDeviceBroadcastName(
        deviceType: ParticleDeviceType,
        serialNum: SerialNumber
    ): String {
        val deviceTypeName = when (deviceType) {
            ARGON,
            A_SERIES -> "Argon"
            BORON,
            B_SERIES -> "Boron"
            XENON,
            X_SERIES -> "Xenon"
            else -> throw IllegalArgumentException("Not a mesh device: $deviceType")
        }
        val serial = serialNum.value
        val lastSix = serial.substring(serial.length - BT_NAME_ID_LENGTH).toUpperCase()

        return "$deviceTypeName-$lastSix"
    }


}

private const val BT_NAME_ID_LENGTH = 6
