package io.particle.particlemesh.bluetooth.connecting

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Observer
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.support.annotation.CheckResult
import android.support.annotation.MainThread
import io.particle.particlemesh.bluetooth.GATTStatusCode
import io.particle.particlemesh.bluetooth.BLELiveDataCallbacks
import io.particle.particlemesh.common.android.SimpleLifecycleOwner
import kotlinx.coroutines.experimental.withTimeoutOrNull
import mu.KotlinLogging
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.suspendCoroutine


private val TIMEOUT = TimeUnit.SECONDS.toMillis(5)


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