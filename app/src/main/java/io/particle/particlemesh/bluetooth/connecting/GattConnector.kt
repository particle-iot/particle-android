package io.particle.particlemesh.bluetooth.connecting

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Observer
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import io.particle.particlemesh.bluetooth.ObservableBLECallbacks
import io.particle.particlemesh.bluetooth.btAdapter
import io.particle.particlemesh.common.android.SimpleLifecycleOwner
import io.particle.particlemesh.common.android.livedata.first
import kotlinx.coroutines.experimental.withTimeoutOrNull
import mu.KotlinLogging
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine


private val INITIAL_CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(10)


class GattConnector(private val ctx: Context) {

    private val lifecycleOwner = SimpleLifecycleOwner()

    private val log = KotlinLogging.logger {}

    suspend fun createGattConnection(
            device: BluetoothDevice
    ): Pair<BluetoothGatt, ObservableBLECallbacks>? {
        lifecycleOwner.setNewState(Lifecycle.State.RESUMED)

        this.ctx.btAdapter.cancelDiscovery()

        val callbacks = ObservableBLECallbacks()
        val gatt = withTimeoutOrNull(INITIAL_CONNECTION_TIMEOUT) {
            doCreateGattConnection(device, ctx, callbacks)
        }

        try {
            return if (gatt == null) null else Pair(gatt, callbacks)
        } finally {
            lifecycleOwner.setNewState(Lifecycle.State.DESTROYED)
            callbacks.connectionStateChangedLD.removeObservers(lifecycleOwner)
        }
    }

    private suspend fun doCreateGattConnection(
            device: BluetoothDevice,
            ctx: Context,
            callbacks: ObservableBLECallbacks
    ): BluetoothGatt {
        return suspendCoroutine { continuation: Continuation<BluetoothGatt> ->
            doCreateGattConnection(device, ctx, callbacks, { continuation.resume(it) })
        }
    }

    private fun doCreateGattConnection(
            device: BluetoothDevice,
            ctx: Context,
            observableCallbacks: ObservableBLECallbacks,
            callback: (BluetoothGatt) -> Unit
    ) {
        log.info { "About to connect to $device" }
        val gattRef = device.connectGatt(ctx.applicationContext, false, observableCallbacks)
        log.info { "Called connectGatt for $gattRef" }

        observableCallbacks.connectionStateChangedLD.observe(lifecycleOwner, Observer {
            log.debug { "Connection state UpDaTeD to $it" }
        })

        observableCallbacks.connectionStateChangedLD
                .first { it == ConnectionState.CONNECTED }
                .observe(lifecycleOwner, Observer {
                    log.debug { "Connection state updated to $it" }
                    if (it == ConnectionState.CONNECTED) {
                        callback(gattRef)
                    } else {
                        log.error { "Connection status not CONNECTED?! it=$it" }
                    }
                })
    }

}