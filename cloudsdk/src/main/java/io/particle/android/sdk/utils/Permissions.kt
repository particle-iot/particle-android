package io.particle.android.sdk.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat


fun Context.appHasPermission(permission: String): Boolean {
    val result = ContextCompat.checkSelfPermission(this, permission)
    return result == PackageManager.PERMISSION_GRANTED
}
