package io.particle.android.sdk.ui

import android.app.Activity
import android.view.View
import androidx.annotation.RawRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B5_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BLUZ
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BORON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.CORE
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.DIGISTUMP_OAK
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ELECTRON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ESP32
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.OTHER
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.P1
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.PHOTON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.RASPBERRY_PI
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.RED_BEAR_DUO
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.XENON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.X_SOM
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.utils.Async
import io.particle.android.sdk.utils.EZ
import io.particle.android.sdk.utils.SnackbarDuration
import io.particle.android.sdk.utils.snackbar
import io.particle.sdk.app.R
import java.io.*


fun flashTinkerWithDialog(
    activity: FragmentActivity,
    snackbarRootView: View,
    device: ParticleDevice
) {

    val tinkerBinaryResId = when (device.deviceType!!) {

        PHOTON -> R.raw.tinker_firmware_photon
        ELECTRON -> R.raw.tinker_firmware_electron
        ARGON -> R.raw.tinker_firmware_080_rc27_argon
        BORON -> R.raw.tinker_firmware_080_rc27_boron
        XENON -> R.raw.tinker_firmware_080_rc27_xenon
        B5_SOM -> R.raw.b5som_tinker_1_4_5_b5som_2

        CORE -> {
            flashKnownAppWithDialog(activity, device, ParticleDevice.KnownApp.TINKER)
            return
        }

        P1,
        A_SOM,
        B_SOM,
        X_SOM,
        ESP32,
        RASPBERRY_PI,
        RED_BEAR_DUO,
        BLUZ,
        DIGISTUMP_OAK,
        OTHER -> {
            snackbarRootView.snackbar(
                "App does not support flashing tinker to this device.",
                SnackbarDuration.LENGTH_LONG
            )
            return
        }
    }

    flashTinkerWithDialog(activity, device, tinkerBinaryResId)
}


private fun flashTinkerWithDialog(
    activity: FragmentActivity,
    device: ParticleDevice,
    @RawRes tinkerFirmwareResId: Int
) {
    val inputStream = activity.resources.openRawResource(tinkerFirmwareResId)
    AlertDialog.Builder(activity)
        .setMessage("Flash Tinker?")
        .setPositiveButton(R.string.flash) { _, _ ->
            flashFromStream(activity, device, inputStream, "Tinker")
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()
}


private fun flashKnownAppWithDialog(
    activity: FragmentActivity,
    device: ParticleDevice,
    knownApp: ParticleDevice.KnownApp
) {
    AlertDialog.Builder(activity)
        .setMessage("Flash ${knownApp.appName.capitalize()}?")
        .setPositiveButton(R.string.flash) { _, _ -> doFlashKnownApp(activity, device, knownApp) }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()
}


// FIXME: remove duplication between the following methods
private fun doFlashKnownApp(
    activity: Activity,
    device: ParticleDevice,
    knownApp: ParticleDevice.KnownApp
) {
    try {
        Async.executeAsync(device, object : Async.ApiProcedure<ParticleDevice>() {
            @Throws(ParticleCloudException::class, IOException::class)
            override fun callApi(sparkDevice: ParticleDevice): Void? {
                device.flashKnownApp(knownApp)
                return null
            }

            override fun onFailure(exception: ParticleCloudException) {
                showFailureMessage(
                    activity,
                    "Unable to reflash ${knownApp.appName.capitalize()}",
                    exception
                )
            }
        }).andIgnoreCallbacksIfActivityIsFinishing(activity)
    } catch (e: ParticleCloudException) {
        showFailureMessage(activity, "Unable to reflash ${knownApp.appName.capitalize()}", e)
    }

}

private fun flashFromStream(
    activity: Activity,
    device: ParticleDevice,
    stream: InputStream,
    name: String
) {
    try {
        Async.executeAsync(device, object : Async.ApiProcedure<ParticleDevice>() {
            @Throws(ParticleCloudException::class, IOException::class)
            override fun callApi(sparkDevice: ParticleDevice): Void? {
                sparkDevice.flashBinaryFile(stream)
                EZ.closeThisThingOrMaybeDont(stream)
                return null
            }

            override fun onFailure(exception: ParticleCloudException) {
                showFailureMessage(activity, "Unable to flash $name", exception)
            }
        }).andIgnoreCallbacksIfActivityIsFinishing(activity)
    } catch (e: ParticleCloudException) {
        showFailureMessage(activity, "Unable to flash $name", e)
    }

}

private fun showFailureMessage(
    activity: Activity,
    message: String,
    exception: ParticleCloudException
) {
    AlertDialog.Builder(activity)
        .setTitle(message)
        .setMessage(exception.bestMessage)
        .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
        .show()
}
