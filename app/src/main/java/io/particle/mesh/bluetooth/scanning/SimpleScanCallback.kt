package io.particle.mesh.bluetooth.scanning

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import io.particle.mesh.common.Predicate
import io.particle.mesh.common.Procedure
import mu.KotlinLogging
import java.util.*


class SimpleScanCallback(
        private val isResultValid: Predicate<ScanResult?>,
        private val scanResultAction: Procedure<List<ScanResult>>
) : ScanCallback() {

    private val log = KotlinLogging.logger {}

    override fun onScanResult(callbackType: Int, result: ScanResult) {
        onBatchScanResults(Collections.singletonList(result))
    }

    override fun onBatchScanResults(results: List<ScanResult?>) {
        scanResultAction(results.filterNotNull().filter(isResultValid))
    }

    override fun onScanFailed(errorCode: Int) {
        when (errorCode) {
            ScanCallback.SCAN_FAILED_ALREADY_STARTED,
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED,
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR,
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> {
                log.warn { "onScanFailed(), error code=$errorCode" }
            }
            else -> return
        }
    }

}
