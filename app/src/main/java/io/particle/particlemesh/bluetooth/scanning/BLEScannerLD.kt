package io.particle.particlemesh.bluetooth.scanning

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import io.particle.particlemesh.bluetooth.BluetoothAdapterState
import io.particle.particlemesh.common.Predicate
import io.particle.particlemesh.common.android.livedata.AbsentLiveData
import io.particle.particlemesh.common.android.livedata.switchMap
import io.particle.particlemesh.common.truthy
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging


/**
 * Build a LiveData Bluetooth scanner which will scan when both of these are true:
 *
 * 1. Bluetooth is enabled
 * 2. the [toggleLD] LiveData value is true
 *
 * The toggle lets you kill scans immediately, even if the returned LiveData
 * is still being observed
 */
fun buildReactiveBluetoothScanner(
        toggleLD: LiveData<Boolean?>,  // a LiveData which can turn the whole operation on and off
        adapterStateLD: LiveData<BluetoothAdapterState?>,
        scannerLD: BLEScannerLD
): LiveData<List<ScanResult>?> {
    return toggleLD
            .switchMap { if (it.truthy()) adapterStateLD else AbsentLiveData() }
            .switchMap { if (it == BluetoothAdapterState.ENABLED) scannerLD else AbsentLiveData<List<ScanResult>?>() }
}


class BLEScannerLD(
        private val bluetoothAdapter: BluetoothAdapter,
        private val resultsFilter: Predicate<ScanResult>,
        private val scanFilters: List<ScanFilter> = listOf(),
        private val scanSettings: ScanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build()
) : MutableLiveData<List<ScanResult>?>() {

    private val log = KotlinLogging.logger {}

    private val scanCallback: ScanCallback
    @Volatile
    private var isScanning: Boolean = false

    init {
        this.scanCallback = SimpleScanCallback(
                { sr -> sr != null && sr.scanRecord != null },
                { newResultsReceived(it) }
        )
    }

    override fun onActive() {
        super.onActive()
        if (isScanning) {
            return
        }
        log.info { "Starting scan!" }
        bluetoothAdapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
        isScanning = true
    }

    override fun onInactive() {
        super.onInactive()

        // stop scanning
        if (bluetoothAdapter.isEnabled) {
            log.info { "Stopping scan!" }
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        }
        isScanning = false

        value = listOf()
    }

    private fun newResultsReceived(newResults: List<ScanResult>) {
        launch {
            val filtered = newResults.filter(resultsFilter)
            if (hasActiveObservers()) {
                postValue(filtered)
            }
        }
    }
}
