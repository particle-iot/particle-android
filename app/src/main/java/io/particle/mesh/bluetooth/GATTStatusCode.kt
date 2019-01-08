package io.particle.mesh.bluetooth

import android.bluetooth.BluetoothGatt
import io.particle.mesh.common.buildIntValueMap


enum class GATTStatusCode(val intValue: Int) {

    SUCCESS(BluetoothGatt.GATT_SUCCESS),
    FAILURE(BluetoothGatt.GATT_FAILURE),  // non-specific failure
    UNKNOWN(0x10101),  // made up value that doesn't conflict with/overload any others

    INSUFFICIENT_AUTHENTICATION(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION),
    INSUFFICIENT_ENCRYPTION(BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION),
    INVALID_ATTRIBUTE_LENGTH(BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH),
    INVALID_OFFSET(BluetoothGatt.GATT_INVALID_OFFSET),
    READ_NOT_PERMITTED(BluetoothGatt.GATT_READ_NOT_PERMITTED),
    REQUEST_NOT_SUPPORTED(BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED),
    WRITE_NOT_PERMITTED(BluetoothGatt.GATT_WRITE_NOT_PERMITTED);


    companion object {

        private val intValueMap = buildIntValueMap(values()) { state -> state.intValue }

        fun fromIntValue(intValue: Int): GATTStatusCode {
            return intValueMap.get(intValue, UNKNOWN)
        }
    }

}