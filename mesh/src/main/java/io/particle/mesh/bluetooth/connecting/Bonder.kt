package io.particle.mesh.bluetooth.connecting

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import io.particle.mesh.common.Predicate
import io.particle.mesh.common.and
import io.particle.mesh.common.android.SimpleLifecycleOwner
import io.particle.mesh.common.android.livedata.BroadcastReceiverLD
import io.particle.mesh.common.android.livedata.first
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume


enum class BondingResult {
    BONDED,
    TIMED_OUT,
    ERROR_DID_NOT_START,
    ERROR_OTHER
}


data class BondingStateUpdate(
        val device: BluetoothDevice,
        val newState: BondingState,
        val previousState: BondingState
) {

    companion object {

        private val log = KotlinLogging.logger {}

        fun fromIntentBroadcast(intent: Intent): BondingStateUpdate {
            val newStateInt = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
            val prevStateInt = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
            val update = BondingStateUpdate(
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE),
                    BondingState.fromIntValue(newStateInt),
                    BondingState.fromIntValue(prevStateInt)
            )
            log.debug { "New bonding state intent received: $update" }
            return update
        }

    }

}


private const val BONDING_TIMEOUT_MILLIS = 15000L


class Bonder(ctx: Context) {

    private val log = KotlinLogging.logger {}

    private val lifecycleOwner = SimpleLifecycleOwner()
    private val broadcastReceiverLD = buildBondingStateLD(ctx)


    suspend fun bondToDevice(device: BluetoothDevice): BondingResult {
        lifecycleOwner.setNewState(Lifecycle.State.RESUMED)

        log.info { "Pre-bond to device (OUTSIDE timeout block): $device" }
        val result = withTimeoutOrNull(BONDING_TIMEOUT_MILLIS) {
            log.info { "Pre-bond to device (INSIDE timeout block): $device" }
            doBondToDevice(device)
        }

        try {
            log.warn { "Bonding result=$result device=$device" }
            return result ?: BondingResult.TIMED_OUT
        } finally {
            lifecycleOwner.setNewState(Lifecycle.State.DESTROYED)
            broadcastReceiverLD.removeObservers(lifecycleOwner)
        }
    }

    private suspend fun doBondToDevice(device: BluetoothDevice): BondingResult {
        return suspendCoroutine { continuation: Continuation<BondingResult> ->
            doBondToDevice(device) { continuation.resume(it) }
        }
    }

    private fun doBondToDevice(device: BluetoothDevice, callback: (BondingResult) -> Unit) {
        val nameMatches: Predicate<BondingStateUpdate?> = { device.address == it?.device?.address }
        val isBonded: Predicate<BondingStateUpdate?> = { it?.newState == BondingState.BOND_BONDED }
        broadcastReceiverLD
                .first(nameMatches and isBonded)
                .observe(lifecycleOwner, Observer {
                    log.debug { "Bonding state updated: $it" }
                    if (it?.newState != BondingState.BOND_BONDED) {
                        // FIXME: remove this later, this was just copypasta
                        // that seems like it should be unnecessary
                        log.warn { "New bonding state not BOND_BONDED?! newState=${it?.newState}" }
                    } else {
                        log.info { "Bonded!" }
                        callback(BondingResult.BONDED)
                    }
                })

        val willStartBonding = device.createBond()

        if (!willStartBonding) {
            // FAIL right out of the gate
            callback(BondingResult.ERROR_DID_NOT_START)
        }
    }

}


private fun buildBondingStateLD(ctx: Context): BroadcastReceiverLD<BondingStateUpdate> {
    return BroadcastReceiverLD(
            ctx,
            BluetoothDevice.ACTION_BOND_STATE_CHANGED,
            { BondingStateUpdate.fromIntentBroadcast(it) }
    )
}

