package io.particle.mesh.bluetooth.connecting

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import io.particle.mesh.bluetooth.BLELiveDataCallbacks
import io.particle.mesh.bluetooth.BTCharacteristicWriter
import io.particle.mesh.bluetooth.btAdapter
import io.particle.mesh.bluetooth.connecting.ConnectionState.DISCONNECTED
import io.particle.mesh.common.QATool
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.connection.BT_SETUP_RX_CHARACTERISTIC_ID
import io.particle.mesh.setup.connection.BT_SETUP_SERVICE_ID
import io.particle.mesh.setup.connection.BT_SETUP_TX_CHARACTERISTIC_ID
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.ui.utils.buildMatchingDeviceNameSuspender
import io.particle.mesh.setup.utils.checkIsThisTheMainThread
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import mu.KotlinLogging


enum class ConnectionPriority(val sdkVal: Int) {
    HIGH(1),
    BALANCED(0),
    LOW_POWER(2)
}


class BluetoothConnection(
        private val connectionStateChangedLD: LiveData<ConnectionState?>,
        private val gatt: BluetoothGatt,
        private val callbacks: BLELiveDataCallbacks,
        // this channel receives arbitrary-length arrays (not limited to BLE MTU)
        val packetSendChannel: SendChannel<ByteArray>,
        // this channel emits arbitrary-length arrays (not limited to BLE MTU)
        private val closablePacketReceiveChannel: Channel<ByteArray>
) {

    init {
        connectionStateChangedLD.observeForever {
            if (it == DISCONNECTED) {
                disconnect(false)
            }
        }
    }

    val deviceName: String
        get() = gatt.device.name

    val isConnected: Boolean
        get() = connectionStateChangedLD.value == ConnectionState.CONNECTED

    val packetReceiveChannel: ReceiveChannel<ByteArray>
        get() = closablePacketReceiveChannel

    fun setConnectionPriority(priority: ConnectionPriority) {
        gatt.requestConnectionPriority(priority.sdkVal)
    }

    fun disconnect(closeGatt: Boolean = true) {
        QATool.runSafely(
                { packetSendChannel.close() },
                { closablePacketReceiveChannel.close() },
                { callbacks.closeChannel() },
                { gatt.disconnect() }
        )

        if (!closeGatt) {
            return
        }
        // calling .close() *immediately* after .disconnect() was sometimes causing
        // the disconnect to fail, thus the delay.  Hacky, but it works. :-/
        GlobalScope.launch(Dispatchers.Main) {
            delay(100)
            QATool.runSafely({ gatt.close() })
        }
    }
}


typealias BTDeviceAddress = String

const val CONNECTION_TIMEOUT_MILLIS = 10000L


class BluetoothConnectionManager(private val ctx: Context) {

    private val log = KotlinLogging.logger {}


    @MainThread
    suspend fun connectToDevice(
        deviceName: String,
        scopes: Scopes,
        timeout: Long = CONNECTION_TIMEOUT_MILLIS
    ): BluetoothConnection? {
        checkIsThisTheMainThread()

        val address = scanForDevice(deviceName, timeout) ?: return null

        log.info { "Connecting to device $address" }
        // 1. Attempt to connect
        val device = ctx.btAdapter.getRemoteDevice(address)
        // If this returns null, we're finished, return null ourselves
        val (gatt, bleWriteChannel, callbacks) = doConnectToDevice(device) ?: return null


        val messageWriteChannel = Channel<ByteArray>(Channel.UNLIMITED)
        scopes.backgroundScope.launch {
            for (packet in messageWriteChannel) {
                QATool.runSafely({ bleWriteChannel.writeToCharacteristic(packet) })
            }
        }

        val conn = BluetoothConnection(
                callbacks.connectionStateChangedLD,
                gatt,
                callbacks,
                messageWriteChannel,
                callbacks.readOrChangedReceiveChannel as Channel<ByteArray>
        )
        // this is the default, but ensure that the OS isn't remembering it from the
        // previous connection to the device
        conn.setConnectionPriority(ConnectionPriority.BALANCED)
        return conn
    }

    private suspend fun scanForDevice(deviceName: String, timeout: Long): BTDeviceAddress? {
        log.info { "entering scanForDevice()" }
        val scannerSuspender = buildMatchingDeviceNameSuspender(ctx, deviceName)
        val scanResult = withTimeoutOrNull(timeout) {
            scannerSuspender.awaitResult()
        }
        return scanResult?.device?.address
    }

    private suspend fun doConnectToDevice(
            device: BluetoothDevice
    ): Triple<BluetoothGatt, BTCharacteristicWriter, BLELiveDataCallbacks>? {
        val gattConnectionCreator = GattConnector(ctx)
        log.info { "Creating connection to device $device" }
        val gattAndCallbacks = gattConnectionCreator.createGattConnection(device)

        if (gattAndCallbacks == null) {
            log.warn { "Got nothing back from connection creation attempt!!" }
            return null
        }

        log.info { "Got GATT and callbacks!" }
        val (gatt, callbacks) = gattAndCallbacks

        val services = discoverServices(gatt, callbacks)
        if (!services.truthy()) {
            log.warn { "Service discovery failed!" }
            return null
        }

        val writeCharacteristic = initCharacteristics(gatt)
        if (writeCharacteristic == null) {
            log.warn { "Error setting up BLE characteristics" }
            return null
        }

        val bleWriteChannel = BTCharacteristicWriter(
                gatt,
                writeCharacteristic,
                callbacks.onCharacteristicWriteCompleteLD
        )
        return Triple(gatt, bleWriteChannel, callbacks)
    }

    private suspend fun discoverServices(
            gatt: BluetoothGatt,
            btCallbacks: BLELiveDataCallbacks
    ): List<BluetoothGattService>? {
        log.debug { "Discovering services" }
        val discoverer = ServiceDiscoverer(btCallbacks, gatt)
        val services = discoverer.discoverServices()
        log.debug { "Discovering services: DONE" }

        return services
    }

    private fun initCharacteristics(gatt: BluetoothGatt): BluetoothGattCharacteristic? {
        log.debug { "Initializing characteristics" }
        val subscriber = CharacteristicSubscriber(
                gatt,
                BT_SETUP_SERVICE_ID,
                BT_SETUP_RX_CHARACTERISTIC_ID,
                BT_SETUP_TX_CHARACTERISTIC_ID
        )
        // return write characteristic
        return subscriber.subscribeToReadAndReturnWrite()
    }

}
