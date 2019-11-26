package io.particle.android.sdk.utils

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager


interface Broadcaster {
    fun sendBroadcast(intent: Intent)
}


class BroadcastImpl(private val localBroadcaster: LocalBroadcastManager) : Broadcaster {

    override fun sendBroadcast(intent: Intent) {
        localBroadcaster.sendBroadcast(intent)
    }

}