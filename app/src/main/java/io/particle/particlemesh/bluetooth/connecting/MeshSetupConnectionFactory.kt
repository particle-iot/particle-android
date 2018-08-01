package io.particle.particlemesh.bluetooth.connecting

import android.arch.lifecycle.LiveData
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.support.annotation.MainThread
import io.particle.particlemesh.bluetooth.BTCharacteristicWriter
import io.particle.particlemesh.bluetooth.ObservableBLECallbacks
import io.particle.particlemesh.bluetooth.btAdapter
import io.particle.particlemesh.bluetooth.packetTxRxContext
import io.particle.particlemesh.common.QATool
import io.particle.particlemesh.common.truthy
import io.particle.particlemesh.meshsetup.BT_SETUP_RX_CHARACTERISTIC_ID
import io.particle.particlemesh.meshsetup.BT_SETUP_SERVICE_ID
import io.particle.particlemesh.meshsetup.BT_SETUP_TX_CHARACTERISTIC_ID
import io.particle.particlemesh.meshsetup.utils.checkIsThisTheMainThread
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging


enum class ConnectionPriority(val sdkVal: Int) {
    HIGH(1),
    BALANCED(0),
    LOW_POWER(2)
}


class MeshSetupConnection(
        val connectionStateChangedLD: LiveData<ConnectionState?>,
        val gatt: BluetoothGatt,
        // this channel receives arbitrary-length arrays (not limited to BLE MTU)
        val packetSendChannel: SendChannel<ByteArray>,
        // this channel emits arbitrary-length arrays (not limited to BLE MTU)
        private val closablePacketReceiveChannel: Channel<ByteArray>
) {

    val isConnected: Boolean
        get() = connectionStateChangedLD.value == ConnectionState.CONNECTED

    val packetReceiveChannel: ReceiveChannel<ByteArray>
        get() = closablePacketReceiveChannel

    fun setConnectionPriority(priority: ConnectionPriority) {
        gatt.requestConnectionPriority(priority.sdkVal)
    }

    fun disconnect() {
        QATool.runSafely(
                { packetSendChannel.close() },
                { closablePacketReceiveChannel.close() },
                { gatt.disconnect() }
        )
        // calling .close() *immediately* after .disconnect() was sometimes causing
        // the disconnect to fail, thus the delay.
        launch(UI) {
            delay(50)
            gatt.close()
        }
    }
}


typealias BTDeviceAddress = String


class MeshSetupConnectionFactory(private val ctx: Context) {

    private val log = KotlinLogging.logger {}


    @MainThread
    suspend fun connectToDevice(address: BTDeviceAddress): MeshSetupConnection? {

        checkIsThisTheMainThread()

        log.info { "Connecting to device $address" }
        // 1. Attempt to connect
        val device = ctx.btAdapter.getRemoteDevice(address)
        // If this returns null, we're finished, return null ourselves
        val (gatt, bleWriteChannel, callbacks) = doConnectToDevice(device) ?: return null


        val messageWriteChannel = Channel<ByteArray>(128)
        launch(packetTxRxContext) {
            for (packet in messageWriteChannel) {
                QATool.runSafely({ bleWriteChannel.writeToCharacteristic(packet) })
            }
        }

        return MeshSetupConnection(
                callbacks.connectionStateChangedLD,
                gatt,
                messageWriteChannel,
                callbacks.readOrChangedReceiveChannel as Channel<ByteArray>
        )
    }

    private suspend fun doConnectToDevice(
            device: BluetoothDevice
    ): Triple<BluetoothGatt, BTCharacteristicWriter, ObservableBLECallbacks>? {
        val gattConnectionCreator = GattConnector(ctx)
        log.info { "Creating connection to device $device" }
        val gattAndCallbacks = gattConnectionCreator.createGattConnection(device)

        if (gattAndCallbacks == null) {
            log.warn { "Got nothing back from connection creation attempt!!" }
            return null
        }

        log.info { "Got GATT and callbacks!" }
        val (gatt, callbacks) = gattAndCallbacks

//        val bondingResult = bondToDevice(device)
//        if (bondingResult != BondingResult.BONDED) {
//            log.warn { "Bonding failed! result=$bondingResult" }
//            return null
//        }

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
            btCallbacks: ObservableBLECallbacks
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

    private suspend fun bondToDevice(device: BluetoothDevice): BondingResult {
        log.debug { "Attempting to bond..." }
        val bonder = Bonder(ctx)
        val result = bonder.bondToDevice(device)
        log.debug { "Bonding result: $result" }
        return result
    }

}
