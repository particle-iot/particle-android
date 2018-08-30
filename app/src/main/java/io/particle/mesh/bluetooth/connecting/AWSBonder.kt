package io.particle.mesh.bluetooth.connecting

import android.arch.lifecycle.LiveData
import android.bluetooth.BluetoothDevice
import android.content.Context
import io.particle.mesh.common.Predicate
import io.particle.mesh.common.and
import io.particle.mesh.common.android.livedata.BroadcastReceiverLD
import io.particle.mesh.common.android.livedata.LiveDataSuspender
import io.particle.mesh.common.android.livedata.first
import io.particle.mesh.common.android.livedata.map

class AWSBonder(
        private val ctx: Context,
        private val device: BluetoothDevice
) : LiveDataSuspender<BondingResult?>() {

    override fun buildLiveData(): LiveData<BondingResult?> {
        val nameMatches: Predicate<BondingStateUpdate?> = { device.address == it?.device?.address }
        val isBonded: Predicate<BondingStateUpdate?> = { it?.newState == BondingState.BOND_BONDED }
        return buildBondingStateLD(ctx)
                .first(nameMatches and isBonded)
                .map { BondingState.fromIntValue(it!!.newState.intValue).toResult() }
    }

    override fun startAsyncWork(workCompleteCallback: (BondingResult?) -> Unit) {
        super.startAsyncWork(workCompleteCallback)
        val willStartBonding = device.createBond()

        if (!willStartBonding) {
            // FAIL right out of the gate
            workCompleteCallback(BondingResult.ERROR_DID_NOT_START)
        }
    }
}



private fun BondingState.toResult(): BondingResult {
    TODO("finish this")
}


private fun buildBondingStateLD(ctx: Context): BroadcastReceiverLD<BondingStateUpdate> {
    return BroadcastReceiverLD(
            ctx,
            BluetoothDevice.ACTION_BOND_STATE_CHANGED,
            { BondingStateUpdate.fromIntentBroadcast(it) }
    )
}
