package io.particle.android.sdk.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat


fun Context.appHasPermission(permission: String): Boolean {
    val result = ContextCompat.checkSelfPermission(this, permission)
    return result == PackageManager.PERMISSION_GRANTED
}


/**
 * The runtime permissions required to scan for and connect to BLE devices.
 *
 * On API 31+ (Android 12) BLE uses the dedicated BLUETOOTH_SCAN / BLUETOOTH_CONNECT
 * permissions (BLUETOOTH_SCAN is declared "neverForLocation"); on older versions BLE
 * scanning instead relies on ACCESS_FINE_LOCATION.
 */
val bleRuntimePermissions: List<String>
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
