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
import io.particle.mesh.bluetooth.BluetoothAdapterStateLD
import io.particle.mesh.bluetooth.btAdapter
import io.particle.mesh.bluetooth.scanning.BLEScannerLD
import io.particle.mesh.bluetooth.scanning.buildReactiveBluetoothScanner
import io.particle.mesh.common.AsyncWorkSuspender
import io.particle.mesh.common.android.livedata.LiveDataSuspender
import io.particle.mesh.setup.connection.BT_SETUP_SERVICE_ID
import mu.KotlinLogging


private val log = KotlinLogging.logger {}


fun buildMatchingDeviceNameScanner(
    context: Context,
    deviceName: String
): LiveData<List<ScanResult>?> {
    log.info { "Scanning for device $deviceName" }

    val ctx = context.applicationContext

    val toggleScanLD = MutableLiveData<Boolean>()
    toggleScanLD.value = true

    val hasPermissionFunc: () -> Boolean = {
        ctx.appHasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    val scannerLD = buildReactiveBluetoothScanner(
        toggleScanLD,
        BluetoothAdapterStateLD(ctx),
        BLEScannerLD(
            ctx.btAdapter,
            { sr -> sr.device.name != null && sr.device.name == deviceName },
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