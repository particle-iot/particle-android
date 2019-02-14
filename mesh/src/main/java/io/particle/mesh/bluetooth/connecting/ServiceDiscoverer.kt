package io.particle.mesh.bluetooth.connecting

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import io.particle.mesh.bluetooth.GATTStatusCode
import io.particle.mesh.bluetooth.BLELiveDataCallbacks
import io.particle.mesh.common.android.SimpleLifecycleOwner
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging
import java.util.concurrent.TimeUnit
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume


private val TIMEOUT = TimeUnit.SECONDS.toMillis(10)


class ServiceDiscoverer(
        private val observables: BLELiveDataCallbacks,
        private val gatt: BluetoothGatt
) {

    // this lifecycleOwner exists just so we can cancel the observer used below
    private val lifecycleOwner = SimpleLifecycleOwner()

    private val log = KotlinLogging.logger {}

    @MainThread
    @CheckResult
    suspend fun discoverServices(): List<BluetoothGattService>? {
        lifecycleOwner.setNewState(Lifecycle.State.RESUMED)
        try {
            return withTimeoutOrNull(TIMEOUT) {
                doDiscoverServices()
            }
        } finally {
            lifecycleOwner.setNewState(Lifecycle.State.DESTROYED)
        }
    }

    private suspend fun doDiscoverServices(): List<BluetoothGattService>? {
        return suspendCoroutine { continuation ->
            doDiscoverServices { continuation.resume(it) }
        }
    }

    private fun doDiscoverServices(callback: (List<BluetoothGattService>?) -> Unit) {
        log.debug{ "Starting doDiscoverServices()" }
        observables.onServicesDiscoveredLD.observe(lifecycleOwner,
                Observer {
                    log.debug { "Services discovered status updated: $it" }
                    if (it == GATTStatusCode.SUCCESS) {
                        callback(gatt.services)
                    }
                }
        )
        gatt.discoverServices()
    }
}