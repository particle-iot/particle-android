package io.particle.particlemesh.meshsetup.ui.utils

import android.arch.lifecycle.LiveData
import android.bluetooth.le.ScanFilter.Builder
import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import android.support.v4.app.Fragment
import android.widget.Toast
import io.particle.particlemesh.common.android.livedata.distinct
import io.particle.particlemesh.meshsetup.connection.BT_SETUP_SERVICE_ID
import io.particle.particlemesh.meshsetup.connection.buildMeshDeviceScanner
import io.particle.particlemesh.meshsetup.utils.safeToast
import mu.KotlinLogging


private const val BT_NAME_ID_LENGTH = 6

private val log = KotlinLogging.logger {}


fun buildMatchingDeviceScanner(
        fragment: Fragment,
        serialNumber: String
): LiveData<List<ScanResult>?> {

    val deviceType = getDeviceTypeName(serialNumber)
    val lastSix = serialNumber.substring(serialNumber.length - BT_NAME_ID_LENGTH).toUpperCase()

    val deviceName = "$deviceType-$lastSix"

    fragment.requireActivity().safeToast("Scanning for device $deviceName", Toast.LENGTH_LONG)
    log.info { "Scanning for device $deviceName" }

    val scannerAndSwitch = buildMeshDeviceScanner(
            fragment.context!!.applicationContext,
            { sr -> sr.device.name != null && sr.device.name == deviceName },
            Builder().setServiceUuid(ParcelUuid(BT_SETUP_SERVICE_ID)).build()
    )
    scannerAndSwitch.toggleSwitch.value = true
    return scannerAndSwitch.scannerLD.distinct()
}



private fun getDeviceTypeName(serialNumber: String): String {
    val first4 = serialNumber.substring(0, 4)
    return when(first4) {
        "ARGH" -> "Argon"
        "XENH" -> "Xenon"
        "R40K",
        "R31K" -> "Boron"
        else -> "UNKNOWN"
    }

}