package io.particle.mesh.bluetooth.connecting

import android.bluetooth.BluetoothDevice
import io.particle.android.sdk.utils.buildIntValueMap

enum class BondingState(val intValue: Int) {

    BOND_NONE(BluetoothDevice.BOND_NONE),
    BOND_BONDING(BluetoothDevice.BOND_BONDING),
    BOND_BONDED(BluetoothDevice.BOND_BONDED);

    companion object {

        private val intValueMap = buildIntValueMap(values()) { state -> state.intValue }

        fun fromIntValue(intValue: Int): BondingState {
            val state = intValueMap.get(intValue)
            return requireNotNull(state)
        }
    }
}