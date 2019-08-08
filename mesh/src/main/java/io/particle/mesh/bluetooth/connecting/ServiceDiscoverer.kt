package io.particle.mesh.bluetooth.connecting

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import io.particle.mesh.bluetooth.BLELiveDataCallbacks
import io.particle.mesh.bluetooth.GATTStatusCode
import io.particle.mesh.common.android.SimpleLifecycleOwner
import io.particle.mesh.setup.flow.Scopes
import kotlinx.coroutines.CompletableDeferred
import mu.KotlinLogging
import java.util.concurrent.TimeUnit


private val TIMEOUT = TimeUnit.SECONDS.toMillis(5)


class ServiceDiscoverer(
    private val observables: BLELiveDataCallbacks,
    private val gatt: BluetoothGatt,
    private val scopes: Scopes
) {

    // this lifecycleOwner exists just so we can cancel the observer used below
    private val lifecycleOwner = SimpleLifecycleOwner()

    private val log = KotlinLogging.logger {}

    @MainThread
    @CheckResult
    suspend fun discoverServices(): List<BluetoothGattService>? {
        lifecycleOwner.setNewState(Lifecycle.State.RESUMED)
        return try {
            scopes.withMain(TIMEOUT) { doDiscoverServices() }
        } catch (ex: Exception) {
            null
        } finally {
            lifecycleOwner.setNewState(Lifecycle.State.DESTROYED)
        }
    }

    private suspend fun doDiscoverServices(): List<BluetoothGattService>? {
        val deferred = CompletableDeferred<List<BluetoothGattService>?>()
        doDiscoverServices(deferred)
        return deferred.await()
    }

    private fun doDiscoverServices(deferred: CompletableDeferred<List<BluetoothGattService>?>) {
        log.debug { "Starting doDiscoverServices()" }
        observables.onServicesDiscoveredLD.observe(lifecycleOwner,
            Observer {
                log.debug { "Services discovered status updated: $it" }
                if (it == GATTStatusCode.SUCCESS) {
                    deferred.complete(gatt.services)
                }
            }
        )
        gatt.discoverServices()
    }
}
