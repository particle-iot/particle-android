package io.particle.mesh.setup.flow

import androidx.annotation.MainThread
import androidx.collection.ArrayMap
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BORON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.XENON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.X_SOM
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.setup.BarcodeData.CompleteBarcodeData
import io.particle.mesh.setup.SerialNumber
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
import io.particle.mesh.setup.toDeviceType
import mu.KotlinLogging


class DeviceConnector(
    private val cloud: ParticleCloud,
    private val btConnectionManager: BluetoothConnectionManager,
    private val transceiverFactory: ProtocolTransceiverFactory
) {

    private val log = KotlinLogging.logger {}

    private val cache: MutableMap<CompleteBarcodeData, ProtocolTransceiver> = ArrayMap()
    private val cacheLock = Any()

    fun clearCache() {
        Scopes().let {
            it.onMain {
                doClearCache(it)
            }
        }
    }

    private suspend fun doClearCache(scopes: Scopes) {
        val devicesToDisconnect = synchronized(cacheLock) {
            val deviceList = cache.values.toList()
            cache.clear()
            return@synchronized deviceList
        }
        log.info { "doClearCache(): clearing cache for ${devicesToDisconnect.size} devices" }
        for (device in devicesToDisconnect) {
            scopes.withWorker {
                try {
                    log.info { "Asking device ${device.bleBroadcastName} to stop listening mode" }
                    device.sendStopListeningMode()
                } catch (ex: Exception) {
                    // no-op
                } finally {
                    log.info { "Asking device ${device.bleBroadcastName} to disconnect" }
                    device.disconnect()
                }
            }
        }
    }

    @MainThread
    fun getCachedDevice(
        barcode: CompleteBarcodeData,
        connName: String,
        setupContextScopes: Scopes
    ): ProtocolTransceiver? {
        val xceiver: ProtocolTransceiver? = cache[barcode]
        return xceiver?.let {
            if (it.isConnected) {
                log.info { "Found connected device in cache, returning it" }
                it.connectionName = connName
                it.messageSendingScopes = setupContextScopes
                it
            } else {
                log.info { "Found disconnected device in cache, purging it from cache" }
                it.disconnect()
                cache.remove(barcode)
                null
            }
        }
    }

    @MainThread
    suspend fun connect(
        barcode: CompleteBarcodeData,
        connName: String,
        scopes: Scopes
    ): ProtocolTransceiver? {

        log.info { "Checking cache for device with barcode=$barcode" }
        var xceiver = getCachedDevice(barcode, connName, scopes)
        xceiver?.let { return it }

        log.info { "Connected device NOT found in cache, continuing to connect" }

        log.info { "Getting device type for barcode $barcode" }
        val deviceType = scopes.withWorker {
            barcode.serialNumber.toDeviceType(cloud)
        }

        val broadcastName = getDeviceBroadcastName(deviceType, barcode.serialNumber)
        log.info { "Using broadcast name $broadcastName" }

        val (device, deviceScopes) = btConnectionManager.connectToDevice(broadcastName, scopes)
            ?: return null

        xceiver = transceiverFactory.buildProtocolTransceiver(
            device,
            connName,
            deviceScopes,
            barcode.mobileSecret,
            scopes
        )

        xceiver?.let { cache[barcode] = xceiver }

        return xceiver
    }


    private fun getDeviceBroadcastName(
        deviceType: ParticleDeviceType,
        serialNum: SerialNumber
    ): String {
        val deviceTypeName = when (deviceType) {
            ARGON,
            A_SOM -> "Argon"
            BORON,
            B_SOM -> "Boron"
            XENON,
            X_SOM -> "Xenon"
            else -> throw IllegalArgumentException("Not a mesh device: $deviceType")
        }
        val serial = serialNum.value
        val lastSix = serial.substring(serial.length - BT_NAME_ID_LENGTH).toUpperCase()

        return "$deviceTypeName-$lastSix"
    }


}

private const val BT_NAME_ID_LENGTH = 6
