package io.particle.particlemesh.meshsetup.connection

import android.arch.lifecycle.MutableLiveData
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.content.Context
import io.particle.particlemesh.bluetooth.BluetoothAdapterStateLD
import io.particle.particlemesh.bluetooth.btAdapter
import io.particle.particlemesh.bluetooth.scanning.BLEScannerLD
import io.particle.particlemesh.bluetooth.scanning.buildReactiveBluetoothScanner
import io.particle.particlemesh.common.Predicate


class ReactiveScannerAndSwitch(
        val toggleSwitch: MutableLiveData<Boolean>,
        val scannerLD: MutableLiveData<List<ScanResult>?>
)


fun buildMeshDeviceScanner(
        ctx: Context,
        resultsFilter: Predicate<ScanResult>,
        scanFilter: ScanFilter
): ReactiveScannerAndSwitch {

    val toggleScanLD = MutableLiveData<Boolean>()
    toggleScanLD.value = true
    val scanner = buildReactiveBluetoothScanner(
            toggleScanLD,
            BluetoothAdapterStateLD(ctx),
            BLEScannerLD(
                    ctx.btAdapter,
                    resultsFilter,
                    listOf(scanFilter)
            )
    )

    return ReactiveScannerAndSwitch(toggleScanLD, scanner as MutableLiveData<List<ScanResult>?>)
}