package io.particle.mesh.bluetooth.connecting

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import androidx.annotation.MainThread
import io.particle.mesh.common.QATool
import mu.KotlinLogging
import java.util.*


private val log = KotlinLogging.logger {}


// this is pulled from Nordic's BLE impl, but it's also found elsewhere (just google for the string)
private val CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString(
        "00002902-0000-1000-8000-00805f9b34fb"
)



class CharacteristicSubscriber(
        private val gatt: BluetoothGatt,
        private val serviceUuid: UUID,
        private val writeCharacteristicUuid: UUID,
        private val readCharacteristicUuid: UUID
) {

    @MainThread
    fun subscribeToReadAndReturnWrite(): BluetoothGattCharacteristic? {
        val service = gatt.getService(serviceUuid)

        val subscribedToReadChar = subscribeToReadCharacteristic()
        if (!subscribedToReadChar) {
            log.error { "Could not subscribe to read characteristic!" }
            return null
        }

        val writeCharacteristic = service?.characteristics?.firstOrNull {
            c -> c.uuid == writeCharacteristicUuid
        }
        if (writeCharacteristic == null) {
            log.error { "Could not access write characteristic!" }
        }
        return writeCharacteristic
    }

    private fun subscribeToReadCharacteristic(): Boolean {
        val device = gatt.device

        val service = gatt.services.firstOrNull { svc -> svc.uuid == serviceUuid }
        if (service == null) {
            QATool.illegalState("Service not found on $device!")
            return false
        }

        val characteristic = service.characteristics.firstOrNull { c -> c.uuid == readCharacteristicUuid }
        if (characteristic == null) {
            QATool.illegalState("Characteristic not found on $device!")
            return false
        }

        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            QATool.illegalState("Could not set notification characteristic to true on $device!")
            return false
        }

        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID)
        if (descriptor == null) {
            QATool.illegalState("Descriptor not found for characteristic on $device!")
        }

        return if (descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
            gatt.writeDescriptor(descriptor)
        } else {
            QATool.illegalState("Could not set descriptor value on $device!")
            false
        }
    }

}