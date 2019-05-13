package io.particle.mesh.common.android.livedata

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.particle.mesh.common.android.filterFromAction
import io.particle.mesh.setup.utils.checkIsThisTheMainThread


open class BroadcastReceiverLD<T>(
        context: Context,
        private val broadcastAction: String,
        private val intentValueTransformer: (Intent) -> T,
        private val useLocalBroadcastManager: Boolean = false
) : MutableLiveData<T?>() {

    protected val appCtx = context.applicationContext

    private val localBroadcastManager = LocalBroadcastManager.getInstance(appCtx)
    private val innerReceiver = Receiver()

    override fun onActive() {
        super.onActive()
        checkIsThisTheMainThread()
        if (useLocalBroadcastManager) {
            localBroadcastManager.registerReceiver(innerReceiver, innerReceiver.filter)
        } else {
            appCtx.registerReceiver(innerReceiver, innerReceiver.filter)
        }
    }

    override fun onInactive() {
        super.onInactive()
        checkIsThisTheMainThread()
        if (useLocalBroadcastManager) {
            localBroadcastManager.unregisterReceiver(innerReceiver)
        } else {
            appCtx.unregisterReceiver(innerReceiver)
        }
    }

    private fun onBroadcastReceived(intent: Intent) {
        value = intentValueTransformer(intent)
    }

    private inner class Receiver : BroadcastReceiver() {
        val filter = filterFromAction(broadcastAction)
        override fun onReceive(context: Context, intent: Intent) = onBroadcastReceived(intent)
    }
}
