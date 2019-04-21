package io.particle.mesh.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.mesh.bluetooth.connecting.ConnectionState
import io.particle.mesh.common.QATool
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.common.toHex
import io.particle.mesh.common.truthy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import mu.KotlinLogging


typealias CharacteristicAndStatus = Pair<BluetoothGattCharacteristic, GATTStatusCode>


class BLELiveDataCallbacks : BluetoothGattCallback() {

    val connectionStateChangedLD: LiveData<ConnectionState?> = MutableLiveData()
    val onServicesDiscoveredLD: LiveData<GATTStatusCode> = MutableLiveData()
    val onCharacteristicWriteCompleteLD: LiveData<GATTStatusCode> = MutableLiveData()
    val onCharacteristicReadFailureLD: LiveData<CharacteristicAndStatus> = MutableLiveData()
    val onCharacteristicChangedFailureLD: LiveData<BluetoothGattCharacteristic> = MutableLiveData()
    val onMtuChangedLD: LiveData<Int?> = MutableLiveData()

    val readOrChangedReceiveChannel: ReceiveChannel<ByteArray>
        get() = mutableReceiveChannel
    private val mutableReceiveChannel = Channel<ByteArray>(256)


    private val log = KotlinLogging.logger {}


    fun closeChannel() {
        mutableReceiveChannel.close()
    }


    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        val state = ConnectionState.fromIntValue(newState)
        log.debug { "onConnectionStateChange() gatt: $gatt, state=$state" }

        (connectionStateChangedLD as MutableLiveData).setOnMainThread(state)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        (onServicesDiscoveredLD as MutableLiveData).setOnMainThread(
                GATTStatusCode.fromIntValue(status)
        )
    }

    override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
    ) {
        if (!characteristic.value.truthy()) {
            (onCharacteristicChangedFailureLD as MutableLiveData).setOnMainThread(characteristic)
            return
        }
        receivePacket(characteristic.value)
    }

    override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            statusCode: Int
    ) {
        val status = GATTStatusCode.fromIntValue(statusCode)
        if (!characteristic.value.truthy() || status != GATTStatusCode.SUCCESS) {
            (onCharacteristicReadFailureLD as MutableLiveData).setOnMainThread(
                    Pair(characteristic, status)
            )
            return
        }
        receivePacket(characteristic.value)
    }

    override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            statusCode: Int
    ) {
        (onCharacteristicWriteCompleteLD as MutableLiveData).setOnMainThread(
                GATTStatusCode.fromIntValue(statusCode)
        )
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        log.info { "onMtuChanged() new MTU=$mtu" }
        onMtuChangedLD.castAndPost(mtu)
    }

    private fun receivePacket(packet: ByteArray) {
        log.trace { "Packet received, size=${packet.size} contents=${packet.toHex()}" }

        if (!mutableReceiveChannel.isClosedForSend) {
            QATool.runSafely({ mutableReceiveChannel.offer(packet) })
        } else {
            log.warn { "Channel is closed, cannot pass along packet" }
        }
    }

}
