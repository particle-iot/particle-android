package io.particle.android.sdk.ui

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import androidx.appcompat.app.AlertDialog

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.utils.Async
import io.particle.android.sdk.utils.EZ
import io.particle.sdk.app.R


internal object FlashAppHelper {

    fun flashAppFromBinaryWithDialog(
        activity: FragmentActivity,
        device: ParticleDevice, path: File
    ) {
        AlertDialog.Builder(activity)
            // FIXME: this is just for flashing Tinker for now, but later it could be used
            // for whatever file the user wants to upload (and "known apps" will work for
            // the Photon, too)
            // .content(String.format("Flash %s?", capitalize(knownApp.getAppName())))
            .setMessage("Flash Tinker?")
            .setPositiveButton(R.string.flash) { dialog, which ->
                flashFromBinary(
                    activity, device, path
                )
            }
            .setNegativeButton(R.string.cancel) { dialog, which -> dialog.dismiss() }
            .show()
    }


    fun flashPhotonTinkerWithDialog(
        activity: FragmentActivity,
        device: ParticleDevice
    ) {
        val inputStream = activity.resources.openRawResource(R.raw.tinker_firmware_photon)
        AlertDialog.Builder(activity)
            .setMessage("Flash Tinker?")
            .setPositiveButton(R.string.flash) { dialog, which ->
                flashFromStream(
                    activity, device, inputStream, "Tinkerrrrr"
                )
            }
            .setNegativeButton(R.string.cancel) { dialog, which -> dialog.dismiss() }
            .show()
    }


    fun flashKnownAppWithDialog(
        activity: FragmentActivity,
        device: ParticleDevice,
        knownApp: ParticleDevice.KnownApp
    ) {
        AlertDialog.Builder(activity)
            .setMessage(String.format("Flash %s?", capitalize(knownApp.appName)))
            .setPositiveButton(R.string.flash) { dialog, which ->
                flashKnownApp(
                    activity, device, knownApp
                )
            }
            .setNegativeButton(R.string.cancel) { dialog, which -> dialog.dismiss() }
            .show()
    }


    // FIXME: remove duplication between the following methods
    private fun flashKnownApp(
        activity: Activity, device: ParticleDevice,
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
                        "Unable to reflash " + capitalize(knownApp.appName)!!,
                        exception
                    )
                }
            }).andIgnoreCallbacksIfActivityIsFinishing(activity)
        } catch (e: ParticleCloudException) {
            showFailureMessage(activity, "Unable to reflash " + capitalize(knownApp.appName)!!, e)
        }

    }

    private fun flashFromBinary(
        activity: Activity, device: ParticleDevice,
        binaryFile: File
    ) {
        // FIXME: incorporate real error handling here
        try {
            val fis = FileInputStream(binaryFile)
            flashFromStream(activity, device, fis, binaryFile.name)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }


    private fun flashFromStream(
        activity: Activity, device: ParticleDevice,
        stream: InputStream, name: String
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
                    showFailureMessage(activity, "Unable to reflash from $name", exception)
                }
            }).andIgnoreCallbacksIfActivityIsFinishing(activity)
        } catch (e: ParticleCloudException) {
            showFailureMessage(activity, "Unable to reflash from $name", e)
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
            .setPositiveButton(R.string.ok) { dialog, which -> dialog.dismiss() }
            .show()
    }

    // lifted from Apache commons-lang StringUtils
    private fun capitalize(str: String?): String? {
        val strLen: Int
        if (str == null || (strLen = str.length) == 0) {
            return str
        }

        val firstCodepoint = str.codePointAt(0)
        val newCodePoint = Character.toTitleCase(firstCodepoint)
        if (firstCodepoint == newCodePoint) {
            // already capitalized
            return str
        }

        val newCodePoints = IntArray(strLen) // cannot be longer than the char array
        var outOffset = 0
        newCodePoints[outOffset++] = newCodePoint // copy the first codepoint
        var inOffset = Character.charCount(firstCodepoint)
        while (inOffset < strLen) {
            val codepoint = str.codePointAt(inOffset)
            newCodePoints[outOffset++] = codepoint // copy the remaining ones
            inOffset += Character.charCount(codepoint)
        }
        return String(newCodePoints, 0, outOffset)
    }

}
