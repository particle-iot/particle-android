package io.particle.mesh.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context


val Context.btAdapter: BluetoothAdapter
    get() {
        val btMgr = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return btMgr.adapter
    }
