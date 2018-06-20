package io.particle.particlemesh.common.android

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import io.particle.particlemesh.meshsetup.utils.runOnMainThread


class SimpleLifecycleOwner : LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = registry

    fun setNewState(newState: Lifecycle.State) {
        runOnMainThread { registry.markState(newState) }
    }

}