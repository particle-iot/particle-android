package io.particle.mesh.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import io.particle.mesh.common.android.livedata.BroadcastReceiverLD


enum class BluetoothAdapterState {
    ENABLED,
    DISABLED;
}


class BluetoothAdapterStateLD(
        ctx: Context
) : BroadcastReceiverLD<BluetoothAdapterState?>(
        ctx,
        BluetoothAdapter.ACTION_STATE_CHANGED,
        Companion::getStateFromIntent
) {

    override fun onActive() {
        super.onActive()
        value = appCtx.btAdapter.isEnabled.toAdapterState()
    }

    companion object {
        private fun getStateFromIntent(intent: Intent): BluetoothAdapterState {
            val currentState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
            return (currentState == BluetoothAdapter.STATE_ON).toAdapterState()
        }
    }
}


private fun Boolean.toAdapterState(): BluetoothAdapterState {
    return if (this) BluetoothAdapterState.ENABLED else BluetoothAdapterState.DISABLED
}
