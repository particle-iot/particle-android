package io.particle.mesh.setup.ui.utils

import android.bluetooth.le.ScanFilter.Builder
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.ParcelUuid
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.snakydesign.livedataextensions.distinctUntilChanged
import com.snakydesign.livedataextensions.filter
import com.snakydesign.livedataextensions.map
import com.snakydesign.livedataextensions.nonNull
import io.particle.android.sdk.utils.appHasPermission
import io.particle.android.sdk.utils.bleRuntimePermissions
import io.particle.mesh.bluetooth.BluetoothAdapterStateLD
import io.particle.mesh.bluetooth.btAdapter
import io.particle.mesh.bluetooth.scanning.BLEScannerLD
import io.particle.mesh.bluetooth.scanning.buildReactiveBluetoothScanner
import io.particle.mesh.common.AsyncWorkSuspender
import io.particle.mesh.common.android.livedata.LiveDataSuspender
import io.particle.mesh.setup.connection.BT_SETUP_SERVICE_ID
import mu.KotlinLogging


private val log = KotlinLogging.logger {}


/**
 * Match a scan result against the device name from the sticker/barcode using the name advertised
 * in the BLE scan record. Reading [ScanResult.getDevice].name (i.e. BluetoothDevice.getName())
 * requires BLUETOOTH_CONNECT on API 31+ and may be null mid-scan; the advertised local name is
 * available with only BLUETOOTH_SCAN — the same permission the scan itself needs — so matching on
 * it avoids a SecurityException and an unnecessary permission dependency during scanning.
 */
private fun ScanResult.matchesAdvertisedName(deviceName: String): Boolean {
    val advertisedName = this.scanRecord?.deviceName
    return advertisedName != null && advertisedName == deviceName
}


fun buildMatchingDeviceNameScanner(
    context: Context,
    deviceName: String
): LiveData<List<ScanResult>?> {
    log.info { "Scanning for device $deviceName" }

    val ctx = context.applicationContext

    val toggleScanLD = MutableLiveData<Boolean>()
    toggleScanLD.value = true

    val hasPermissionFunc: () -> Boolean = {
        bleRuntimePermissions.all { ctx.appHasPermission(it) }
    }

    val scannerLD = buildReactiveBluetoothScanner(
        toggleScanLD,
        BluetoothAdapterStateLD(ctx),
        BLEScannerLD(
            ctx.btAdapter,
            { sr -> sr.matchesAdvertisedName(deviceName) },
            hasPermissionFunc,
            listOf(Builder().setServiceUuid(ParcelUuid(BT_SETUP_SERVICE_ID)).build())
        )
    )

    return scannerLD.distinctUntilChanged()
}


fun buildMatchingDeviceNameSuspender(
    context: Context,
    deviceName: String
): AsyncWorkSuspender<ScanResult?> {
    val scannerLD = buildMatchingDeviceNameScanner(context, deviceName)
    return object : LiveDataSuspender<ScanResult?>() {
        override fun buildLiveData(): LiveData<ScanResult?> {
            return scannerLD.nonNull()
                .filter { it!!.isNotEmpty() }
                .map { it!![0] }
        }
    }
}