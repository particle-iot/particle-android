package io.particle.android.common

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import com.afollestad.materialdialogs.MaterialDialog
import io.particle.mesh.ui.R
import io.particle.mesh.common.QATool
import mu.KotlinLogging


private val log = KotlinLogging.logger {}


fun Context.isLocationServicesAvailable(): Boolean {
    val isAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        this.getSystemService(LocationManager::class.java).isLocationEnabled
    } else {
        var locationMode = 0
        try {
            @Suppress("DEPRECATION")
            locationMode = Settings.Secure.getInt(
                this.contentResolver,
                Settings.Secure.LOCATION_MODE
            )
        } catch (e: Settings.SettingNotFoundException) {
            QATool.report(e)
        }

        @Suppress("DEPRECATION")
        locationMode != Settings.Secure.LOCATION_MODE_OFF
    }

    log.info { "Location services available: $isAvailable" }

    return isAvailable
}


fun Context.promptUserToEnableLocationServices(onCancelledAction: (() -> Unit)? = null) {
    MaterialDialog.Builder(this)
        .positiveText(android.R.string.ok)
        .negativeText(R.string.p_mesh_action_exit_setup)
        .onPositive { _, _ -> this.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
        .onNegative { _, _ -> onCancelledAction?.invoke() }
        .cancelListener { onCancelledAction?.invoke() }
        .canceledOnTouchOutside(true)
        .content(
            """Location services are required to find Bluetooth devices.

To continue with setup, enable location services on the next screen.""")
        .show()
}